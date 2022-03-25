package com.biglucas.demos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import java.net.URI;
import java.net.URL;
import java.util.List;

public class PageActivity extends AppCompatActivity {
    private Gemini gemini;
    private Uri getUri() {
        TextView urlText = findViewById(R.id.browser_url);
        String str = urlText.getText().toString();
        Uri uri = Uri.parse(str);
        return uri;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        TextView urlText = findViewById(R.id.browser_url);
        Intent intent = getIntent();
        Uri openUri = intent.getData();
        urlText.setText(openUri.toString());
        this.gemini = new Gemini(URI.create(openUri.toString()));
        System.out.println(openUri.toString());
    }

    public void handlePageLoad(String url) {
        if (!url.startsWith("gemini://")) {
            url = String.format("gemini://%s", url);
        }
        Uri uri = Uri.parse(url);
        System.out.println(uri.toString());
        ((TextView)findViewById(R.id.browser_url)).setText(uri.toString());
        LinearLayout content = findViewById(R.id.browser_content);
        content.removeAllViewsInLayout();

        // TODO: fetch and rendering
        try {
            List<String> list = this.gemini.request(this, URI.create(url.toString()));
            for (String item : list) {
                TextView tv = new TextView(this);
                tv.setText(item);
                content.addView(tv);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handlePageGo(View view) {
        Uri uri = getUri();

        TextView urlText = findViewById(R.id.browser_url);
        handlePageLoad(urlText.getText().toString());
    }

}