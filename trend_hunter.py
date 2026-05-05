import requests
import xml.etree.ElementTree as ET
import firebase_admin
from firebase_admin import credentials, db
import os
import json
import time
import logging

# Logging setup taake debug karne mein asani ho
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# CONFIGURATION
PEXELS_API_KEY = os.getenv("PEXELS_API_KEY")
FIREBASE_DB_URL = "https://status-saver-92d48-default-rtdb.firebaseio.com"
# Jin countries ka data humein chahiye
COUNTRIES = ["US", "IN", "PK", "GB", "BR", "ID", "AE", "SA"]

def initialize_firebase():
    """Firebase safety check ke sath initialize karta hai"""
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
    """Pexels API se high resolution image uthata hai with Retries"""
    if not PEXELS_API_KEY:
        logging.error("Pexels API Key is missing!")
        return None

    # Status ke liye query ko aesthetic banate hain
    search_query = f"{keyword} aesthetic"
    url = f"https://api.pexels.com/v1/search?query={search_query}&orientation=portrait&per_page=1"
    headers = {"Authorization": PEXELS_API_KEY}

    for attempt in range(3): # 3 dafa koshish karega agar fail hua
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
            elif response.status_code == 429: # Rate limit handling
                time.sleep(5 * (attempt + 1))
            else:
                logging.warning(f"Pexels API Error {response.status_code} for {keyword}")
        except Exception as e:
            logging.error(f"Attempt {attempt+1} failed for {keyword}: {e}")
            time.sleep(2)
    return None

def get_trends_for_country(geo_code):
    """Google Trends RSS se top keywords nikalta hai"""
    url = f"https://trends.google.com/trends/trendingsearches/daily/rss?geo={geo_code}"
    headers = {'User-Agent': 'Mozilla/5.0'}
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code == 200:
            root = ET.fromstring(response.content)
            keywords = []
            for item in root.findall('.//item'):
                title = item.find('title').text
                if title:
                    keywords.append(title)
            return keywords[:10] # Top 10 trends
    except Exception as e:
        logging.error(f"Trend Fetch Error for {geo_code}: {e}")
    return []

def update_global_database():
    """Main function: Har country ka data alag node mein save karega"""
    if not initialize_firebase():
        return

    for country in COUNTRIES:
        logging.info(f"--- Processing Country: {country} ---")
        trends = get_trends_for_country(country)
        
        if not trends:
            logging.warning(f"No trends found for {country}, skipping.")
            continue

        country_media_list = []
        for kw in trends:
            # Agar 6 items mil jayein toh agle mulk par chalo
            if len(country_media_list) >= 6:
                break
            
            media_item = fetch_pexels_media(kw)
            if media_item:
                country_media_list.append(media_item)
                logging.info(f"Added: {kw} for {country}")
                time.sleep(1) # API rate limit protection

        if country_media_list:
            try:
                # Node structure: trending_status/PK, trending_status/US etc.
                ref = db.reference(f'/trending_status/{country}')
                ref.set(country_media_list)
                logging.info(f"Successfully updated {country} node in Firebase.")
            except Exception as e:
                logging.error(f"Firebase Update Error for {country}: {e}")

    # Ek 'Global' node bhi bana dete hain fallback ke liye
    try:
        global_ref = db.reference('/trending_status/GLOBAL')
        # US ka data hi as a global set kar dete hain
        us_data = db.reference('/trending_status/US').get()
        if us_data:
            global_ref.set(us_data)
    except:
        pass

if __name__ == "__main__":
    update_global_database()
