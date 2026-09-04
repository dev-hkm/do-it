import os
import sys
import uuid
import json
import urllib.request
import urllib.parse
from pathlib import Path

def upload_to_tmpfiles(file_path: Path) -> str:
    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    crlf = b"\r\n"
    filename = "Do-It-release.apk"
    with open(file_path, "rb") as f:
        data = f.read()
    parts = [
        b"--" + boundary.encode() + crlf,
        f'Content-Disposition: form-data; name="file"; filename="{filename}"'.encode("utf-8") + crlf,
        b"Content-Type: application/vnd.android.package-archive" + crlf + crlf,
        data,
        crlf + b"--" + boundary.encode() + b"--" + crlf
    ]
    req = urllib.request.Request(
        "https://tmpfiles.org/api/v1/upload",
        data=b"".join(parts),
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "User-Agent": "curl/7.68.0"
        }
    )
    with urllib.request.urlopen(req, timeout=40) as resp:
        res = json.loads(resp.read().decode())
        url = res["data"]["url"]
        return url.replace("tmpfiles.org/", "tmpfiles.org/dl/")

def send_ntfy(topic: str, message: str, title: str = "TODO APK Release"):
    url = f"https://ntfy.sh/{topic}"
    headers = {
        "Title": title.encode("utf-8").decode("latin-1", "ignore"),
        "Priority": "high",
        "Tags": "package,white_check_mark",
        "Content-Type": "text/plain; charset=utf-8"
    }
    req = urllib.request.Request(
        url,
        data=message.encode("utf-8"),
        headers=headers,
        method="POST"
    )
    with urllib.request.urlopen(req) as resp:
        return resp.read().decode("utf-8")

if __name__ == "__main__":
    apk_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("app/build/outputs/apk/release/Do-It-release.apk")
    changelog = sys.argv[2] if len(sys.argv) > 2 else "Workflow verification build."
    
    if not apk_path.exists():
        print(f"Error: {apk_path} not found.")
        sys.exit(1)

    print(f"Uploading direct APK {apk_path.name} ({apk_path.stat().st_size} bytes) to tmpfiles.org CDN...")
    download_url = upload_to_tmpfiles(apk_path)
    print(f"Upload success! Direct APK Link: {download_url}")

    ntfy_body = f"🚀 Link tải trực tiếp file APK (1-chạm cài đặt): {download_url}\n\n📝 Changelog:\n{changelog}"
    print("Sending ntfy notification to topic 'filesend'...")
    send_ntfy("filesend", ntfy_body, title="TODO APK Release Ready")
    print("Notification sent successfully!")
