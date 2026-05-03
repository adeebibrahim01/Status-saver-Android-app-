import os
import json
import time
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
                    color='#FFD700' # Golden color for Premium feel
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
            print(f'🚀 Notification sent for Status {status_id}: {response}')
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

        current_time = int(time.time() * 1000)
        one_hour_ms = 3600000 
        notification_count = 0

        for token, statuses in data.items():
            for s_id, s_info in statuses.items():
                # Handling safe retrieval of expiryTime
                expiry = s_info.get('expiryTime', 0)
                if not isinstance(expiry, (int, float)):
                    continue # Skip if date is saved as String text

                already_notified = s_info.get('notified', False)
                time_left = expiry - current_time

                # CASE 1: 1 ghante se kam bacha hai aur pehle notification nahi bheji
                if 0 < time_left <= one_hour_ms and not already_notified:
                    success = send_notification(token, s_id)
                    if success:
                        # Database mein 'notified' true kar dein taake dobara na jaye
                        ref.child(token).child(s_id).update({'notified': True})
                        notification_count += 1
                
                # CASE 2: Status expire ho chuka hai (Cleanup)
                elif time_left < 0:
                    ref.child(token).child(s_id).remove()
                    print(f"🗑️ Removed expired status: {s_id}")

        print(f"✅ Process completed. Notifications sent in this run: {notification_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
