import requests
import json
import os
import time
import threading
from flask import Flask, jsonify, request
import firebase_admin
from firebase_admin import credentials, messaging
from bs4 import BeautifulSoup

app = Flask(__name__)

# Шляхи
CONFIG_PATH = '/home/aiuser/code/alert_backend/config.json'
FIREBASE_KEY_PATH = '/home/aiuser/code/alert_backend/firebase-key.json'

# Ініціалізація Firebase
if not firebase_admin._apps:
    cred = credentials.Certificate(FIREBASE_KEY_PATH)
    firebase_admin.initialize_app(cred)

# Словник регіонів для парсингу (можна доповнювати)
REGION_MAP = {
    "Вінницька": "1",
    "Волинська": "2",
    "Дніпропетровська": "3",
    "Донецька": "4",
    "Житомирська": "5",
    "Закарпатська": "6",
    "Запорізька": "7",
    "Івано-Франківська": "8",
    "Київська": "9",
    "Кіровоградська": "10",
    "Луганська": "11",
    "Львівська": "12",
    "Миколаївська": "13",
    "Одеська": "14",
    "Полтавська": "15",
    "Рівненська": "16",
    "Сумська": "17",
    "Тернопільська": "18",
    "Харківська": "19",
    "Херсонська": "20",
    "Хмельницька": "21",
    "Черкаська": "22",
    "Чернівецька": "23",
    "Чернігівська": "24",
    "м. Київ": "25",
    "АР Крим": "26"
}

last_state = {} # region_id: status (True/False)

def send_push(region_id, action):
    """Шлемо alert_on або alert_off"""
    topic = f"region_{region_id}"
    message = messaging.Message(
        data={'action': action, 'region_id': str(region_id)},
        topic=topic,
    )
    try:
        messaging.send(message)
        print(f"Sent {action} to {topic}")
    except Exception as e:
        print(f"Firebase Error: {e}")

def parse_telegram():
    """Парсимо офіційний ТГ канал через веб-прев'ю"""
    global last_state
    try:
        url = "https://t.me/s/air_alert_ua"
        headers = {'User-Agent': 'Mozilla/5.0'}
        res = requests.get(url, headers=headers, timeout=10)
        if res.status_code != 200: return

        soup = BeautifulSoup(res.text, 'lxml')
        messages = soup.find_all('div', class_='tgme_widget_message_text')
        
        if not messages: return

        # Визначаємо ліміт повідомлень: 50 для початкової ініціалізації, 5 для чергових перевірок
        is_initial = len(last_state) == 0
        limit = 50 if is_initial else 5
        recent_texts = [m.get_text() for m in messages[-limit:]]
        
        for region_name, r_id in REGION_MAP.items():
            old_status = last_state.get(r_id, False)
            is_alert = old_status # Зберігаємо попередній стан
            
            # Шукаємо за ключовим словом (коренем назви) для гнучкості
            search_keyword = region_name.replace("ська", "").replace("ька", "").strip()
            
            for text in reversed(recent_texts):
                text_upper = text.upper()
                if search_keyword.upper() in text_upper:
                    if "ПОВІТРЯНА ТРИВОГА" in text_upper or "ЗАГРОЗА" in text_upper:
                        is_alert = True
                        break
                    if "ВІДБІЙ" in text_upper:
                        is_alert = False
                        break
            
            # Перевіряємо чи змінився стан
            if is_alert != old_status:
                action = "alert_on" if is_alert else "alert_off"
                send_push(r_id, action)
                last_state[r_id] = is_alert

    except Exception as e:
        print(f"Parser Error: {e}")

def loop():
    while True:
        parse_telegram()
        time.sleep(10) # Кожні 10 секунд

@app.route('/status', methods=['GET'])
def get_status():
    r_id = request.args.get('region_id', '18')
    status = last_state.get(r_id, False)
    return jsonify({"region_id": r_id, "alert": status})

if __name__ == '__main__':
    # Одразу ініціалізуємо стан при старті перед запуском Flask
    print("[*] Initializing air alert states from Telegram history...")
    parse_telegram()
    print(f"[+] Initialization complete. Current states: {last_state}")
    
    threading.Thread(target=loop, daemon=True).start()
    app.run(host='0.0.0.0', port=5000)
