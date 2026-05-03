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
            print("❌ Error: Secret FIREBASE_SERVICE_ACCOUNT missing.")
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
            data={'statusId': str(status_id), 'type': 'expiry_alert'},
            token=token,
        )
        try:
            messaging.send(message)
            return True
        except Exception as e:
            print(f"❌ FCM Error: {e}")
            return False

    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()

        if not data:
            print("ℹ️ No alerts in database.")
            return

        current_time_ms = int(time.time() * 1000)
        one_hour_ms = 3600000 
        notification_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict): continue
            
            for s_id, s_info in statuses.items():
                expiry_raw = s_info.get('expiryTime', 0)
                expiry_ms = 0

                # 🔥 Matching Android Format: "04 May 2026 02:25:56 AM"
                if isinstance(expiry_raw, str):
                    try:
                        # Strip spaces and parse
                        dt = datetime.strptime(expiry_raw.strip(), "%d %b %Y %I:%M:%S %p")
                        expiry_ms = int(dt.timestamp() * 1000)
                    except:
                        print(f"⚠️ Format Mismatch: {expiry_raw}")
                        continue
                else:
                    expiry_ms = expiry_raw

                already_notified = s_info.get('notified', False)
                time_left = expiry_ms - current_time_ms

                # Notification logic
                if 0 < time_left <= one_hour_ms and not already_notified:
                    if send_notification(token, s_id):
                        ref.child(token).child(s_id).update({'notified': True})
                        notification_count += 1
                        print(f"🚀 Sent to Status ID: {s_id}")
                
                # Cleanup
                elif time_left < 0:
                    ref.child(token).child(s_id).delete()
                    print(f"🗑️ Cleaned up expired ID: {s_id}")

        print(f"✅ Run Complete. Sent: {notification_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
