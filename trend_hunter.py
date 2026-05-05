import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, db
import os
import json

# --- CONFIGURATION ---
# GitHub Secrets se dynamic values uthayega
PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"
COUNTRY = "PK" 

def initialize_firebase():
    """Firebase ko Service Account Secret ke zariye initialize karta hai"""
    if not firebase_admin._apps:
        try:
            # GitHub Secrets mein save kiye gaye JSON string ko load karna
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
                raise ValueError("FIREBASE_SERVICE_ACCOUNT secret not found!")
            
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            print("Firebase Initialized Successfully")
        except Exception as e:
            print(f"Firebase Init Error: {e}")
            exit(1)

def get_google_trends():
    """Google Trends RSS se keywords nikalta hai"""
    url = f"https://trends.google.com/trends/trendingsearches/daily/rss?geo={COUNTRY}"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }
    
    try:
        response = requests.get(url, headers=headers)
        if response.status_code != 200:
            return ["Status Video", "Trending Now", "Viral Status"]
            
        root = ET.fromstring(response.content)
        keywords = []
        for item in root.findall('.//item'):
            title = item.find('title').text
            if title:
                keywords.append(title)
        
        return keywords[:10] # Top 10 trends backup ke liye
    except Exception as e:
        print(f"Trend Fetch Error: {e}")
        return ["WhatsApp Status", "Motivation", "Funny Status"]

def fetch_pexels_videos(keyword):
    """Pexels API se portrait video dhoondta hai"""
    if not PEXELS_API_KEY:
        print("Pexels API Key Missing!")
        return None
        
    url = f"https://api.pexels.com/videos/search?query={keyword}&orientation=portrait&per_page=1"
    headers = {"Authorization": PEXELS_API_KEY}
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        data = response.json()
        
        if data.get('videos') and len(data['videos']) > 0:
            video = data['videos'][0]
            # HD link dhoondna
            video_url = video['video_files'][0]['link']
            for f in video['video_files']:
                if f['quality'] == 'hd':
                    video_url = f['link']
                    break
            
            return {
                "title": keyword,
                "thumbnailUrl": video['image'],
                "videoUrl": video_url,
                "mediaType": "video"
            }
    except Exception as e:
        print(f"Pexels Error for {keyword}: {e}")
    return None

def update_firebase():
    initialize_firebase()
    trends = get_google_trends()
    print(f"Processing Trends: {trends}")
    
    trending_list = []
    for kw in trends:
        # Stop if we already have 6 good videos
        if len(trending_list) >= 6:
            break
            
        print(f"Fetching media for: {kw}")
        item = fetch_pexels_videos(kw)
        if item:
            trending_list.append(item)
    
    if trending_list:
        ref = db.reference('/trending_status')
        ref.set(trending_list)
        print(f"Done! {len(trending_list)} trends updated in Firebase.")
    else:
        print("No videos found to update.")

if __name__ == "__main__":
    update_firebase()
