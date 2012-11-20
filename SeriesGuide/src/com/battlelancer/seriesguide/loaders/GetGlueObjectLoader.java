
package com.battlelancer.seriesguide.loaders;

import android.content.Context;

import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser;
import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.GetGlueObject;
import com.battlelancer.seriesguide.util.Utils;

import org.apache.http.client.ClientProtocolException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;

/**
 * Loads a list of TV shows using a search term against GetGlue's
 * glue/findObjects end point.
 * 
 * @see <a
 *      href="http://getglue.com/api#networkwide-methods">http://getglue.com/api#networkwide-methods</a>
 */
public class GetGlueObjectLoader extends GenericListLoader<GetGlueObject> {

    private static final String TAG = "GetGlueObjectLoader";
    private String mQuery;

    public GetGlueObjectLoader(String query, Context context) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<GetGlueObject> loadInBackground() {
        String url = GetGlue.GETGLUE_APIPATH_V2 + GetGlue.GETGLUE_FIND_OBJECTS + mQuery;

        // build request
        HttpURLConnection request = GetGlue.buildGetGlueRequest(url, getContext());
        if (request == null) {
            return null;
        }

        // execute request and parse response
        InputStream responseIn = null;
        try {
            request.connect();

            GetGlueXmlParser getGlueXmlParser = new GetGlueXmlParser();
            responseIn = request.getInputStream();

            int statuscode = request.getResponseCode();
            if (statuscode == HttpURLConnection.HTTP_OK) {
                List<GetGlueObject> tvShows = getGlueXmlParser.parseObjects(responseIn);
                return tvShows;
            } else {
                // TODO: hm, what are we going to do with this here?
                // GetGlueXmlParser.Error error =
                // getGlueXmlParser.parseError(responseIn);
            }
        } catch (ClientProtocolException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        } catch (IOException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        } catch (IllegalStateException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        } catch (XmlPullParserException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        } finally {
            if (responseIn != null) {
                try {
                    responseIn.close();
                } catch (IOException e) {
                    Utils.trackExceptionAndLog(getContext(), TAG, e);
                }
            }
        }

        return null;
    }

}
