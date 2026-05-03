import requests
import json
import time

# Firebase Details
DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com/StatusAlerts.json"
FCM_URL = "https://fcm.googleapis.com/fcm/send"
SERVER_KEY = "YOUR_FCM_SERVER_KEY" # Firebase Console > Project Settings > Cloud Messaging se milega

def send_notification(token, status_id):
    headers = {
        'Content-Type': 'application/json',
        'Authorization': 'key=' + SERVER_KEY
    }
    body = {
        "to": token,
        "notification": {
            "title": "Status Expiring Soon! ⏳",
            "body": "Your viewed status will be gone in 1 hour. Save it now!",
            "click_action": "OPEN_GALLERY"
        },
        "data": {
            "statusId": status_id
        }
    }
    requests.post(FCM_URL, headers=headers, data=json.dumps(body))

# Firebase se data read karna
response = requests.get(DB_URL)
data = response.json()

if data:
    current_time = int(time.time() * 1000)
    one_hour_ms = 3600000

    for token, statuses in data.items():
        for s_id, s_info in statuses.items():
            expiry = s_info.get('expiryTime', 0)
            # Agar expiry mein 1 ghanta ya us se kam bacha hai
            if 0 < (expiry - current_time) <= one_hour_ms:
                send_notification(token, s_id)
                print(f"Notification sent to {token} for {s_id}")