import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, db

# --- CONFIGURATION ---
PEXELS_API_KEY = "YOUR_PEXELS_API_KEY"
FIREBASE_DB_URL = "YOUR_FIREBASE_DATABASE_URL"
COUNTRY = "pkr" # Pakistan ke liye (ya 'in' India ke liye)

# Firebase Setup
# GitHub Actions mein hum 'serviceAccountKey.json' ko secrets se create karenge
cred = credentials.Certificate("service_account.json")
firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})

def get_google_trends():
    """Google Trends se top keywords uthata hai"""
    url = f"https://trends.google.com/trends/trendingsearches/daily/rss?geo={COUNTRY.upper()}"
    response = requests.get(url)
    root = ET.fromstring(response.content)
    keywords = []
    for item in root.findall('.//item/title'):
        keywords.append(item.text)
    return keywords[:5] # Top 5 trends kaafi hain

def fetch_pexels_videos(keyword):
    """Keyword use karke Pexels se vertical videos dhoondta hai"""
    url = f"https://api.pexels.com/videos/search?query={keyword}&orientation=portrait&per_page=1"
    headers = {"Authorization": PEXELS_API_KEY}
    response = requests.get(url, headers=headers).json()
    
    if response.get('videos'):
        video = response['videos'][0]
        return {
            "title": keyword,
            "thumbnailUrl": video['image'],
            "videoUrl": video['video_files'][0]['link'],
            "mediaType": "video"
        }
    return None

def update_firebase():
    trends = get_google_trends()
    trending_data = []
    
    for kw in trends:
        data = fetch_pexels_videos(kw)
        if data:
            trending_data.append(data)
    
    # Firebase mein data update karna
    ref = db.reference('/trending_status')
    ref.set(trending_data)
    print("Firebase Updated Successfully!")

if __name__ == "__main__":
    update_firebase()