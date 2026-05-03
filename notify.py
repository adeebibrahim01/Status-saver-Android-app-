import os
import json
import time
from datetime import datetime
import firebase_admin
from firebase_admin import credentials, db, messaging

def run_notifier():
    try:
        # 1. Credentials Load
        service_account_env = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if not service_account_env:
            print("❌ Secret missing.")
            return

        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        
        # Explicit Project ID
        project_id = "status-saver-92d48"

        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': f'https://{project_id}-default-rtdb.firebaseio.com/',
                'projectId': project_id
            })
        print(f"✅ Firebase Ready: {project_id}")

    except Exception as e:
        print(f"❌ Init Error: {e}")
        return

    def send_notification(token, status_id):
        try:
            # Simple message for testing
            message = messaging.Message(
                notification=messaging.Notification(
                    title='Expiry Alert! ⏳',
                    body='Save your status now!',
                ),
                token=token
            )
            response = messaging.send(message)
            print(f"🚀 SUCCESS: {response}")
            return True
        except Exception as e:
            print(f"❌ FCM ERROR: {str(e)}")
            return False

    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()
        if not data:
            print("ℹ️ Empty DB.")
            return

        # Pakistan Time Adjustment
        now_ms = int((time.time() + (5 * 3600)) * 1000)
        one_hour = 3600000 
        sent_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict): continue
            for s_id, s_info in statuses.items():
                expiry_str = s_info.get('expiryTime', '')
                try:
                    dt = datetime.strptime(expiry_str.strip(), "%d %b %Y %I:%M:%S %p")
                    expiry_ms = int(dt.timestamp() * 1000)
                except: continue

                time_diff = expiry_ms - now_ms
                print(f"🔍 Checking {s_id}: {time_diff/60000:.2f} mins left")

                if 0 < time_diff <= one_hour and not s_info.get('notified', False):
                    if send_notification(token, s_id):
                        ref.child(token).child(s_id).update({'notified': True})
                        sent_count += 1
                elif time_diff < 0:
                    ref.child(token).child(s_id).delete()

        print(f"✅ Sent: {sent_count}")

    except Exception as e:
        print(f"❌ DB Error: {e}")

if __name__ == "__main__":
    run_notifier()
