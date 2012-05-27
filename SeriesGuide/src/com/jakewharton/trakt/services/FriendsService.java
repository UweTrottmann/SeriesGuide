package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.entities.UserProfile;

import java.util.List;

public class FriendsService extends TraktApiService {
    /**
     * Add a new friend. This will put the request in pending status until the
     * potential friend accepts.
     *
     * @param friend Username of the friend to add.
     * @return Builder instance.
     */
    public AddBuilder add(String friend) {
        return (new AddBuilder(this)).friend(friend);
    }

    /**
     * Get a list of all friends including the timestamp when they were
     * approved.
     *
     * @return Builder instance.
     */
    public AllBuilder all() {
        return new AllBuilder(this);
    }

    /**
     * Approve a friend request.
     *
     * @param friend Username of the friend to approve.
     * @return Builder instance.
     */
    public ApproveBuilder approve(String friend) {
        return (new ApproveBuilder(this)).friend(friend);
    }

    /**
     * Delete a friend.
     *
     * @param friend Username of the friend to delete.
     * @return Builder instance.
     */
    public DeleteBuilder delete(String friend) {
        return (new DeleteBuilder(this)).friend(friend);
    }

    /**
     * Deny a friend request.
     *
     * @param friend Username of the friend to deny.
     * @return Builder instance.
     */
    public DenyBuilder deny(String friend) {
        return (new DenyBuilder(this)).friend(friend);
    }

    /**
     * Get a list of all friends requests including the timestamp when the
     * friend request was made. Use the approve and deny methods to manage each
     * request.
     *
     * @return Builder instance.
     */
    public RequestsBuilder requests() {
        return new RequestsBuilder(this);
    }


    public static final class AddBuilder extends TraktApiBuilder<Response> {
        private static final String POST_FRIEND = "friend";

        private static final String URI = "/friends/add/" + FIELD_API_KEY;

        private AddBuilder(FriendsService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Username of the friend to add.
         *
         * @param friend Value.
         * @return Builder instance.
         */
        public AddBuilder friend(String friend) {
            this.postParameter(POST_FRIEND, friend);
            return this;
        }
    }
    public static final class AllBuilder extends TraktApiBuilder<List<UserProfile>> {
        private static final String URI = "/friends/all/" + FIELD_API_KEY;

        private AllBuilder(FriendsService service) {
            super(service, new TypeToken<List<UserProfile>>() {}, URI, HttpMethod.Post);
        }
    }
    public static final class ApproveBuilder extends TraktApiBuilder<Response> {
        private static final String POST_FRIEND = "friend";

        private static final String URI = "/friends/approve/" + FIELD_API_KEY;

        private ApproveBuilder(FriendsService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Username of the friend to approve.
         *
         * @param friend Value.
         * @return Builder instance.
         */
        public ApproveBuilder friend(String friend) {
            this.postParameter(POST_FRIEND, friend);
            return this;
        }
    }
    public static final class DeleteBuilder extends TraktApiBuilder<Response> {
        private static final String POST_FRIEND = "friend";

        private static final String URI = "/friends/delete/" + FIELD_API_KEY;

        private DeleteBuilder(FriendsService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Username of the friend to delete.
         *
         * @param friend Value.
         * @return Builder instance.
         */
        public DeleteBuilder friend(String friend) {
            this.postParameter(POST_FRIEND, friend);
            return this;
        }
    }
    public static final class DenyBuilder extends TraktApiBuilder<Response> {
        private static final String POST_FRIEND = "friend";

        private static final String URI = "/friends/deny/" + FIELD_API_KEY;

        private DenyBuilder(FriendsService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Username of the friend to delete.
         *
         * @param friend Value.
         * @return Builder instance.
         */
        public DenyBuilder friend(String friend) {
            this.postParameter(POST_FRIEND, friend);
            return this;
        }
    }
    public static final class RequestsBuilder extends TraktApiBuilder<List<UserProfile>> {
        private static final String URI = "/friends/requests/" + FIELD_API_KEY;

        private RequestsBuilder(FriendsService service) {
            super(service, new TypeToken<List<UserProfile>>() {}, URI, HttpMethod.Post);
        }
    }
}
