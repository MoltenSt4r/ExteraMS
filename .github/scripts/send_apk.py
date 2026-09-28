import asyncio
import glob
import json
import os

from pyrogram import Client

APK_GLOB = "TMessagesProj/build/outputs/apk/release/*.apk"
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


def caption():
    sha = (os.environ.get("COMMIT_SHA") or "")[:9]
    tail = f"`{sha}`\n{os.environ.get('RUN_URL', '')}"
    entries = commits() or [head_subject()]

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

    async with Client(
        "ci",
        api_id=int(os.environ["TG_API_ID"]),
        api_hash=os.environ["TG_API_HASH"],
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
        return raw


asyncio.run(main())
