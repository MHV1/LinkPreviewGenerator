/*
 * Copyright (C) 2018 Milan Herrera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mhv.parsing;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlExtractor {

    private static final String TAG = "HtmlExtractor";

    // Use the default user agent a desktop browser would use.
    // This helps avoiding problems exclusive to mobile versions of certain websites.
    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";

    // The following regex(s) help when finding the necessary tags for the link preview.
    // Pages that are so badly formatted and can't even follow this formatting
    // don't deserve to exist :)
    private static final Pattern HEAD_PATTERN =
            Pattern.compile("<head[^>]*>[\\w|\\t|\\r|\\W]*</head>",
            Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    private static final Pattern TITLE_PATTERN =
            Pattern.compile("<title[^>]*>(.*)</title>",
            Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    private static final Pattern META_PATTERN =
            Pattern.compile("<meta(.+?)[/]?>",
            Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

    public String getHtmlMeta(String urlString) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);

        ContentType contentType = getContentTypeHeader(connection);
        if (contentType == null) {
            return null;
        }

        if (!contentType.contentType.equals("text/html")) {
            Log.d(TAG, "Invalid content type. Not HTML: " + contentType.contentType);
            return null;
        } else {
            // If not charset has been specified use the default one.
            Charset charset = getCharset(contentType);
            if (charset == null) {
                charset = Charset.defaultCharset();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 400) {
                Log.e(TAG, "HTTP error while connecting to: "
                        + urlString + " response code: " + responseCode);
                // No point continuing...
                return null;
            }

            InputStream in = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));

            StringBuilder headTagContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                headTagContent.append(line);
                // Once the closing tag for the head has been found, stop reading.
                if (line.contains("</head>")) {
                    break;
                }
            }

            reader.close();

            // Get all necessary tags and put them all together in a single string.
            StringBuilder linkInfoBuilder = new StringBuilder();
            String head = getHeadTag(headTagContent.toString());

            String titleTag = getTitleTag(head);
            if (titleTag != null) {
                linkInfoBuilder.append(titleTag);
                linkInfoBuilder.append("\n");
            }

            String root = formatRooTAddress(urlString);
            linkInfoBuilder.append(root);
            linkInfoBuilder.append("\n");

            // Return the string containing the necessary tags to be parsed.
            return getMetaTags(head, linkInfoBuilder);
        }
    }

    private String getHeadTag(final String s) {
        final Matcher headMatcher = HEAD_PATTERN.matcher(s);
        if (headMatcher.find()) {
            String head = headMatcher.group(0);
            Log.d(TAG, "Head: " + head);
            return head;
        }
        return null;
    }

    private String getTitleTag(final String s) {
        final Matcher titleMatcher = TITLE_PATTERN.matcher(s);
        if (titleMatcher.find()) {
            String title = titleMatcher.group(0);
            Log.d(TAG, "Title: " + title);
            return title;
        } else {
            Log.d(TAG, "Title tag not found. Moving on to metadata...");
            return null;
        }
    }

    private String getMetaTags(final String s, StringBuilder builder) {
        final Matcher matcher = META_PATTERN.matcher(s);
        while (matcher.find()) {
            Log.d(TAG, "Meta: " + matcher.group(0));
            builder.append(matcher.group(0));
            builder.append("\n");
        }
        return builder.toString();
    }

    private String formatRooTAddress(String urlString) {
        String root = urlString.replaceFirst("^(http://|https://)","");
        root = root.split("/", 2)[0];
        return "<root>" + root + "</root>";
    }

    private static ContentType getContentTypeHeader(URLConnection conn) {
        int i = 0;
        boolean moreHeaders;

        do {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if (headerName != null && headerName.equals("Content-Type")) {
                Log.d(TAG, "Found content type: " + headerValue);
                return new ContentType(headerValue);
            }

            i++;
            moreHeaders = headerName != null || headerValue != null;

        } while (moreHeaders);

        return null;
    }

    private static Charset getCharset(ContentType contentType) {
        if (contentType != null &&
            contentType.charsetName != null &&
            Charset.isSupported(contentType.charsetName)) {
                Charset charset = Charset.forName(contentType.charsetName);
                Log.d(TAG, "Charset: " + charset.toString());
                return charset;
        }

        return null;
    }

    private static final class ContentType {

        private static final Pattern CHARSET_HEADER =
                Pattern.compile("charset=([-_a-zA-Z0-9]+)",
                Pattern.CASE_INSENSITIVE|Pattern.DOTALL);

        private String contentType;
        private String charsetName;

        private ContentType(String headerValue) {

            if (headerValue == null) {
                throw new IllegalArgumentException("ContentType must " +
                        "be constructed with a non-null headerValue");
            }

            int n = headerValue.indexOf(";");

            if (n != -1) {
                contentType = headerValue.substring(0, n);
                Matcher matcher = CHARSET_HEADER.matcher(headerValue);
                if (matcher.find()) {
                    charsetName = matcher.group(1);
                }
            } else {
                contentType = headerValue;
            }
        }
    }
}
