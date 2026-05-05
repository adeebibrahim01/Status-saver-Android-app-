import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, db
import os
import json
import time
import logging
import random

# Logging setup
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# CONFIGURATION
PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"
# Supported Countries
COUNTRIES = ["US", "IN", "PK", "GB", "BR", "ID", "AE", "SA"]

def initialize_firebase():
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
                raise EnvironmentError("FIREBASE_SERVICE_ACCOUNT Secret missing!")
            
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            logging.info("Firebase Initialized Successfully.")
    except Exception as e:
        logging.error(f"Critical Firebase Init Error: {e}")
        return False
    return True

def fetch_pexels_media(keyword):
    """Pexels API with high-res logic and better query refinement"""
    if not PEXELS_API_KEY:
        logging.error("Pexels API Key is missing!")
        return None

    # Status content ko khubsurat banane ke liye aesthetic suffixes
    suffixes = [" aesthetic", " wallpaper", " nature", " cinematic"]
    search_query = f"{keyword}{random.choice(suffixes)}"
    
    url = f"https://api.pexels.com/v1/search?query={search_query}&orientation=portrait&per_page=1"
    headers = {"Authorization": PEXELS_API_KEY}

    for attempt in range(3):
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
                return None
            elif response.status_code == 429:
                time.sleep(10 * (attempt + 1))
            else:
                logging.warning(f"Pexels Error {response.status_code} for {keyword}")
        except Exception as e:
            logging.error(f"Pexels Attempt {attempt+1} failed: {e}")
            time.sleep(2)
    return None

def get_trends_for_country(geo_code):
    """Google Trends RSS fetcher with Advanced Browser Headers to avoid blocks"""
    url = f"https://trends.google.com/trends/trendingsearches/daily/rss?geo={geo_code}"
    
    # Real Browser Headers
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.5',
        'DNT': '1',
        'Connection': 'keep-alive',
        'Upgrade-Insecure-Requests': '1'
    }
    
    try:
        # Har request ke darmiyan random delay taake Google block na kare
        time.sleep(random.uniform(2, 5)) 
        response = requests.get(url, headers=headers, timeout=15)
        
        if response.status_code == 200:
            root = ET.fromstring(response.content)
            keywords = []
            for item in root.findall('.//item'):
                title_node = item.find('title')
                if title_node is not None and title_node.text:
                    keywords.append(title_node.text)
            
            return keywords[:12] # Zyada keywords uthayein taake variety mile
        else:
            logging.error(f"Google Trends Blocked (Status {response.status_code}) for {geo_code}")
    except Exception as e:
        logging.error(f"Trend Fetch Error for {geo_code}: {e}")
    return []

def update_global_database():
    if not initialize_firebase():
        return

    all_global_items = []

    for country in COUNTRIES:
        logging.info(f"--- Fetching Trends for: {country} ---")
        trends = get_trends_for_country(country)
        
        if not trends:
            continue

        country_media_list = []
        for kw in trends:
            if len(country_media_list) >= 8: # Har mulk ke 8 items
                break
            
            media_item = fetch_pexels_media(kw)
            if media_item:
                country_media_list.append(media_item)
                # Global list mein bhi add karein fallback ke liye
                if len(all_global_items) < 20:
                    all_global_items.append(media_item)
                logging.info(f"Success: {kw} ({country})")
                time.sleep(1.5)

        if country_media_list:
            try:
                db.reference(f'/trending_status/{country}').set(country_media_list)
                logging.info(f"Firebase Updated: {country}")
            except Exception as e:
                logging.error(f"Firebase Set Error: {e}")

    # GLOBAL Fallback Update
    if all_global_items:
        try:
            random.shuffle(all_global_items)
            db.reference('/trending_status/GLOBAL').set(all_global_items)
            logging.info("GLOBAL fallback node updated.")
        except Exception as e:
            logging.error(f"Global Update Error: {e}")

if __name__ == "__main__":
    update_global_database()
