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

# Google News mapping for stability (Country & Language code)
NEWS_GEO_MAP = {
    "US": "ceid=US:en",
    "IN": "ceid=IN:en",
    "PK": "ceid=PK:en",
    "GB": "ceid=GB:en",
    "BR": "ceid=BR:pt-419",
    "ID": "ceid=ID:id",
    "AE": "ceid=AE:en",
    "SA": "ceid=SA:ar"
}

def initialize_firebase():
    """Firebase initialization with service account from environment variables"""
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
                logging.error("FIREBASE_SERVICE_ACCOUNT not found in environment.")
                return False
            service_account_info = json.loads(service_account_env)
            cred = credentials.Certificate(service_account_info)
            firebase_admin.initialize_app(cred, {'databaseURL': FIREBASE_DB_URL})
            logging.info("Firebase Initialized Successfully.")
        return True
    except Exception as e:
        logging.error(f"Firebase Init Error: {e}")
        return False

def get_trending_from_news(ceid_param):
    """Fetches trending headlines from Google News RSS for specific region"""
    url = f"https://news.google.com/rss?{ceid_param}"
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    }

    try:
        response = requests.get(url, headers=headers, timeout=20)
        if response.status_code == 200:
            root = ET.fromstring(response.content)
            keywords = []
            for item in root.findall('.//item')[:15]: # Take top 15 news
                title = item.find('title').text
                if title:
                    # Clean the keyword to make it suitable for Pexels search
                    clean_kw = title.split('-')[0].strip() 
                    short_kw = " ".join(clean_kw.split()[:3]) 
                    keywords.append(short_kw)
            
            logging.info(f"Fetched {len(keywords)} trends from News.")
            return list(set(keywords))
        else:
            logging.error(f"News Error Status Code: {response.status_code}")
            return []
    except Exception as e:
        logging.error(f"News Fetch Error: {e}")
        return []

def fetch_pexels_media(keyword):
    """Fetches high-quality portrait images from Pexels based on keyword"""
    if not PEXELS_API_KEY: 
        logging.error("PEXELS_API_KEY is missing.")
        return None
    
    encoded_kw = urllib.parse.quote(keyword)
    # Adding 'aesthetic' to the query for better status-style images
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
        logging.error(f"Pexels Error for keyword '{keyword}': {e}")
    return None

def main():
    if not initialize_firebase(): return

    # STEP 1: PURANA DATA DELETE KAREIN (Clean Database)
    # Taka purane aur dead links remove ho jayain aur database fresh rahay
    try:
        db.reference('/trending_status').delete()
        logging.info("Database cleaned: Old trending data removed.")
    except Exception as e:
        logging.warn(f"Database clean warning: {e}")

    global_data = []

    # STEP 2: NAYA DATA FETCH KAREIN
    for code, ceid in NEWS_GEO_MAP.items():
        logging.info(f"--- Processing Country: {code} ---")
        trends = get_trending_from_news(ceid)
        
        if not trends: continue

        country_list = []
        for kw in trends:
            if len(country_list) >= 6: break # Max 6 items per country
            
            item = fetch_pexels_media(kw)
            if item:
                country_list.append(item)
                global_data.append(item)
                time.sleep(1.2) # Avoid Pexels rate limits

        if country_list:
            db.reference(f'/trending_status/{code}').set(country_list)
            logging.info(f"Updated node for {code} with {len(country_list)} items.")

    # STEP 3: GLOBAL NODE & METADATA UPDATE
    if global_data:
        # Shuffle for variety in global feed
        random.shuffle(global_data)
        db.reference('/trending_status/GLOBAL').set(global_data[:30]) # Top 30 for global
        
        # Metadata for tracking
        current_time = time.strftime("%Y-%m-%d %H:%M:%S", time.gmtime())
        db.reference('/trending_status/metadata').set({
            "last_updated": current_time,
            "refresh_interval": "4 Hours",
            "total_items": len(global_data)
        })
        
        logging.info(f"GLOBAL node updated successfully at {current_time}.")

if __name__ == "__main__":
    main()
