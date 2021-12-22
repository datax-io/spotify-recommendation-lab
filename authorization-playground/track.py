from typing import Dict, Set, Any


class Track:
    def __init__(
        self,
        track: Dict[str, Any],
        tracks_in_albums: Set[str],
        tracks_in_own_playlists: Set[str],
        tracks_in_foreign_playlists: Set[str],
        recent_tracks: Set[str],
        saved_tracks: Set[str],
        top_tracks_short_term: Set[str],
        top_tracks_medium_term: Set[str],
        top_tracks_long_term: Set[str],
        albums_in_own_playlists: Set[str],
        albums_in_foreign_playlists: Set[str],
    ):
        self.id = track["id"]
        self.name = track["name"]
        self.in_recent_tracks = track["id"] in recent_tracks
        self.in_saved_tracks = track["id"] in saved_tracks
        self.in_top_tracks_short_term = track["id"] in top_tracks_short_term
        self.in_top_tracks_medium_term = track["id"] in top_tracks_medium_term
        self.in_top_tracks_long_term = track["id"] in top_tracks_long_term
        self.in_album = track["id"] in tracks_in_albums
        self.in_own_playlists = track["id"] in tracks_in_own_playlists
        self.in_foreign_playlists = track["id"] in tracks_in_foreign_playlists
        album = track.get("album", None)
        if album is not None:
            self.album = album["name"]
            self.album_in_own_playlists = album["id"] in albums_in_own_playlists
            self.album_in_foreign_playlists = album["id"] in albums_in_foreign_playlists
        else:
            self.album = ""
        features = track.get("track_features", None)
        if features is not None:
            self.acousticness = features["acousticness"]
            self.danceability = features["danceability"]
            self.duration_ms = features["duration_ms"]
            self.energy = features["energy"]
            self.instrumentalness = features["instrumentalness"]
            self.key = features["key"]
            self.liveness = features["liveness"]
            self.loudness = features["loudness"]
            self.mode = features["mode"]
            self.speechiness = features["speechiness"]
            self.tempo = features["tempo"]
            self.time_signature = features["time_signature"]
            self.valence = features["valence"]
        else:
            self.acousticness = ""
            self.danceability = ""
            self.duration_ms = ""
            self.energy = ""
            self.instrumentalness = ""
            self.key = ""
            self.liveness = ""
            self.loudness = ""
            self.mode = ""
            self.speechiness = ""
            self.tempo = ""
            self.time_signature = ""
            self.valence = ""

    def score(self):
        score = 0
        score += 1 if self.in_top_tracks_short_term else 0
        score += 1 if self.in_top_tracks_medium_term else 0
        score += 1 if self.in_top_tracks_long_term else 0
        score += 1 if self.in_saved_tracks and self.in_album else 0
        score += 1 if self.in_own_playlists else 0
        score -= (
            1
            if self.in_album
            and self.album_in_own_playlists
            and not self.in_own_playlists
            else 0
        )
        return score

    def get_dict(self):
        return {
            "id": self.id,
            "name": self.name,
            "album": self.album,
            "in_recent": self.in_recent_tracks,
            "in_saved": self.in_saved_tracks,
            "in_album": self.in_album,
            "in_short_term_top_tracks": self.in_top_tracks_short_term,
            "in_medium_term_top_tracks": self.in_top_tracks_medium_term,
            "in_long_term_top_tracks": self.in_top_tracks_long_term,
            "in_own_playlists": self.in_own_playlists,
            "in_foreign_playlists": self.in_foreign_playlists,
            "album_in_own_playlists": self.album_in_own_playlists,
            "album_in_foreign_playlists": self.album_in_foreign_playlists,
            "acousticness": self.acousticness,
            "danceability": self.danceability,
            "duration_ms": self.duration_ms,
            "energy": self.energy,
            "instrumentalness": self.instrumentalness,
            "key": self.key,
            "liveness": self.liveness,
            "loudness": self.loudness,
            "mode": self.mode,
            "speechiness": self.speechiness,
            "tempo": self.tempo,
            "time_signature": self.time_signature,
            "valence": self.valence,
            "score": self.score(),
        }
