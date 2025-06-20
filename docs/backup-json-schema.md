# Export and import data: Expected JSON format

_SeriesGuide can [export and import your data](https://www.seriesgui.de/help/how-to/backup/backup)
using JSON text files._

The expected format and values are documented here.

To see an example file, export your data from SeriesGuide. Note that unlike the examples below the
exported JSON is written without indents to save space and increase performance. Use a text editor
or other tool to format it for easier reading.

When importing, not all but a few required values need to be present. In most cases, SeriesGuide 
will fill in the missing values when data is next updated.

## Shows

The JSON is structured like this (see the classes, they document each property):

- array of [Show](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/Show.java)
  - contains a `seasons` array of [Season](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/Season.java)
    - contains an `episodes` array of [Episode](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/Episode.java)

The `language` value should be one of the TMDB language codes SeriesGuide supports, like `en-US` 
(see `content_languages` in [donottranslate.xml](/app/src/main/res/values/donottranslate.xml)). Or a 
two-letter language code (like `en`) that can be mapped to one of the supported codes.

[shows-export-example.json](shows-export-example.json)

```json
[
    {
        "tmdb_id": 68421,
        "tvdb_id": 332331,
        "imdb_id": "tt2261227",
        "trakt_id": 122265,

        "title": "Altered Carbon",
        "overview": "A description of this show.",

        "language": "en-US",

        "first_aired": "2018-02-02T08:00:00Z",
        "release_time": 2000,
        "release_weekday": 4,
        "release_timezone": "America/New_York",
        "country": "us",
        
        "custom_release_time": 400,
        "custom_release_day_offset": 0,
        "custom_release_timezone": "America/New_York",

        "poster": "/95IsiH4p5937YXQHaOS2W2dWYOG.jpg",
        "content_rating": "MA",
        "status": "canceled",
        "runtime": 50,
        "genres": "Sci-Fi \u0026 Fantasy|Drama",
        "network": "Netflix",

        "rating_tmdb": 7.395,
        "rating_tmdb_votes": 1095,
        "rating": 7.774462758189026,
        "rating_votes": 4793,
        "rating_user": 9,
      
        "favorite": false,
        "notify": true,
        "hidden": false,

        "last_watched_ms": 1614593199175,

        "user_note": "Note text",
        "user_note_trakt_id": 123,

        "seasons": [
            {
                "season": 1,
                "tmdb_id": "61343",
                "tvdb_id": 1234,
                "episodes": [
                    {
                        "tmdb_id": 991306,
                        "tvdb_id": 1401623,
                        "episode": 1,
                        "title": "Out of the Past",
                        "first_aired": 1517558400000,
                        
                        "watched": true,
                        "plays": 1,
                        "skipped": false,
                        "collected": false,
                        
                        "episode_dvd": 1.0,
                        "overview": "An episode description",
                        "image": "/2Kp2SNMcJBExFBaThMXbWzr4JTn.jpg",
                        "writers": "Scott Peters|Kenneth Johnson",
                        "gueststars": "Alan Tudyk|Stefan Arngrim",
                        "directors": "Terrence O\u0027Hara|Alan Tudyk",
                        
                        "rating_tmdb": 8.1,
                        "rating_tmdb_votes": 11,
                        
                        "rating": 5.88608,
                        "rating_votes": 79,
                        "rating_user": 8
                    }
                ]
            }
        ]

    }
]
```

When importing from another app or data source a minimal amount of values can be enough, here is an
example:

[shows-import-minimal.json](shows-import-minimal.json)

```json
[
    {
        "tmdb_id": 68421,
        "language": "de-DE",
        "seasons": [
            {
                "tmdb_id": "81447",
                "episodes": [
                    {
                        "tmdb_id": 1401623,
                        "watched": true
                    }
                ]
            }
        ]
    }
]
```

While the `language` is technically not required, it's easier to set it in the JSON than changing it
for each show after importing.

## Lists

The JSON is structured like this (see the classes, they document each property):

- array of [List](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/List.java)
  - contains an `items` array of [ListItem](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/ListItem.java)

Note: `type` values of `episode`, `season` and `show` are legacy values and are only displayed if
`externalId` matches a TVDB ID in the SeriesGuide library. So this does not work for new shows added
to the library.

```json
[
    {
        "items": [
            {
                "externalId": "62425",
                "list_item_id": "62425-4-firstlist",
                "tvdb_id": 0,
                "type": "tmdb-show"
            },
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
        "list_id": "First%20list",
        "name": "First list",
        "order": 0
    }
]
```

## Movies

The JSON is an array of [Movie](/app/src/main/java/com/battlelancer/seriesguide/dataliberation/model/Movie.java)
(see the class, it documents each property).

```json
[
    {
        "tmdb_id": 19913,
        "imdb_id": "tt1022603",
        
        "title": "(500) Days of Summer",
        "released_utc_ms": 1256162400000,
        "runtime_min": 95,
        "poster": "/f9mbM0YMLpYemcWx6o2WeiYQLDP.jpg",
        "overview": "A description of this movie.",
        
        "in_collection": false,
        "in_watchlist": false,
        "watched": true,
        "plays": 1,
        
        "last_updated_ms": 1619620588554
    }
]
```
