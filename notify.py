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
        print(f"✅ Firebase Ready (UTC Universal Mode): {project_id}")

    except Exception as e:
        print(f"❌ Init Error: {e}")
        return

    def send_notification(token, status_id):
        # Notification Payload
        message = messaging.Message(
            notification=messaging.Notification(
                title='Status Expiring Soon! ⏳',
                body='Your viewed status will be gone soon. Save it now!',
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

        # ✅ WORLDWIDE GENERIC: Current UTC Time (Seconds to Milliseconds)
        # time.time() hamesha UTC return karta hai
        now_ms = int(time.time() * 1000)
        one_hour_ms = 3600000 
        sent_count = 0

        for token, statuses in data.items():
            if not isinstance(statuses, dict): continue
            
            for s_id, s_info in statuses.items():
                expiry_str = s_info.get('expiryTime', '')
                if not expiry_str: continue

                try:
                    # ✅ Parsing string and forcing UTC interpretation
                    dt = datetime.strptime(expiry_str.strip(), "%d %b %Y %I:%M:%S %p")
                    dt = dt.replace(tzinfo=timezone.utc) 
                    expiry_ms = int(dt.timestamp() * 1000)
                except Exception as parse_error:
                    print(f"⚠️ Date Parse Error for {s_id}: {parse_error}")
                    continue

                already_notified = s_info.get('notified', False)
                time_diff = expiry_ms - now_ms
                
                # Log for transparency
                print(f"🔍 Checking {s_id}: {time_diff/60000:.2f} mins remaining (Global UTC)")

                # Notification logic (within 60 mins and not yet notified)
                if 0 < time_diff <= one_hour_ms and not already_notified:
                    if send_notification(token, s_id):
                        ref.child(token).child(s_id).update({'notified': True})
                        sent_count += 1
                
                # Cleanup logic (if time has passed)
                elif time_diff < 0:
                    ref.child(token).child(s_id).delete()
                    print(f"🗑️ Cleaned/Deleted Expired: {s_id}")

        print(f"✅ Final Processed Count: {sent_count}")

    except Exception as e:
        print(f"❌ Database Error: {e}")

if __name__ == "__main__":
    run_notifier()
