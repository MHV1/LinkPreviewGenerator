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
import android.util.Xml;

import com.mhv.parsing.model.LinkPreview;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

public class TagParser {

    private static final String TAG = "TagParser";

    private HashMap<String, String> attrMap = new HashMap<>();

    public LinkPreview parse(Reader reader) {
        LinkPreview linkPreview = new LinkPreview();
        String text = "";

        XmlPullParserFactory factory;
        XmlPullParser parser;

        try {
            factory = XmlPullParserFactory.newInstance();
            parser = factory.newPullParser();
            // This feature helps ignoring rubbish characters and other crap found in many HTML tags.
            // So, just relax and move on, instead of throwing exotic parser exceptions.
            parser.setFeature(Xml.FEATURE_RELAXED, true);
            parser.setInput(reader);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        int attrCount = parser.getAttributeCount();
                        Log.d(TAG, "Start tag: " + tagName + " -- attr count: " + attrCount);

                        String attrName;
                        String attrValue;

                        String nameAttr = null;
                        String contentAttr = null;

                        if (attrCount > 0) {
                            for (int i = 0; i < attrCount; i++) {
                                attrName = parser.getAttributeName(i);
                                attrValue = parser.getAttributeValue(i);

                                // Beware that content and name attributes
                                // can appear in any order inside the tag.

                                if (attrName.equalsIgnoreCase("content")) {
                                    contentAttr = attrValue;
                                }

                                if (attrValue.equalsIgnoreCase("twitter:title") ||
                                        attrValue.equalsIgnoreCase("og:title")) {
                                    nameAttr = attrValue;

                                } else if (attrValue.equalsIgnoreCase("description")    ||
                                        attrValue.equalsIgnoreCase("og:description") ||
                                        attrValue.equalsIgnoreCase("twitter:description")) {
                                    nameAttr = attrValue;

                                } else if (attrValue.equalsIgnoreCase("og:image")) {
                                    nameAttr = attrValue;
                                }

                                if (nameAttr != null && contentAttr != null) {
                                    attrMap.put(nameAttr, contentAttr);
                                    Log.d(TAG, "Name: " + nameAttr + " content: " + contentAttr);
                                }

                                Log.d(TAG, " -- " + attrName + " " + attrValue);
                            }
                        }
                        break;

                    case XmlPullParser.TEXT:
                        text = parser.getText();
                        break;

                    case XmlPullParser.END_TAG:
                        if (tagName.equalsIgnoreCase("title")
                                && attrMap.get("title") == null) {
                            linkPreview.setTitle(text.trim());
                        } else if (tagName.equalsIgnoreCase("root")) {
                            linkPreview.setRootAdd(text);
                        }
                        break;
                }

                eventType = parser.next();
            }

            // This map can be taken way in future versions.
            // It's only here to contain the the info that has been previously parsed.
            for (Map.Entry<String, String> entry : attrMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.contains("title")) {
                    linkPreview.setTitle(value.trim());
                } else if (key.contains("description")) {
                    linkPreview.setDescription(value.trim());
                } else if (key.contains("image")) {
                    // If the image link points to a valid URL, then fetch it.
                    // If not, ignore it.
                    if (value.contains("http://") || value.contains("https://")){
                        linkPreview.setImageUrl(value);
                    }
                }
            }

            attrMap.clear();
            return linkPreview;

        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "An exception has occurred while parsing the head contents.", e);
            return linkPreview;
        }
    }
}
