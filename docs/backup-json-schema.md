# Backup JSON schema

_SeriesGuide can [export and import your data](http://seriesgui.de/help#backup) using JSON text files. Their schema is documented here._

The actual exported JSON is written without indents and comments to save space and increase performance. The following representation is for documentation purposes only.

When importing, not all but a few required values need to be present. SeriesGuide will in most cases fill in missing values on the next update.

## Shows

```json
[
    {
        "content_rating": "MA",
        "country": "us", // two letter ISO 3166-1 alpha-2 country code
        "favorite": true,
        "first_aired": "2018-02-02T08:00:00Z", // ISO 8601 datetime string
        "hidden": false,
        "imdb_id": "tt2261227",
        "language": "en",
        "last_watched_ms": 1614593199175,
        "network": "Netflix",
        "notify": true,
        "poster": "/95IsiH4p5937YXQHaOS2W2dWYOG.jpg", // TMDb poster path
        "rating": 9.5, // 0.0 to 10.0
        "rating_user": 10, // 0, 1 to 10
        "rating_votes": 100,
        "release_time": 300, // Encoded 24 hour local time (hhmm)
        "release_timezone": "America/New_York", // tz database name (Olson)
        "release_weekday": 4, // Local release week day (1-7, 0 if daily, -1 if unknown)
        "runtime": 50, // in minutes
        "seasons": [
            {
                "episodes": [
                    {
                        "collected": false,
                        "episode": 1,
                        "first_aired": 1517558400000, // ms
                        "imdb_id": "",
                        "plays": 1,
                        "skipped": false,
                        "title": "Out of the Past",
                        "tmdb_id": 1401623,
                        "watched": true
                    }
                ],
                "season": 1,
                "tmdb_id": "81447"
            }
        ],
        "status": "canceled", // see JsonExportTask.ShowStatusExport
        "title": "Altered Carbon",
        "tmdb_id": 68421, // required, or set linked tvdb_id
        "trakt_id": 122265,
        "tvdb_id": 332331 // if tmdb_id not set, used to look it up
    }
]
```

## Lists

```json
[
    {
        "items": [
            {
                "externalId": "62425", // TMDB ID
                "list_item_id": "62425-4-firstlist",
                "tvdb_id": 0, // unused
                "type": "tmdb-show"
            },
            // type episode, season and show are legacy items, 
            // are only displayed if externalId matches a TVDB ID in SeriesGuide library,
            // so does not work for new shows added to library
            {
                "externalId": "5443955",
                "list_item_id": "5443955-3-firstlist",
                "tvdb_id": 0,
                "type": "episode"
            },
            {
                "externalId": "620558",
                "list_item_id": "620558-2-firstlist",
                "tvdb_id": 0,
                "type": "season"
            },
            {
                "externalId": "253491",
                "list_item_id": "253491-1-firstlist",
                "tvdb_id": 0,
                "type": "show"
            }
        ],
        "list_id": "firstlist",
        "name": "First list",
        "order": 2
    }
]
```

## Movies

```json
[
    {
        "imdb_id": "tt1022603",
        "in_collection": false,
        "in_watchlist": false,
        "last_updated_ms": 1619620588554,
        "overview": "Some text.",
        "plays": 1,
        "poster": "/f9mbM0YMLpYemcWx6o2WeiYQLDP.jpg", // TMDB poster path
        "released_utc_ms": 1256162400000,
        "runtime_min": 95,
        "title": "(500) Days of Summer",
        "tmdb_id": 19913,
        "watched": true
    }
]
```
