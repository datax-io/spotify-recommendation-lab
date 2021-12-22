# %%

import os
import sys
import webbrowser as browser
from typing import Optional
from urllib.parse import urlencode, urlparse

import requests

# from local import access_token

base_url = "https://api.spotify.com/v1"

app_id = "AXh6jUbAiBXCntF4YxyDNcW"
app_link = "https://steward.oasislabs.com/apps/AXh6jUbAiBXCntF4YxyDNcW/join"
client_id = "CB1QZPV8cVrGsw5YCtxyyPs"

access_token = os.environ.get("PARCEL_TOKEN")
headers = {}


def get_user_id() -> Optional[str]:
    global access_token, headers
    try:
        headers = {"Authorization": f"Bearer {access_token}"}
        return requests.get(f"{base_url}/me", headers=headers).json()["id"]
    except:
        access_token = None
        headers = {}
        return None


current_user_id = get_user_id()

# %% Paste the url in browser address bar here and run

if access_token is None:
    redirected_uri = """
http://localhost:8888/callback#access_token=BQAoEJlYyPwAjv3EeUVvaKsEWQtcwRz-DqQt5aSpJ9cSGlhZE8pv_byD5UVpOcLDD6473nGJ9WyNIthyqp10xbi6wljerVh_cmdZ6tiCt_YZ8A_8DLs5-pgRE1srDpKB-uorrHLHZ2LGWzCtJNWhxpA5cGX9g8Labs8rgl0BTbTIjow&token_type=Bearer&expires_in=3600
    """
    access_token = urlparse(redirected_uri).fragment.split("&")[0].split("=")[-1]
    current_user_id = get_user_id()

# %% Run to get access token from browser

if access_token is None:
    params = {
        "response_type": "code",
        "redirect_uri": "http://localhost:8888",
        "client_id": client_id,
        "scope": " ".join(["openid", "profile", "email", "parcel.safe"]),
        "audience": "https://api.oasislabs.com/parcel",
    }
    url = f"https://auth.oasislabs.com/oauth/token?{urlencode(params)}"
    print(url)
    browser.open(url)
    print("Please first authorize")
    sys.exit(1)
