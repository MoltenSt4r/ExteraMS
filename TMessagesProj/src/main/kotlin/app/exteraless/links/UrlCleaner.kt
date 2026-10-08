package app.exteraless.links

class UrlCleaner internal constructor(private val rules: List<AdGuardRule>) {

    fun clean(url: String): String {
        val parsed = ParsedUrl.parse(url) ?: return url

        val host = parsed.host.lowercase()
        val pathAndQuery = parsed.pathAndQuery
        val applicable = rules.filter { it.matchesUrl(host, pathAndQuery, url) }
        if (applicable.isEmpty()) return url
        if (applicable.any { it.exception && it.paramMatcher is ParamMatcher.All }) return url

        val (exceptions, removals) = applicable.partition { it.exception }
        if (removals.isEmpty()) return url

        val (dropped, kept) = parsed.params.partition { (k, v) ->
            removals.any { it.paramMatcher.matches(k, v) } &&
                exceptions.none { it.paramMatcher.matches(k, v) }
        }

        val stripDelim = parsed.hadQuery && kept.isEmpty() &&
            removals.any { it.paramMatcher is ParamMatcher.All }

        if (dropped.isEmpty() && !stripDelim) return url
        return parsed.rebuild(kept)
    }

    companion object {
        fun fromAdGuardFilter(text: String): UrlCleaner {
            val rules = text.lineSequence().mapNotNull(AdGuardRuleParser::parse).toList()
            return UrlCleaner(rules)
        }
    }
}

private fun AdGuardRule.matchesUrl(host: String, pathAndQuery: String, fullUrl: String): Boolean {
    if (domainInclude.isNotEmpty() && domainInclude.none { hostMatches(host, it) }) return false
    if (domainExclude.any { hostMatches(host, it) }) return false
    return when (val m = urlMatcher) {
        is UrlMatcher.Any -> true
        is UrlMatcher.FullRegex -> m.regex.containsMatchIn(fullUrl)
        is UrlMatcher.Host -> {
            val hostOk = m.hostRegex?.matches(host) ?: hostMatches(host, m.host)
            hostOk && (m.pathRegex == null || m.pathRegex.containsMatchIn(pathAndQuery))
        }
    }
}

private fun hostMatches(host: String, rule: String): Boolean = host == rule || host.endsWith(".$rule")

internal class ParsedUrl private constructor(
    val host: String,
    val params: List<Pair<String, String?>>,
    val hadQuery: Boolean,
    private val scheme: String,
    private val authority: String,
    private val path: String,
    private val fragment: String?,
) {
    val pathAndQuery: String
        get() = if (!hadQuery) path else "$path?${rebuildQuery(params)}"

    fun rebuild(keptParams: List<Pair<String, String?>>): String = buildString {
        append(scheme).append("://").append(authority).append(path)
        if (keptParams.isNotEmpty()) append('?').append(rebuildQuery(keptParams))
        if (fragment != null) append('#').append(fragment)
    }

    companion object {
        fun parse(url: String): ParsedUrl? {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd <= 0) return null
            val scheme = url.substring(0, schemeEnd)
            val afterScheme = url.substring(schemeEnd + 3)

            val fragIdx = afterScheme.indexOf('#')
            val beforeFrag = if (fragIdx < 0) afterScheme else afterScheme.substring(0, fragIdx)
            val fragment = if (fragIdx < 0) null else afterScheme.substring(fragIdx + 1)

            val qIdx = beforeFrag.indexOf('?')
            val authAndPath = if (qIdx < 0) beforeFrag else beforeFrag.substring(0, qIdx)
            val query = if (qIdx < 0) "" else beforeFrag.substring(qIdx + 1)

            val pathStart = authAndPath.indexOf('/')
            val authority = if (pathStart < 0) authAndPath else authAndPath.substring(0, pathStart)
            val path = if (pathStart < 0) "" else authAndPath.substring(pathStart)

            val hostPort = authority.substringAfterLast('@')
            val host = hostPort.substringBefore(':')
            if (host.isEmpty()) return null

            return ParsedUrl(
                host = host,
                params = if (query.isEmpty()) emptyList() else parseQuery(query),
                hadQuery = qIdx >= 0,
                scheme = scheme,
                authority = authority,
                path = path,
                fragment = fragment,
            )
        }

        private fun parseQuery(q: String): List<Pair<String, String?>> =
            q.split('&').mapNotNull { piece ->
                if (piece.isEmpty()) return@mapNotNull null
                val eq = piece.indexOf('=')
                if (eq < 0) piece to null
                else piece.substring(0, eq) to piece.substring(eq + 1)
            }

        private fun rebuildQuery(params: List<Pair<String, String?>>): String =
            params.joinToString("&") { (k, v) -> if (v == null) k else "$k=$v" }
    }
}
