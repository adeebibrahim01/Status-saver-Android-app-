import requests
import firebase_admin
from firebase_admin import credentials, db
import xml.etree.ElementTree as ET
import os
import json
import time
import logging
import random
import urllib.parse

# Logging configuration
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Constants
PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"

# Google News mapping
NEWS_GEO_MAP = {
    "US": "ceid=US:en",
    "IN": "ceid=IN:en",
    "PK": "ceid=PK:en",
    "GB": "ceid=GB:en",
    "BR": "ceid=BR:pt-419",
    "ID": "ceid=ID:id",
    "AE": "ceid=AE:en",
    "SA": "ceid=SA:ar",
    "TR": "ceid=TR:tr",  # Added Turkey for more variety
    "DE": "ceid=DE:de"   # Added Germany
}

def initialize_firebase():
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
                logging.error("FIREBASE_SERVICE_ACCOUNT not found.")
                return False
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            logging.info("Firebase Initialized.")
        return True
    except Exception as e:
        logging.error(f"Firebase Init Error: {e}")
        return False

def get_trending_from_news(ceid_param):
    url = f"https://news.google.com/rss?{ceid_param}"
    headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/124.0.0.0'}
    try:
        response = requests.get(url, headers=headers, timeout=20)
        if response.status_code == 200:
            root = ET.fromstring(response.content)
            keywords = []
            # Har country se 20 potential keywords uthayen
            for item in root.findall('.//item')[:20]:
                title = item.find('title').text
                if title:
                    clean_kw = title.split('-')[0].strip()
                    short_kw = " ".join(clean_kw.split()[:3])
                    keywords.append(short_kw)
            return list(set(keywords))
        return []
    except Exception as e:
        logging.error(f"News Fetch Error: {e}")
        return []

def fetch_pexels_media(keyword):
    if not PEXELS_API_KEY: return None
    encoded_kw = urllib.parse.quote(keyword)
    url = f"https://api.pexels.com/v1/search?query={encoded_kw}+aesthetic&orientation=portrait&per_page=1"
    headers = {"Authorization": PEXELS_API_KEY}
    try:
        response = requests.get(url, headers=headers, timeout=15)
        if response.status_code == 200:
            data = response.json()
            if data.get('photos'):
                photo = data['photos'][0]
                return {
                    "title": keyword,
                    "thumbnailUrl": photo['src']['large'],
                    "mediaUrl": photo['src']['original'],
                    "mediaType": "image"
                }
    except Exception as e:
        pass
    return None

def main():
    if not initialize_firebase(): return

    # Clear old data
    db.reference('/trending_status').delete()

    all_media_pool = []

    # Har country se dynamic data collect karein
    for code, ceid in NEWS_GEO_MAP.items():
        logging.info(f"Checking Trends for: {code}")
        trends = get_trending_from_news(ceid)

        country_items = []
        for kw in trends:
            # Har country se max 10 items lenge taake koi ek mulk dominate na kare
            if len(country_items) >= 10: break

            item = fetch_pexels_media(kw)
            if item:
                country_items.append(item)
                all_media_pool.append(item)
                time.sleep(0.8) # Smooth rate limiting

        # Individual country node update
        if country_items:
            db.reference(f'/trending_status/{code}').set(country_items)

    # FINAL STEP: GLOBAL Feed (MIX OF WORLD TRENDS)
    if all_media_pool:
        # Duniya bhar ke trends ko mix karein
        random.shuffle(all_media_pool)

        # Ab top 48 pick karein jo poori dunya ka mix hoga
        final_global_data = all_media_pool[:48]

        db.reference('/trending_status/GLOBAL').set(final_global_data)

        # Update Metadata
        db.reference('/trending_status/metadata').set({
            "last_updated": time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime()),
            "total_items": len(final_global_data),
            "source_count": len(NEWS_GEO_MAP)
        })

        logging.info(f"Global feed updated with {len(final_global_data)} mixed world items.")

if __name__ == "__main__":
    main()
