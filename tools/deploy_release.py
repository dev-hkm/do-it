import os
import sys
import uuid
import json
import urllib.request
import urllib.parse
from pathlib import Path

def upload_to_r2(file_path: Path, object_name: str = None) -> str:
    import boto3
    from botocore.config import Config

    if object_name is None:
        object_name = file_path.name

    account_id = os.environ.get("R2_ACCOUNT_ID", "2808e79787056afd24887aa213ba3d13")
    access_key_id = os.environ.get("R2_ACCESS_KEY_ID", "f8e23653fbe5c1d8d55697b1d7907f80")
    secret_access_key = os.environ.get("R2_SECRET_ACCESS_KEY", "a695cc7ccd61cdf9caa52acb0a6624154a2dee44373298237d6b93b072e3baa6")
    bucket_name = os.environ.get("R2_BUCKET", "filesend")
    public_base = os.environ.get("R2_PUBLIC_BASE", "https://pub-66aa1ff95abc4f6198b4ab5def83a4a0.r2.dev").rstrip("/")

    endpoint_url = f"https://{account_id}.r2.cloudflarestorage.com"
    s3_client = boto3.client(
        "s3",
        endpoint_url=endpoint_url,
        aws_access_key_id=access_key_id,
        aws_secret_access_key=secret_access_key,
        config=Config(signature_version="s3v4"),
        region_name="auto"
    )

    s3_client.upload_file(
        Filename=str(file_path),
        Bucket=bucket_name,
        Key=object_name,
        ExtraArgs={"ContentType": "application/vnd.android.package-archive"}
    )

    return f"{public_base}/{object_name}"

def upload_to_tmpfiles(file_path: Path) -> str:
    boundary = f"----WebKitFormBoundary{uuid.uuid4().hex}"
    crlf = b"\r\n"
    filename = file_path.name
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

def upload_apk(file_path: Path) -> str:
    print(f"Uploading APK to Cloudflare R2 bucket 'filesend'...")
    try:
        url = upload_to_r2(file_path)
        print("Upload to Cloudflare R2 succeeded!")
        return url
    except Exception as e:
        print(f"R2 upload encountered an issue ({type(e).__name__}). Using fallback...")
        return upload_to_tmpfiles(file_path)

def send_ntfy(topic: str, message: str, title: str = "DoIt APK Release Ready"):
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
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8")

if __name__ == "__main__":
    apk_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("app/build/outputs/apk/release/Do-It-release.apk")
    changelog = sys.argv[2] if len(sys.argv) > 2 else "Major Upgrade v5: Smart deadlines, Tagged backup v2, Batch actions, Collapsible done section, Performance optimizations."
    
    if not apk_path.exists():
        print(f"Error: {apk_path} not found.")
        sys.exit(1)

    print(f"Processing APK: {apk_path.name} ({apk_path.stat().st_size} bytes)")
    download_url = upload_apk(apk_path)
    print(f"Public Download URL: {download_url}")

    ntfy_body = f"🚀 Link tải trực tiếp file APK (1-chạm cài đặt): {download_url}\n\n📝 Changelog:\n{changelog}"
    print("Sending ntfy notification to topic 'filesend'...")
    send_ntfy("filesend", ntfy_body, title="DoIt APK Release Ready")
    print("Notification sent successfully!")
