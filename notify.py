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
        
        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': 'https://status-saver-92d48-default-rtdb.firebaseio.com/'
            })
        
        print("✅ Firebase initialized successfully.")

    except Exception as e:
        print(f"❌ Initialization Error: {e}")
        return

    # 2. Notification bhejne ka function
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
            data={
                'statusId': str(status_id),
                'type': 'expiry_alert'
            },
            token=token,
        )
        try:
            response = messaging.send(message)
            print(f'🚀 Successfully sent notification for Status {status_id}: {response}')
            return True
        except Exception as e:
            print(f'❌ FCM Error for {status_id}: {e}')
            return False

    # 3. Database Check aur Action Logic
    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()

        if not data:
            print("ℹ️ No alerts found in database.")
            return

        current_time_ms = int(time.time() * 1000)
        one_hour_ms = 3600000 
        notification_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict):
                continue
                
            for s_id, s_info in statuses.items():
                expiry_raw = s_info.get('expiryTime', 0)
                expiry_ms = 0

                # 🔥 Text Date ko Number (ms) mein convert karne ka logic
                if isinstance(expiry_raw, str):
                    try:
                        # Format match: "04 May 2026 2:25:56 AM"
                        # Note: %-I handle karta hai single digit hour (2) ko
                        dt = datetime.strptime(expiry_raw, "%d %b %Y %I:%M:%S %p")
                        expiry_ms = int(dt.timestamp() * 1000)
                    except Exception as parse_error:
                        # Agar format mein thoda boht farq ho (e.g. 02 instead of 2)
                        try:
                            dt = datetime.strptime(expiry_raw, "%d %b %Y %H:%M:%S")
                            expiry_ms = int(dt.timestamp() * 1000)
                        except:
                            print(f"⚠️ Date format error for {s_id}: {expiry_raw}")
                            continue
                else:
                    expiry_ms = expiry_raw

                already_notified = s_info.get('notified', False)
                time_left = expiry_ms - current_time_ms

                # DEBUG: Check karne k liye k kitna time bacha h
                # print(f"Status {s_id}: {time_left/60000:.2f} mins remaining.")

                # CASE 1: Notification logic
                if 0 < time_left <= one_hour_ms and not already_notified:
                    success = send_notification(token, s_id)
                    if success:
                        ref.child(token).child(s_id).update({'notified': True})
                        notification_count += 1
                
                # CASE 2: Cleanup logic
                elif time_left < 0:
                    ref.child(token).child(s_id).delete()
                    print(f"🗑️ Removed expired status: {s_id}")

        print(f"✅ Process finished. Total notifications sent: {notification_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
