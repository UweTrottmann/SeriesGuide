package com.jakewharton.trakt.services;

import com.google.myjson.reflect.TypeToken;
import com.jakewharton.trakt.TraktApiBuilder;
import com.jakewharton.trakt.TraktApiService;
import com.jakewharton.trakt.entities.Response;

public class AccountService extends TraktApiService {
    /**
     * Create a new trakt account. Username and e-mail must be unique and not
     * already exist in trakt.
     *
     * @param username Username to register.
     * @param password SHA1 hash of password.
     * @param email E-mail to register, a welcome email will automatically be sent here.
     * @return Builder instance.
     */
    public CreateBuilder create(String username, String password, String email) {
        return (new CreateBuilder(this)).username(username).password(password).email(email);
    }

    /**
     * Test trakt credentials. This is useful for your configuration screen and
     * is a simple way to test someone's trakt account.
     *
     * @return Builder instance.
     */
    public TestBuilder test() {
        return new TestBuilder(this);
    }


    public static final class CreateBuilder extends TraktApiBuilder<Response> {
        private static final String POST_USERNAME = "username";
        private static final String POST_PASSWORD = "password";
        private static final String POST_EMAIL = "email";

        private static final String URI = "/account/create/" + FIELD_API_KEY;

        private CreateBuilder(AccountService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }

        /**
         * Username to register.
         *
         * @param username Value.
         * @return Builder instance.
         */
        public CreateBuilder username(String username) {
            this.postParameter(POST_USERNAME, username);
            return this;
        }

        /**
         * SHA1 hash of password.
         *
         * @param password Value.
         * @return Builder instance.
         */
        public CreateBuilder password(String password) {
            this.postParameter(POST_PASSWORD, password);
            return this;
        }

        /**
         * E-mail to register, a welcome email will automatically be sent here.
         *
         * @param email Value.
         * @return Builder instance.
         */
        public CreateBuilder email(String email) {
            this.postParameter(POST_EMAIL, email);
            return this;
        }
    }
    public static final class TestBuilder extends TraktApiBuilder<Response> {
        private static final String URI = "/account/test/" + FIELD_API_KEY;

        private TestBuilder(AccountService service) {
            super(service, new TypeToken<Response>() {}, URI, HttpMethod.Post);
        }
    }
}
