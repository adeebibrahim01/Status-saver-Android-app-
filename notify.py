import os
import json
import time
from datetime import datetime
import firebase_admin
from firebase_admin import credentials, db, messaging

def run_notifier():
    try:
        service_account_env = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if not service_account_env:
            print("❌ Error: Secret missing.")
            return

        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        
        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': 'https://status-saver-92d48-default-rtdb.firebaseio.com/'
            })
        print("✅ Firebase initialized.")

    except Exception as e:
        print(f"❌ Initialization Error: {e}")
        return

    def send_notification(token, status_id):
        # Bilkul basic message taake pehle permission test ho jaye
        message = messaging.Message(
            notification=messaging.Notification(
                title='Status Expiring! ⏳',
                body='Save it before it disappears.',
            ),
            token=token,
        )
        try:
            response = messaging.send(message)
            print(f"🚀 Success: {response}")
            return True
        except Exception as e:
            print(f"❌ Permission/FCM Error: {e}")
            return False

    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()
        if not data:
            print("ℹ️ No data.")
            return

        current_time_ms = int((time.time() + (5 * 3600)) * 1000)
        one_hour_ms = 3600000 
        notification_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict): continue
            for s_id, s_info in statuses.items():
                expiry_raw = s_info.get('expiryTime', '')
                try:
                    dt = datetime.strptime(expiry_raw.strip(), "%d %b %Y %I:%M:%S %p")
                    expiry_ms = int(dt.timestamp() * 1000)
                except: continue

                time_left = expiry_ms - current_time_ms
                print(f"🔍 Checking {s_id}: {time_left/60000:.2f} mins left")

                if 0 < time_left <= one_hour_ms and not s_info.get('notified', False):
                    if send_notification(token, s_id):
                        ref.child(token).child(s_id).update({'notified': True})
                        notification_count += 1
                elif time_left < 0:
                    ref.child(token).child(s_id).delete()

        print(f"✅ Sent: {notification_count}")
    except Exception as e:
        print(f"❌ DB Error: {e}")

if __name__ == "__main__":
    run_notifier()
