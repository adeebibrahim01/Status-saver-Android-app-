import os
import json
import time
from datetime import datetime, timezone
import firebase_admin
from firebase_admin import credentials, db, messaging

def run_notifier():
    try:
        # 1. Credentials & Firebase Initialization
        service_account_env = os.environ.get('FIREBASE_SERVICE_ACCOUNT')
        if not service_account_env:
            print("❌ Error: Secret 'FIREBASE_SERVICE_ACCOUNT' missing.")
            return

        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        
        project_id = "status-saver-92d48"

        if not firebase_admin._apps:
            firebase_admin.initialize_app(cred, {
                'databaseURL': f'https://{project_id}-default-rtdb.firebaseio.com/',
                'projectId': project_id
            })
        print(f"✅ Firebase Ready (Universal UTC Mode): {project_id}")

    except Exception as e:
        print(f"❌ Init Error: {e}")
        return

    def send_notification(token, status_id):
        # Professional Notification Payload
        message = messaging.Message(
            notification=messaging.Notification(
                title='Status Expiring Soon! ⏳',
                body='One of your viewed statuses is about to expire. Save it now!',
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
            print("ℹ️ Empty DB. No actions needed.")
            return

        # ✅ World Wide Generic UTC Time
        now_ms = int(time.time() * 1000)
        one_hour_ms = 3600000 
        sent_count = 0

        # Naya Structure: device_id -> status_id -> details
        for device_id, statuses in data.items():
            if not isinstance(statuses, dict):
                continue
            
            for s_id, s_info in statuses.items():
                expiry_str = s_info.get('expiryTime', '')
                token = s_info.get('token')
                
                if not expiry_str or not token:
                    continue

                try:
                    # Parsing UTC string from Android
                    dt = datetime.strptime(expiry_str.strip(), "%d %b %Y %I:%M:%S %p")
                    dt = dt.replace(tzinfo=timezone.utc) 
                    expiry_ms = int(dt.timestamp() * 1000)
                except Exception as parse_error:
                    print(f"⚠️ Parse Error for {s_id}: {parse_error}")
                    continue

                already_notified = s_info.get('notified', False)
                time_diff = expiry_ms - now_ms
                
                print(f"🔍 Checking Device [{device_id}] Status [{s_id}]: {time_diff/60000:.2f} mins left")

                # 1. Notification Logic
                if 0 < time_diff <= one_hour_ms and not already_notified:
                    if send_notification(token, s_id):
                        # Device ID aur Status ID dono use kar k update karna h
                        ref.child(device_id).child(s_id).update({'notified': True})
                        sent_count += 1
                
                # 2. Cleanup Logic
                elif time_diff < 0:
                    ref.child(device_id).child(s_id).delete()
                    print(f"🗑️ Cleaned Expired: {s_id} from Device: {device_id}")

        print(f"✅ Final Processed Count: {sent_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
