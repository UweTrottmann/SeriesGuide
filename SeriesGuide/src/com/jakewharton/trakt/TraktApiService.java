
package com.jakewharton.trakt;

import com.google.myjson.GsonBuilder;
import com.google.myjson.JsonDeserializationContext;
import com.google.myjson.JsonDeserializer;
import com.google.myjson.JsonElement;
import com.google.myjson.JsonParseException;
import com.google.myjson.JsonParser;
import com.google.myjson.JsonPrimitive;
import com.google.myjson.JsonSerializationContext;
import com.google.myjson.JsonSerializer;
import com.google.myjson.reflect.TypeToken;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.apibuilder.ApiService;
import com.jakewharton.trakt.entities.ActivityItem;
import com.jakewharton.trakt.entities.ActivityItemBase;
import com.jakewharton.trakt.entities.TvShowEpisode;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.enumerations.ActivityAction;
import com.jakewharton.trakt.enumerations.ActivityType;
import com.jakewharton.trakt.enumerations.DayOfTheWeek;
import com.jakewharton.trakt.enumerations.ExtendedParam;
import com.jakewharton.trakt.enumerations.Gender;
import com.jakewharton.trakt.enumerations.ListItemType;
import com.jakewharton.trakt.enumerations.ListPrivacy;
import com.jakewharton.trakt.enumerations.MediaType;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.enumerations.RatingType;
import com.jakewharton.trakt.util.Base64;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Trakt-specific API service extension which facilitates provides helper
 * methods for performing remote method calls as well as deserializing the
 * corresponding JSON responses.
 * 
 * @author Jake Wharton <jakewharton@gmail.com>
 */
public abstract class TraktApiService extends ApiService {
    /** Default connection timeout (in milliseconds). */
    private static final int DEFAULT_TIMEOUT_CONNECT = 60 * (int) TraktApiBuilder.MILLISECONDS_IN_SECOND;

    /** Default read timeout (in milliseconds). */
    private static final int DEFAULT_TIMEOUT_READ = 60 * (int) TraktApiBuilder.MILLISECONDS_IN_SECOND;

    /** HTTP header name for authorization. */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /** HTTP authorization type. */
    private static final String HEADER_AUTHORIZATION_TYPE = "Basic";

    /** Character set used for encoding and decoding transmitted values. */
    private static final Charset UTF_8_CHAR_SET = Charset.forName(ApiService.CONTENT_ENCODING);

    /** HTTP post method name. */
    private static final String HTTP_METHOD_POST = "POST";

    /** Format for decoding JSON dates in string format. */
    private static final SimpleDateFormat JSON_STRING_DATE = new SimpleDateFormat("yyy-MM-dd");

    /** Default plugin version debug string. */
    private static final String DEFAULT_PLUGIN_VERSION = Info.FULL_NAME;

    /** Default media center version debug string. */
    private static final String DEFAULT_MEDIA_CENTER_VERSION = Info.FULL_NAME;

    /** Default media center build date debug string. */
    private static final String DEFAULT_MEDIA_CENTER_DATE = Info.DATE;

    /** Default application name debug string. */
    private static final String DEFAULT_APP_DATE = Info.DATE;

    /** Default application version debug string. */
    private static final String DEFAULT_APP_VERSION = Info.FULL_NAME;

    /** Time zone for Trakt dates. */
    private static final TimeZone TRAKT_TIME_ZONE = TimeZone.getTimeZone("GMT-8:00");

    /** JSON parser for reading the content stream. */
    private final JsonParser parser;

    /** API key. */
    private String apiKey;

    /** Plugin version debug string. */
    private String pluginVersion;

    /** Media center version debug string. */
    private String mediaCenterVersion;

    /** Media center build date debug string. */
    private String mediaCenterDate;

    /** Application date debug string. */
    private String appDate;

    /** Application version debug string. */
    private String appVersion;

    /** Whether or not to use SSL API endpoint. */
    private boolean useSsl;

    /**
     * Create a new Trakt service with our proper default values.
     */
    public TraktApiService() {
        this.parser = new JsonParser();

        // Setup timeout defaults
        this.setConnectTimeout(DEFAULT_TIMEOUT_CONNECT);
        this.setReadTimeout(DEFAULT_TIMEOUT_READ);

        // Setup debug string defaults
        this.setPluginVersion(DEFAULT_PLUGIN_VERSION);
        this.setMediaCenterVersion(DEFAULT_MEDIA_CENTER_VERSION);
        this.setMediaCenterDate(DEFAULT_MEDIA_CENTER_DATE);
        this.setAppDate(DEFAULT_APP_DATE);
        this.setAppVersion(DEFAULT_APP_VERSION);
    }

    /**
     * Execute request using HTTP GET.
     * 
     * @param url URL to request.
     * @return JSON object.
     */
    public JsonElement get(String url) {
        return this.unmarshall(this.executeGet(url));
    }

    /**
     * Execute request using HTTP POST.
     * 
     * @param url URL to request.
     * @param postBody String to use as the POST body.
     * @return JSON object.
     */
    public JsonElement post(String url, String postBody) {
        return this.unmarshall(this.executeMethod(url, postBody, null, HTTP_METHOD_POST,
                HttpURLConnection.HTTP_OK));
    }

    /**
     * Set email and password to use for HTTP basic authentication.
     * 
     * @param username Username.
     * @param password_sha Password SHA1.
     */
    public void setAuthentication(String username, String password_sha) {
        if ((username == null) || (username.length() == 0)) {
            throw new IllegalArgumentException("Username must not be empty.");
        }
        if ((password_sha == null) || (password_sha.length() == 0)) {
            throw new IllegalArgumentException("Password SHA must not be empty.");
        }

        String source = username + ":" + password_sha;
        String authentication = HEADER_AUTHORIZATION_TYPE + " "
                + Base64.encodeBytes(source.getBytes());

        this.addRequestHeader(HEADER_AUTHORIZATION, authentication);
    }

    /**
     * Get the API key.
     * 
     * @return Value
     */
    /* package */String getApiKey() {
        return this.apiKey;
    }

    /**
     * Set API key to use for client authentication by Trakt.
     * 
     * @param value Value.
     */
    public void setApiKey(String value) {
        this.apiKey = value;
    }

    /**
     * Get the plugin version debug string used for scrobbling.
     * 
     * @return Value.
     */
    /* package */String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Set the plugin version debug string used for scrobbling.
     * 
     * @param pluginVersion Value.
     */
    public void setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
    }

    /**
     * Get the media center version debug string used for scrobbling.
     * 
     * @return Value.
     */
    /* package */String getMediaCenterVersion() {
        return mediaCenterVersion;
    }

    /**
     * Set the media center version debug string used for scrobbling.
     * 
     * @param mediaCenterVersion Value.
     */
    public void setMediaCenterVersion(String mediaCenterVersion) {
        this.mediaCenterVersion = mediaCenterVersion;
    }

    /**
     * Get the media center build date debug string used for scrobbling.
     * 
     * @return Value.
     */
    /* package */String getMediaCenterDate() {
        return mediaCenterDate;
    }

    /**
     * Set the media center build date debug string used for scrobbling.
     * 
     * @param mediaCenterDate Value.
     */
    public void setMediaCenterDate(String mediaCenterDate) {
        this.mediaCenterDate = mediaCenterDate;
    }

    /**
     * Get the application date debug string used for checking in.
     * 
     * @return Value.
     */
    /* package */String getAppDate() {
        return appDate;
    }

    /**
     * Set the application date debug string used for checking in.
     * 
     * @param appDate Value.
     */
    public void setAppDate(String appDate) {
        this.appDate = appDate;
    }

    /**
     * Get the application version debug string used for checking in.
     * 
     * @return Value.
     */
    /* package */String getAppVersion() {
        return appVersion;
    }

    /**
     * Set the application version debug string used for checking in.
     * 
     * @param appVersion Value.
     */
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Get whether or not we want to use the SSL API endpoint.
     * 
     * @return Value.
     */
    /* package */boolean getUseSsl() {
        return useSsl;
    }

    /**
     * Set whether or not to use the SSL API endpoint.
     * 
     * @param useSsl Value.
     */
    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    /**
     * Use GSON to deserialize a JSON object to a native class representation.
     * 
     * @param <T> Native class type.
     * @param typeToken Native class type wrapper.
     * @param response Serialized JSON object.
     * @return Deserialized native instance.
     */
    @SuppressWarnings("unchecked")
    protected <T> T unmarshall(TypeToken<T> typeToken, JsonElement response) {
        return (T) TraktApiService.getGsonBuilder().create()
                .fromJson(response, typeToken.getType());
    }

    /**
     * Use GSON to deserialize a JSON string to a native class representation.
     * 
     * @param <T> Native class type.
     * @param typeToken Native class type wrapper.
     * @param reponse Serialized JSON string.
     * @return Deserialized native instance.
     */
    @SuppressWarnings("unchecked")
    protected <T> T unmarshall(TypeToken<T> typeToken, String reponse) {
        return (T) TraktApiService.getGsonBuilder().create().fromJson(reponse, typeToken.getType());
    }

    /**
     * Read the entirety of an input stream and parse to a JSON object.
     * 
     * @param jsonContent JSON content input stream.
     * @return Parsed JSON object.
     */
    protected JsonElement unmarshall(InputStream jsonContent) {
        try {
            JsonElement element = this.parser.parse(new InputStreamReader(jsonContent,
                    UTF_8_CHAR_SET));
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            } else if (element.isJsonArray()) {
                return element.getAsJsonArray();
            } else {
                throw new ApiException("Unknown content found in response." + element);
            }
        } catch (Exception e) {
            throw new ApiException(e);
        } finally {
            ApiService.closeStream(jsonContent);
        }
    }

    /**
     * Create a {@link GsonBuilder} and register all of the custom types needed
     * in order to properly deserialize complex Trakt-specific type.
     * 
     * @return Assembled GSON builder instance.
     */
    public static GsonBuilder getGsonBuilder() {
        GsonBuilder builder = new GsonBuilder();

        // class types
        builder.registerTypeAdapter(Integer.class, new JsonDeserializer<Integer>() {
            @Override
            public Integer deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                try {
                    return Integer.valueOf(json.getAsInt());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                try {
                    long value = json.getAsLong();
                    Calendar date = Calendar.getInstance(TRAKT_TIME_ZONE);
                    date.setTimeInMillis(value * TraktApiBuilder.MILLISECONDS_IN_SECOND);
                    return date.getTime();
                } catch (NumberFormatException outer) {
                    try {
                        return JSON_STRING_DATE.parse(json.getAsString());
                    } catch (ParseException inner) {
                        throw new JsonParseException(outer);
                    }
                }
            }
        });
        builder.registerTypeAdapter(Calendar.class, new JsonDeserializer<Calendar>() {
            @Override
            public Calendar deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                Calendar value = Calendar.getInstance(TRAKT_TIME_ZONE);
                value.setTimeInMillis(json.getAsLong() * TraktApiBuilder.MILLISECONDS_IN_SECOND);
                return value;
            }
        });
        builder.registerTypeAdapter(TvShowSeason.Episodes.class,
                new JsonDeserializer<TvShowSeason.Episodes>() {
                    @Override
                    public TvShowSeason.Episodes deserialize(JsonElement json, Type typeOfT,
                            JsonDeserializationContext context) throws JsonParseException {
                        TvShowSeason.Episodes episodes = new TvShowSeason.Episodes();
                        try {
                            if (json.isJsonArray()) {
                                if (json.getAsJsonArray().get(0).isJsonPrimitive()) {
                                    // Episode number list
                                    Field fieldNumbers = TvShowSeason.Episodes.class
                                            .getDeclaredField("numbers");
                                    fieldNumbers.setAccessible(true);
                                    fieldNumbers.set(episodes, context.deserialize(json,
                                            (new TypeToken<List<Integer>>() {
                                            }).getType()));
                                } else {
                                    // Episode object list
                                    Field fieldList = TvShowSeason.Episodes.class
                                            .getDeclaredField("episodes");
                                    fieldList.setAccessible(true);
                                    fieldList.set(episodes, context.deserialize(json,
                                            (new TypeToken<List<TvShowEpisode>>() {
                                            }).getType()));
                                }
                            } else {
                                // Episode count
                                Field fieldCount = TvShowSeason.Episodes.class
                                        .getDeclaredField("count");
                                fieldCount.setAccessible(true);
                                fieldCount.set(episodes, Integer.valueOf(json.getAsInt()));
                            }
                        } catch (SecurityException e) {
                            throw new JsonParseException(e);
                        } catch (NoSuchFieldException e) {
                            throw new JsonParseException(e);
                        } catch (IllegalArgumentException e) {
                            throw new JsonParseException(e);
                        } catch (IllegalAccessException e) {
                            throw new JsonParseException(e);
                        }
                        return episodes;
                    }
                });
        builder.registerTypeAdapter(ActivityItemBase.class,
                new JsonDeserializer<ActivityItemBase>() {
                    // XXX See:
                    // https://groups.google.com/d/topic/traktapi/GQlT9HfAEjw/discussion
                    @Override
                    public ActivityItemBase deserialize(JsonElement json, Type typeOfT,
                            JsonDeserializationContext context) throws JsonParseException {
                        if (json.isJsonArray()) {
                            if (json.getAsJsonArray().size() != 0) {
                                throw new JsonParseException(
                                        "\"watched\" field returned a non-empty array.");
                            }
                            return null;
                        } else {
                            return context.deserialize(json, ActivityItem.class);
                        }
                    }
                });
        // enum types
        builder.registerTypeAdapter(ActivityAction.class, new JsonDeserializer<ActivityAction>() {
            @Override
            public ActivityAction deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                return ActivityAction.fromValue(json.getAsString());
            }
        });
        builder.registerTypeAdapter(ActivityType.class, new JsonDeserializer<ActivityType>() {
            @Override
            public ActivityType deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                return ActivityType.fromValue(json.getAsString());
            }
        });
        builder.registerTypeAdapter(DayOfTheWeek.class, new JsonDeserializer<DayOfTheWeek>() {
            @Override
            public DayOfTheWeek deserialize(JsonElement arg0, Type arg1,
                    JsonDeserializationContext arg2) throws JsonParseException {
                return DayOfTheWeek.fromValue(arg0.getAsString());
            }
        });
        builder.registerTypeAdapter(ExtendedParam.class, new JsonDeserializer<ExtendedParam>() {
            @Override
            public ExtendedParam deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                return ExtendedParam.fromValue(json.getAsString());
            }
        });
        builder.registerTypeAdapter(Gender.class, new JsonDeserializer<Gender>() {
            @Override
            public Gender deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
                    throws JsonParseException {
                return Gender.fromValue(arg0.getAsString());
            }
        });
        builder.registerTypeAdapter(ListItemType.class, new JsonDeserializer<ListItemType>() {
            @Override
            public ListItemType deserialize(JsonElement arg0, Type arg1,
                    JsonDeserializationContext arg2) throws JsonParseException {
                return ListItemType.fromValue(arg0.getAsString());
            }
        });
        builder.registerTypeAdapter(ListPrivacy.class, new JsonDeserializer<ListPrivacy>() {
            @Override
            public ListPrivacy deserialize(JsonElement arg0, Type arg1,
                    JsonDeserializationContext arg2) throws JsonParseException {
                return ListPrivacy.fromValue(arg0.getAsString());
            }
        });
        builder.registerTypeAdapter(MediaType.class, new JsonDeserializer<MediaType>() {
            @Override
            public MediaType deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                return MediaType.fromValue(json.getAsString());
            }
        });
        builder.registerTypeAdapter(Rating.class, new JsonDeserializer<Rating>() {
            @Override
            public Rating deserialize(JsonElement json, Type typeOfT,
                    JsonDeserializationContext context) throws JsonParseException {
                return Rating.fromValue(json.getAsString());
            }
        });
        builder.registerTypeAdapter(Rating.class, new JsonSerializer<Rating>() {
            @Override
            public JsonElement serialize(Rating src, Type typeOfSrc,
                    JsonSerializationContext context) {
                return new JsonPrimitive(src.toString());
            }
        });
        builder.registerTypeAdapter(RatingType.class, new JsonDeserializer<RatingType>() {
            @Override
            public RatingType deserialize(JsonElement arg0, Type arg1,
                    JsonDeserializationContext arg2) throws JsonParseException {
                return RatingType.fromValue(arg0.getAsString());
            }
        });

        return builder;
    }
}
