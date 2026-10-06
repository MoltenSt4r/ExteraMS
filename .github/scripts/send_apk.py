import asyncio
import glob
import json
import os

from pyrogram import Client

APK_GLOB = os.environ.get("APK_GLOB") or "TMessagesProj/build/outputs/apk/release/*.apk"
CAPTION_LIMIT = 1024
TITLE = "ExteraMS v12.10.3"


def commits():
    try:
        payload = json.loads(os.environ.get("COMMITS_JSON") or "[]")
    except Exception:
        payload = []
    out = []
    for commit in payload:
        subject = (commit.get("message") or "").strip().splitlines()
        if subject:
            out.append(subject[0])
    return out


def head_subject():
    lines = (os.environ.get("COMMIT_MESSAGE") or "").strip().splitlines()
    return lines[0] if lines else "без описания"


def quote(entries):
    if not entries:
        return None
    body = [f">• {entry}" for entry in entries]
    body[0] = "**" + body[0]
    body[-1] = body[-1] + "||"
    return "\n".join(body)


def get_entries():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    changelog_file = os.path.join(script_dir, "release_changelog.txt")
    if os.path.isfile(changelog_file):
        with open(changelog_file, "r", encoding="utf-8") as f:
            lines = [l.strip() for l in f if l.strip() and not l.strip().startswith("#")]
            if lines:
                return lines

    raw = os.environ.get("COMMIT_MESSAGE") or ""
    lines = [line.strip() for line in raw.splitlines() if line.strip()]
    bullets = [line.lstrip("•-* ").strip() for line in lines if line.startswith(("-", "*", "•"))]
    if bullets:
        return bullets

    return commits() or [head_subject()]


def caption():
    sha = (os.environ.get("COMMIT_SHA") or "")[:9]
    tail = f"`{sha}`\n{os.environ.get('RUN_URL', '')}"
    entries = get_entries()

    while True:
        parts = [f"**{TITLE}**"]
        block = quote(entries)
        if block:
            parts.append(block)
        parts.append(tail)
        text = "\n\n".join(parts)
        if len(text) <= CAPTION_LIMIT or not entries:
            return text[:CAPTION_LIMIT]
        entries = entries[1:]


async def main() -> None:
    apks = sorted(glob.glob(APK_GLOB), key=os.path.getmtime)
    if not apks:
        raise SystemExit(f"APK не найден: {APK_GLOB}")
    apk = apks[-1]
    print(f"{os.path.basename(apk)} — {os.path.getsize(apk) / 1024 / 1024:.1f} МБ")

    api_id = int(os.environ.get("TG_API_ID") or 39205442)
    api_hash = os.environ.get("TG_API_HASH") or "1090b570704d1d97975994ee9c177dba"

    async with Client(
        "ci",
        api_id=api_id,
        api_hash=api_hash,
        bot_token=os.environ["TG_BOT_TOKEN"],
        in_memory=True,
        no_updates=True,
    ) as app:
        message = await app.send_document(
            chat(),
            apk,
            caption=caption(),
            file_name=os.path.basename(apk),
            force_document=True,
        )
        print("отправлено, id =", message.id)


def chat():
    raw = os.environ["TG_CHAT_ID"].strip()
    try:
        return int(raw)
    except ValueError:
        if not raw.startswith(("@", "-")):
            return "@" + raw
        return raw


if __name__ == "__main__":
    import sys
    try:
        asyncio.run(main())
    except Exception as e:
        import traceback
        print(f"::warning::Ошибка отправки APK в Telegram: {type(e).__name__}: {e}")
        traceback.print_exc()
        sys.exit(1)
