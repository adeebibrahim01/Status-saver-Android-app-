import requests
import firebase_admin
from firebase_admin import credentials, db
from pytrends.request import TrendReq
import os
import json
import time
import logging
import random

# Logging configuration
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Constants
PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"

# Country mapping for PyTrends
GEO_MAP = {
    "US": "united_states",
    "IN": "india",
    "PK": "pakistan",
    "GB": "united_kingdom",
    "BR": "brazil",
    "ID": "indonesia",
    "AE": "united_arab_emirates",
    "SA": "saudi_arabia"
}

def initialize_firebase():
    """Firebase initialization with environment secret safety check"""
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
                logging.error("FIREBASE_SERVICE_ACCOUNT secret is missing!")
                return False
            
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            logging.info("Firebase Initialized Successfully.")
        return True
    except Exception as e:
        logging.error(f"Firebase Init Error: {e}")
        return False

def get_trends_stable(country_name):
    """Fetches trending searches using PyTrends with reliable headers"""
    try:
        # hl='en-US' for English results, tz=360 for timezone offset
        pytrends = TrendReq(hl='en-US', tz=360, timeout=(10,25))
        df = pytrends.trending_searches(pn=country_name)
        keywords = df[0].tolist()
        
        logging.info(f"Fetched {len(keywords)} trends for {country_name}")
        return keywords[:12]
    except Exception as e:
        logging.error(f"PyTrends Error for {country_name}: {e}")
        return []

def fetch_pexels_media(keyword):
    """Fetches a high-quality vertical image from Pexels API"""
    if not PEXELS_API_KEY:
        logging.error("PEXELS_API_KEY is missing!")
        return None
    
    # Refining query for status/wallpaper style
    query = f"{keyword} wallpaper portrait"
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
        else:
            logging.warning(f"Pexels API returned status {response.status_code} for {keyword}")
    except Exception as e:
        logging.error(f"Pexels Request Error: {e}")
    return None

def update_global_database():
    """Main execution logic to update Firebase nodes by country"""
    if not initialize_firebase():
        return

    all_global_items = []

    for code, full_name in GEO_MAP.items():
        logging.info(f"--- Processing: {full_name} ---")
        trends = get_trends_stable(full_name)
        
        if not trends:
            continue

        country_media_list = []
        for kw in trends:
            if len(country_media_list) >= 6:
                break
            
            media_item = fetch_pexels_media(kw)
            if media_item:
                country_media_list.append(media_item)
                # Build global list as fallback
                if len(all_global_items) < 30:
                    all_global_items.append(media_item)
                time.sleep(1.5) # Protect Pexels rate limits

        if country_media_list:
            try:
                db.reference(f'/trending_status/{code}').set(country_media_list)
                logging.info(f"Firebase node '{code}' updated.")
            except Exception as e:
                logging.error(f"Firebase Update Error for {code}: {e}")

    # Update GLOBAL fallback node
    if all_global_items:
        try:
            random.shuffle(all_global_items)
            db.reference('/trending_status/GLOBAL').set(all_global_items)
            logging.info("GLOBAL fallback node updated successfully.")
        except Exception as e:
            logging.error(f"Global Fallback Error: {e}")

if __name__ == "__main__":
    update_global_database()
