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
    try:
        if not firebase_admin._apps:
            service_account_env = os.getenv("FIREBASE_SERVICE_ACCOUNT")
            if not service_account_env:
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
    """Google News se trending headlines nikalne ka stable tareeqa"""
    # Google News RSS is much more reliable than Trends RSS
    url = f"https://news.google.com/rss?{ceid_param}"
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36'
    }

    try:
        response = requests.get(url, headers=headers, timeout=20)
        if response.status_code == 200:
            root = ET.fromstring(response.content)
            keywords = []
            for item in root.findall('.//item')[:15]: # Pehli 15 headlines
                title = item.find('title').text
                if title:
                    # Headline se short keyword nikalna (Pexels ke liye)
                    clean_kw = title.split('-')[0].strip() # News source ka naam hatana
                    short_kw = " ".join(clean_kw.split()[:3]) # Pehle 3 words uthana
                    keywords.append(short_kw)
            
            logging.info(f"Fetched {len(keywords)} trends from News.")
            return list(set(keywords)) # Duplicates khatam karna
        else:
            logging.error(f"News Error {response.status_code}")
            return []
    except Exception as e:
        logging.error(f"News Fetch Error: {e}")
        return []

def fetch_pexels_media(keyword):
    if not PEXELS_API_KEY: return None
    
    # Keyword ko URL friendly banana
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
        logging.error(f"Pexels Error: {e}")
    return None

def main():
    if not initialize_firebase(): return

    global_data = []

    for code, ceid in NEWS_GEO_MAP.items():
        logging.info(f"--- Country: {code} ---")
        trends = get_trending_from_news(ceid)
        
        if not trends: continue

        country_list = []
        for kw in trends:
            if len(country_list) >= 6: break
            
            item = fetch_pexels_media(kw)
            if item:
                country_list.append(item)
                global_data.append(item)
                time.sleep(1)

        if country_list:
            db.reference(f'/trending_status/{code}').set(country_list)
            logging.info(f"Updated {code} in Firebase.")

    if global_data:
        random.shuffle(global_data)
        db.reference('/trending_status/GLOBAL').set(global_data[:25])
        logging.info("GLOBAL update done.")

if __name__ == "__main__":
    main()
