# %%

import csv
import os
import sys
import webbrowser as browser
from typing import Any, Dict, Set, List, Optional
from urllib.parse import urlencode, urlparse

import requests

# from local import access_token
from track import Track

# All tracks
tracks: Dict[str, Any] = {}

page_limit = 49

base_url = "https://api.spotify.com/v1"

client_id = "d7fe81d64c81412082b9c57e5beaab4e"
access_token = os.environ.get("ACCESS_TOKEN")
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
        "response_type": "token",
        "redirect_uri": "http://localhost:8888/callback",
        "client_id": client_id,
        "show_dialog": "true",
        "scope": ",".join(
            [
                "user-read-recently-played",
                "user-library-read",
                "playlist-read-private",
                "user-top-read",
            ]
        ),
    }
    url = f"https://accounts.spotify.com/authorize?{urlencode(params)}"
    browser.open(url)
    print("Please first authorize")
    sys.exit(1)

# %% Get recent tracks

recent_tracks: Set[str] = set()


def get_recent_tracks(offset: int = 0):
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-users-saved-tracks
    print(f"Getting recent tracks (offset={offset})")

    recent_items = requests.get(
        f"{base_url}/me/player/recently-played",
        params={"limit": page_limit},
        headers=headers,
    ).json()["items"]

    for track in [item["track"] for item in recent_items if "track" in item]:
        tracks[track["id"]] = track
        recent_tracks.add(track["id"])

    if len(recent_tracks) == page_limit:
        get_recent_tracks(offset=offset + page_limit)


get_recent_tracks()


# %% Get top tracks


def get_top_tracks(time_range: str, offset: int = 0):
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-users-top-artists-and-tracks
    print(f"Getting top tracks (time_range={time_range}, offset={offset})")

    top_tracks: Set[str] = set()

    track_items = requests.get(
        f"{base_url}/me/top/tracks",
        params={"limit": page_limit, "offset": offset, "time_range": time_range},
        headers=headers,
    ).json()["items"]

    for track in track_items:
        tracks[track["id"]] = track
        top_tracks.add(track["id"])

    if len(track_items) == page_limit:
        top_tracks.update(
            get_top_tracks(time_range=time_range, offset=offset + page_limit)
        )
    return top_tracks


top_tracks_short_term: Set[str] = get_top_tracks(time_range="short_term")
top_tracks_medium_term: Set[str] = get_top_tracks(time_range="medium_term")
top_tracks_long_term: Set[str] = get_top_tracks(time_range="long_term")

# %% Get saved tracks

saved_tracks: Set[str] = set()


def get_saved_tracks(offset: int = 0):
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-users-saved-tracks
    print(f"Getting saved tracks (offset={offset})")

    track_items = requests.get(
        f"{base_url}/me/tracks",
        params={"limit": page_limit, "offset": offset},
        headers=headers,
    ).json()["items"]

    for track in [item["track"] for item in track_items]:
        tracks[track["id"]] = track
        saved_tracks.add(track["id"])

    if len(track_items) == page_limit:
        get_saved_tracks(offset=offset + page_limit)


get_saved_tracks()

# %% Get saved albums

albums: Dict[str, Any] = {}
tracks_in_albums: Set[str] = set()

albums_in_own_playlists: Set[str] = set()
albums_in_foreign_playlists: Set[str] = set()


def get_albums(offset: int = 0):
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-users-saved-albums
    print(f"Getting saved albums (offset={offset})")

    album_items = requests.get(
        f"{base_url}/me/albums",
        params={"limit": page_limit, "offset": offset},
        headers=headers,
    ).json()["items"]

    for album in [item["album"] for item in album_items]:
        albums[album["id"]] = album
        for track in album["tracks"]["items"]:
            track["album"] = album
            tracks[track["id"]] = track
            tracks_in_albums.add(track["id"])

    if len(album_items) == page_limit:
        get_albums(offset=offset + page_limit)


get_albums()

# %% Get saved playlists


playlists: Dict[str, Any] = {}
tracks_in_own_playlists: Set[str] = set()
tracks_in_foreign_playlists: Set[str] = set()


def get_playlist_tracks(playlist, playlist_is_own: bool, offset: int = 0) -> List:
    print(f"Getting tracks for playlist {playlist['id']} (offset={offset})")
    track_items = requests.get(
        f"{base_url}/playlists/{playlist['id']}/tracks",
        params={"limit": page_limit, "offset": offset},
        headers=headers,
    ).json()["items"]

    track_objs = [track_item["track"] for track_item in track_items]
    try:
        for track_obj in [
            track_obj for track_obj in track_objs if track_obj is not None
        ]:
            tracks[track_obj["id"]] = track_obj
            if playlist_is_own:
                tracks_in_own_playlists.add(track_obj["id"])
            else:
                tracks_in_foreign_playlists.add(track_obj["id"])

            album = track_obj.get("album", {}).get("id", None)
            if album is not None:
                if playlist_is_own:
                    albums_in_own_playlists.add(album)
                else:
                    albums_in_foreign_playlists.add(album)
    except Exception:
        print("track_obj is None")
        return track_objs

    if len(track_objs) == page_limit:
        track_objs.extend(
            get_playlist_tracks(playlist, playlist_is_own, offset=offset + page_limit)
        )
    return track_objs


def get_playlists(offset: int = 0):
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-a-list-of-current-users-playlists
    print(f"Getting created / saved playlists (offset={offset})")

    playlist_items = requests.get(
        f"{base_url}/me/playlists",
        params={"limit": page_limit, "offset": offset},
        headers=headers,
    ).json()["items"]

    for playlist in playlist_items:
        playlist_is_own = playlist["owner"]["id"] == current_user_id
        track_objs = get_playlist_tracks(playlist, playlist_is_own)
        playlist["tracks"] = track_objs
        playlists[playlist["id"]] = playlist

    if len(playlist_items) == page_limit:
        get_playlists(offset=offset + page_limit)


get_playlists(0)

# %% Get track's audio features

track_features: Dict[str, Any] = {}


def get_track_features():
    # https://developer.spotify.com/documentation/web-api/reference/#/operations/get-several-audio-features
    track_ids = list(tracks.keys())
    offset = 0
    while track_ids[offset : offset + 100]:
        print(
            f"Getting track features for tracks from {offset} to {offset + 100} out of {len(tracks)}"
        )
        features = requests.get(
            f"{base_url}/audio-features",
            params={"ids": ",".join(map(str, track_ids[offset : offset + 100]))},
            headers=headers,
        ).json()["audio_features"]
        for feature in features:
            if feature is not None:
                tracks[feature["id"]]["track_features"] = feature

        offset = offset + 100


get_track_features()

# %% Output results

print("====")
print("playlists: ", len(playlists))
print("tracks_in_own_playlists: ", len(tracks_in_own_playlists))
print("tracks_in_foreign_playlists: ", len(tracks_in_foreign_playlists))

print("====")
print("albums: ", len(albums))
print("tracks_in_albums: ", len(tracks_in_albums))

print("====")
print("recent_tracks: ", len(recent_tracks))
print("saved_tracks: ", len(saved_tracks))
print("top_tracks_short_term: ", len(top_tracks_short_term))
print("top_tracks_medium_term: ", len(top_tracks_medium_term))
print("top_tracks_long_term: ", len(top_tracks_long_term))

print("====")
data: List[Dict[str, Any]] = [
    Track(
        track,
        tracks_in_albums=tracks_in_albums,
        tracks_in_own_playlists=tracks_in_own_playlists,
        tracks_in_foreign_playlists=tracks_in_foreign_playlists,
        recent_tracks=recent_tracks,
        saved_tracks=saved_tracks,
        top_tracks_short_term=top_tracks_short_term,
        top_tracks_medium_term=top_tracks_medium_term,
        top_tracks_long_term=top_tracks_long_term,
        albums_in_own_playlists=albums_in_own_playlists,
        albums_in_foreign_playlists=albums_in_foreign_playlists,
    ).get_dict()
    for track in tracks.values()
]
data.sort(key=lambda x: x["score"], reverse=True)
with open("output.csv", mode="w") as output_file:
    writer = csv.DictWriter(output_file, fieldnames=data[0].keys())
    writer.writeheader()
    writer.writerows(data)

print("output saved")
