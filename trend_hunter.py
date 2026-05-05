import requests
import os
import json
import firebase_admin
from firebase_admin import credentials, db

PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"

def initialize_firebase():
    if not firebase_admin._apps:
        service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
        service_account_info = json.loads(service_account_env)
        cred = credentials.Certificate(service_account_info)
        firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})

def fetch_pexels_media(keyword):
    """Images aur Videos dono dhoondta hai"""
    headers = {"Authorization": PEXELS_API_KEY}
    
    # --- Pehle Image Dhoondte hain (Focus) ---
    img_url = f"https://api.pexels.com/v1/search?query={keyword}&orientation=portrait&per_page=1"
    try:
        img_res = requests.get(img_url, headers=headers).json()
        if img_res.get('photos'):
            photo = img_res['photos'][0]
            return {
                "title": keyword,
                "thumbnailUrl": photo['src']['large'],
                "mediaUrl": photo['src']['original'], # Image ka main link
                "mediaType": "image"
            }
    except Exception as e:
        print(f"Image Error: {e}")

    # --- Agar Image na mile toh Video (Backup) ---
    # Note: Pexels video links expire ho jate hain, isliye Image behtar hai
    return None 

def update_firebase():
    initialize_firebase()
    # Aap trends ki jagah fixed categories bhi rakh sakte hain images ke liye
    keywords = ["Nature", "Architecture", "Abstract", "Aesthetic", "Minimal"] 
    
    trending_list = []
    for kw in keywords:
        item = fetch_pexels_media(kw)
        if item:
            trending_list.append(item)
    
    if trending_list:
        ref = db.reference('/trending_status')
        ref.set(trending_list)
        print("Firebase updated with HD Images!")

if __name__ == "__main__":
    update_firebase()
