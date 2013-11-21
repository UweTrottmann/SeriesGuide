package com.uwetrottmann.seriesguide;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.appengine.api.oauth.OAuthRequestException;
import com.google.appengine.api.users.User;

import java.util.ArrayList;

import javax.inject.Named;

@Api(
        name = "shows",
        version = "1",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID}
)
public class Shows {

    public static ArrayList<Show> shows = new ArrayList<Show>();

    static {
        shows.add(new Show(123, "anonymous@xyz.com"));
        shows.add(new Show(456, "anonymous@xyz.com"));
    }

    public Show getShow(@Named("id") Integer id) {
        return shows.get(id);
    }

    @ApiMethod(name = "shows.multiply", httpMethod = "post")
    public Show multiplyShow(@Named("times") Integer times, Show show) {
        Show response = new Show();
        response.setTvdbId(show.getTvdbId() * times);
        return response;
    }

    @ApiMethod(name = "shows.authed", path = "show/authed")
    public Show authedShow(User user) throws OAuthRequestException {
        if (user == null) {
            throw new OAuthRequestException("Not authorized. Please make sure you are logged in.");
        }
        Show response = new Show(123, user.getEmail());
        return response;
    }

}
