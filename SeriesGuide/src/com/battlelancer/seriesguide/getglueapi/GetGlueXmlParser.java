/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.battlelancer.seriesguide.getglueapi;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses XML feeds from stackoverflow.com. Given an InputStream
 * representation of a feed, it returns a List of entries, where each list
 * element represents a single entry (post) in the XML feed.
 */
public class GetGlueXmlParser {
    private static final String ns = null;

    // We don't use namespaces

    public List<Interaction> parseInteractions(InputStream in) throws XmlPullParserException,
            IOException {
        return new InteractionXmlParser().parse(in);
    }

    public Error parseError(InputStream in) throws XmlPullParserException,
            IOException {
        List<Error> errors = new ErrorXmlParser().parse(in);
        if (errors.size() > 0) {
            return errors.get(0);
        }
        return null;
    }

    public List<GetGlueObject> parseObjects(InputStream in) throws XmlPullParserException {
        return null;
    }

    private abstract class GenericGetGlueXmlParser<T> {
        private String mSecondLevelTag = "";

        public List<T> parse(InputStream in) throws XmlPullParserException,
                IOException {
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(in, null);
                parser.nextTag();
                return readGetGlueContent(parser);
            } finally {
                in.close();
            }
        }

        private List<T> readGetGlueContent(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            parser.require(XmlPullParser.START_TAG, ns, "adaptiveblue");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals(mSecondLevelTag)) {
                    return readContent(parser);
                } else {
                    skip(parser);
                }
            }
            return new ArrayList<T>();
        }

        protected abstract List<T> readContent(XmlPullParser parser) throws XmlPullParserException,
                IOException;

        // Processes text tags in the response.
        protected String readTextTag(XmlPullParser parser, String tag) throws IOException,
                XmlPullParserException {
            parser.require(XmlPullParser.START_TAG, ns, tag);
            String text = readText(parser);
            parser.require(XmlPullParser.END_TAG, ns, tag);
            return text;
        }

        // For text tags, extracts their text values.
        protected String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
            String result = "";
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText();
                parser.nextTag();
            }
            return result;
        }

        /*
         * Skips tags the parser isn't interested in. Uses depth to handle
         * nested tags. i.e., if the next tag after a START_TAG isn't a matching
         * END_TAG, it keeps going until it finds the matching END_TAG (as
         * indicated by the value of "depth" being 0).
         */
        protected void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException();
            }
            int depth = 1;
            while (depth != 0) {
                switch (parser.next()) {
                    case XmlPullParser.END_TAG:
                        depth--;
                        break;
                    case XmlPullParser.START_TAG:
                        depth++;
                        break;
                }
            }
        }
    }

    private class InteractionXmlParser extends GenericGetGlueXmlParser<Interaction> {

        public InteractionXmlParser() {
            super.mSecondLevelTag = "response";
        }

        @Override
        protected List<Interaction> readContent(XmlPullParser parser)
                throws XmlPullParserException, IOException {
            parser.require(XmlPullParser.START_TAG, ns, "response");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("interactions")) {
                    return readInteractions(parser);
                } else {
                    skip(parser);
                }
            }
            return new ArrayList<Interaction>();
        }

        private List<Interaction> readInteractions(XmlPullParser parser)
                throws XmlPullParserException,
                IOException {
            parser.require(XmlPullParser.START_TAG, ns, "interactions");
            List<Interaction> interactions = new ArrayList<Interaction>();
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("interaction")) {
                    interactions.add(readInteraction(parser));
                } else {
                    skip(parser);
                }
            }
            return interactions;
        }

        private Interaction readInteraction(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            parser.require(XmlPullParser.START_TAG, ns, "interaction");
            String title = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("title")) {
                    title = readTextTag(parser, "title");
                } else {
                    skip(parser);
                }
            }
            return new Interaction(title);
        }

    }

    private class ErrorXmlParser extends GenericGetGlueXmlParser<Error> {

        public ErrorXmlParser() {
            super.mSecondLevelTag = "error";
        }

        @Override
        protected List<Error> readContent(XmlPullParser parser) throws XmlPullParserException,
                IOException {
            parser.require(XmlPullParser.START_TAG, ns, "error");
            String code = null;
            String errorName = null;
            String message = null;
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("code")) {
                    code = readTextTag(parser, "code");
                } else if (name.equals("error")) {
                    errorName = readTextTag(parser, "name");
                } else if (name.equals("message")) {
                    message = readTextTag(parser, "message");
                } else {
                    skip(parser);
                }
            }
            List<Error> error = new ArrayList<Error>();
            error.add(new Error(code, errorName, message));
            return error;
        }

    }

    /**
     * Interaction format used by GetGlue (<a
     * href="http://o.getglue.com/api#interaction-format"
     * >http://o.getglue.com/api#interaction-format</a>).<br>
     * <b>Attention:</b> There are additional fields not used here.
     */
    public static class Interaction {
        public final String title;

        private Interaction(String title) {
            this.title = title;
        }
    }

    /**
     * Object format used by GetGlue (<a
     * href="http://getglue.com/api#networkwide-methods"
     * >http://getglue.com/api#networkwide-methods</a>).
     */
    public static class GetGlueObject {
        public final String title;
        public final String key;

        private GetGlueObject(String title, String key) {
            this.title = title;
            this.key = key;
        }
    }

    /**
     * Error format used by GetGlue (<a
     * href="http://getglue.com/api#glue-api-errors"
     * >http://getglue.com/api#glue-api-errors</a>).<br>
     * <b>Attention:</b> There are additional fields not used here.
     */
    public static class Error {
        public final String code;
        public final String name;
        public final String message;

        private Error(String code, String name, String message) {
            this.code = code;
            this.name = name;
            this.message = message;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s:\n%s", code, name, message);
        }
    }

}
