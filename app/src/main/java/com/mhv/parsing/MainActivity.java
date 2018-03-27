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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.mhv.parsing.model.LinkPreview;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.StringReader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private View linkPreviewContainer;
    private TextView titleView;
    private TextView descriptionView;
    private TextView rootView;
    private ImageView linkThumbView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        linkPreviewContainer = findViewById(R.id.link_prev_container);

        if (linkPreviewContainer != null) {
            titleView = linkPreviewContainer.findViewById(R.id.link_preview_title);
            descriptionView = linkPreviewContainer.findViewById(R.id.link_preview_description);
            rootView = linkPreviewContainer.findViewById(R.id.link_preview_url);
            linkThumbView = linkPreviewContainer.findViewById(R.id.link_message_thumbnail);
        }

        final EditText userInput = findViewById(R.id.user_input_view);
        userInput.setText("https://facebook.com");

        Button searchButton = findViewById(R.id.search_url_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String input = userInput.getText().toString();
                Log.d(TAG, "Input: " + input);
                if (!input.isEmpty()) {
                    linkPreviewContainer.setVisibility(View.GONE);
                    new SearchTask().execute(input);
                }
            }
        });
    }

    // TODO: Get this AsyncTask out of here to avoid memory leaks.
    private class SearchTask extends AsyncTask<String, Void, LinkPreview> {

        @Override
        protected LinkPreview doInBackground(String... strings) {
            String url = strings[0];
            HtmlExtractor extractor = new HtmlExtractor();

            String head;
            try {
                head = extractor.getHtmlMeta(url);
            } catch (IOException e) {
                Log.e(TAG, "An error has occurred while reading: " + url, e);
                return null;
            }

            if (head != null && !head.isEmpty()) {
                TagParser parserHelper = new TagParser();
                return parserHelper.parse(new StringReader(head));
            }

            return null;
        }

        @Override
        protected void onPostExecute(LinkPreview linkPreview) {
            if (linkPreview != null) {
                linkPreviewContainer.setVisibility(View.VISIBLE);
                String title = linkPreview.getTitle();
                String description = linkPreview.getDescription();
                String root = linkPreview.getRootAdd();
                String imageUrl = linkPreview.getImageUrl();

                if (title != null && !title.isEmpty()) {
                    titleView.setVisibility(View.VISIBLE);
                    titleView.setText(title);
                } else {
                    // If for some reason there is not title,
                    // better not show the preview at all.
                    linkPreviewContainer.setVisibility(View.GONE);
                }

                if (description != null && !description.isEmpty()) {
                    descriptionView.setVisibility(View.VISIBLE);
                    descriptionView.setText(description);
                } else {
                    descriptionView.setVisibility(View.GONE);
                }

                // If there is a URL, most likely there is a root address.
                rootView.setText(root);

                if (imageUrl != null && !imageUrl.isEmpty()) {
                    linkThumbView.setVisibility(View.VISIBLE);
                    Picasso.with(MainActivity.this)
                            .load(linkPreview.getImageUrl())
                            .into(linkThumbView);
                } else {
                    linkThumbView.setVisibility(View.GONE);
                }
            }
        }
    }
}
