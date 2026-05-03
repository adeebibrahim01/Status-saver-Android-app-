import os
import json
import time
import firebase_admin
from firebase_admin import credentials, db, messaging

def run_notifier():
    # 1. GitHub Secrets se Service Account JSON load karna
    # Iske liye GitHub Repo Settings mein 'FIREBASE_SERVICE_ACCOUNT' name se secret hona chahiye
    try:
        service_account_env = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if not service_account_env:
            print("Error: FIREBASE_SERVICE_ACCOUNT secret not found.")
            return

        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        
        # 2. Firebase Admin SDK initialize karna
        # Note: 'databaseURL' aapki Firebase console se li gayi hai
        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': 'https://status-saver-92d48-default-rtdb.firebaseio.com'
            })
        
        print("Firebase initialized successfully.")

    except Exception as e:
        print(f"Initialization Error: {e}")
        return

    # 3. Notification bhejne ka function (FCM HTTP v1)
    def send_notification(token, status_id):
        message = messaging.Message(
            notification=messaging.Notification(
                title='Status Expiring Soon! ⏳',
                body='Your viewed status will be gone in 1 hour. Save it now!',
            ),
            # Android specific settings taake notification background mein sahi nazar aaye
            android=messaging.AndroidConfig(
                priority='high',
                notification=messaging.AndroidNotification(
                    channel_id='status_alerts_channel', # App mein ye channel ID hona chahiye
                    icon='stock_ticker_update',
                    color='#FFD700' # Golden color as per your preference
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
            print(f'Successfully sent notification to token ending in ...{token[-5:]}: {response}')
        except Exception as e:
            print(f'Error sending notification: {e}')

    # 4. Database read karna aur expiry check karna
    try:
        ref = db.reference('StatusAlerts')
        data = ref.get()

        if not data:
            print("No alerts found in database.")
            return

        current_time = int(time.time() * 1000)
        one_hour_ms = 3600000 # 1 Hour in milliseconds
        notification_count = 0

        for token, statuses in data.items():
            for s_id, s_info in statuses.items():
                expiry = s_info.get('expiryTime', 0)
                
                # Logic: Agar expiry mein 1 ghanta ya us se kam bacha hai
                # Aur wo status abhi expire nahi hua (expiry > current_time)
                if 0 < (expiry - current_time) <= one_hour_ms:
                    send_notification(token, s_id)
                    notification_count += 1
        
        print(f"Process completed. Total notifications sent: {notification_count}")

    except Exception as e:
        print(f"Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
