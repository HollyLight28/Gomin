import requests
import glob
import json

def upload_and_notify():
    bot_token = "8516795662:AAG034DkCfXXwdNR468zDH0Sjh3Ofx2_L1U"
    chat_id = "7581726569"
    
    apk_pattern = "TMessagesProj_AppStandalone/build/outputs/apk/afat/debug/*.apk"
    apk_files = glob.glob(apk_pattern)
    
    if not apk_files:
        print("APK not found!")
        return
    
    apk_path = apk_files[0]
    
    # Try file.io
    print(f"Uploading {apk_path} to file.io...")
    try:
        with open(apk_path, "rb") as f:
            r = requests.post("https://file.io", files={"file": f})
            link = r.json().get("link")
            if link:
                msg = f"🚀 *Gomin Build Ready!*%0A%0A📦 [Завантажити АПК]({link})%0A%0A(Посилання діє 1 раз)"
                requests.post(f"https://api.telegram.org/bot{bot_token}/sendMessage", data={"chat_id": chat_id, "text": msg, "parse_mode": "Markdown"})
                print(f"Sent to Telegram: {link}")
                return
    except Exception as e:
        print(f"file.io failed: {e}")

    print("All upload methods failed.")

if __name__ == "__main__":
    upload_and_notify()
