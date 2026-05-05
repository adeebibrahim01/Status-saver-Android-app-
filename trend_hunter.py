import requests
import firebase_admin
from firebase_admin import credentials, db
from pytrends.request import TrendReq # Nayi library
import os
import json
import time
import logging
import random

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"
COUNTRIES = ["united_states", "india", "pakistan", "united_kingdom", "brazil", "indonesia", "united_arab_emirates", "saudi_arabia"]

def initialize_firebase():
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            logging.info("Firebase Initialized Successfully.")
    except Exception as e:
        logging.error(f"Firebase Init Error: {e}")
        return False
    return True

def get_trends_stable(country_name):
    """PyTrends ka istemal karte hue trending searches nikalta hai"""
    try:
        # Har request ke liye naya instance taake cookies fresh rahein
        pytrends = TrendReq(hl='en-US', tz=360, timeout=(10,25))
        
        # Real-time trending searches (Pytrends 404 se bachta hai)
        df = pytrends.trending_searches(pn=country_name)
        keywords = df[0].tolist()
        
        logging.info(f"Successfully fetched {len(keywords)} trends for {country_name}")
        return keywords[:12]
    except Exception as e:
        logging.error(f"PyTrends Error for {country_name}: {e}")
        return []

def fetch_pexels_media(keyword):
    if not PEXELS_API_KEY: return None
    
    # Query ko mazeed status-friendly banayein
    query = f"{keyword} wallpaper 4k"
    url = f"https://api.pexels.com/v1/search?query={query}&orientation=portrait&per_page=1"
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
        logging.error(f"Pexels Error: {e}")
    return None

def update_global_database():
    if not initialize_firebase(): return

    # Country mapping for PyTrends (Ye names full hone chahiye)
    geo_map = {
        "US": "united_states",
        "IN": "india",
        "PK": "pakistan",
        "GB": "united_kingdom",
        "BR": "brazil",
        "ID": "indonesia",
        "AE": "united_arab_emirates",
        "SA": "saudi_arabia"
    }

    all_global_items = []

    for code, full_name in geo_map.items():
        logging.info(f"--- Processing: {full_name} ---")
        trends = get_trends_stable(full_name)
        
        if not trends:
            continue

        country_media_list = []
        for kw in trends:
            if len(country_media_list) >= 6: break
            
            media_item = fetch_pexels_media(kw)
            if media_item:
                country_media_list.append(media_item)
                if len(all_global_items) < 30:
                    all_global_items.append(media_item)
                time.sleep(1) # Pexels rate limit safety

        if country_media_list:
            db.reference(f'/trending_status/{code}').set(country_media_list)
            logging.info(f"Updated Firebase for {code}")

    # GLOBAL node update
    if all_global_items:
        random.shuffle(all_global_items)
        db.reference('/trending_status/GLOBAL').set(all_global_items)
        logging.info("GLOBAL Fallback Node Updated.")

if __name__ == "__main__":
    update_global_database()
