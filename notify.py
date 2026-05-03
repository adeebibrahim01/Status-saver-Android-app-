import os
import json
import time
from datetime import datetime
import firebase_admin
from firebase_admin import credentials, db, messaging

def run_notifier():
    try:
        # 1. GitHub Secrets se Service Account load karna
        service_account_env = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if not service_account_env:
            print("❌ Error: FIREBASE_SERVICE_ACCOUNT secret not found.")
            return

        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        
        # Project ID explicitly nikalna permissions ke liye
        project_id = service_account_info.get('project_id')

        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': 'https://status-saver-92d48-default-rtdb.firebaseio.com/',
                'projectId': project_id
            })
        print(f"✅ Firebase initialized for project: {project_id}")

    except Exception as e:
        print(f"❌ Initialization Error: {e}")
        return

    # 2. Notification bhejne ka function
    def send_notification(token, status_id):
        # High priority notification config
        message = messaging.Message(
            notification=messaging.Notification(
                title='Status Expiring Soon! ⏳',
                body='Your viewed status will be gone in 1 hour. Save it now!',
            ),
            android=messaging.AndroidConfig(
                priority='high',
                notification=messaging.AndroidNotification(
                    channel_id='status_alerts_channel',
                    icon='stock_ticker_update',
                    color='#FFD700'
                ),
            ),
            data={
                'statusId': str(status_id),
                'type': 'expiry_alert'
            },
            token=token,
        )
        try:
            response = messaging.send(message)
            print(f"🚀 Successfully sent notification: {response}")
            return True
        except Exception as e:
            # Agar abhi bhi permission error aaye toh detail print hogi
            print(f"❌ FCM Error for Status {status_id}: {e}")
            return False

    # 3. Main Logic
    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()

        if not data:
            print("ℹ️ No alerts found in database.")
            return

        # Pakistan Time (UTC+5) adjust karna
        current_time_ms = int((time.time() + (5 * 3600)) * 1000)
        one_hour_ms = 3600000 
        notification_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict):
                continue
                
            for s_id, s_info in statuses.items():
                expiry_raw = s_info.get('expiryTime', '')
                if not expiry_raw:
                    continue

                expiry_ms = 0
                if isinstance(expiry_raw, str):
                    try:
                        # Format: "04 May 2026 03:03:12 AM"
                        dt = datetime.strptime(expiry_raw.strip(), "%d %b %Y %I:%M:%S %p")
                        expiry_ms = int(dt.timestamp() * 1000)
                    except Exception as parse_error:
                        print(f"⚠️ Date format mismatch for {s_id}: {expiry_raw}")
                        continue
                else:
                    expiry_ms = expiry_raw

                already_notified = s_info.get('notified', False)
                time_left = expiry_ms - current_time_ms

                # Log checking
                print(f"🔍 Status {s_id}: Time Left = {time_left/60000:.2f} minutes")

                # Agar 1 ghante se kam time hai aur notification nahi bheji gayi
                if 0 < time_left <= one_hour_ms and not already_notified:
                    if send_notification(token, s_id):
                        # Database mein mark karein ke notification chali gayi h
                        ref.child(token).child(s_id).update({'notified': True})
                        notification_count += 1
                
                # Agar time khatam ho gaya h toh delete kar dein
                elif time_left < 0:
                    ref.child(token).child(s_id).delete()
                    print(f"🗑️ Expired & Deleted from DB: {s_id}")

        print(f"✅ Process finished. Total sent: {notification_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
