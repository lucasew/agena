package com.biglucas.demos;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentContainer;
import androidx.fragment.app.FragmentContainerView;
import androidx.preference.PreferenceFragmentCompat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

    public void handlePageLoaded(ArrayList<String> content) {
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageContentFragment(content))
                .addToBackStack(null)
                .commit();
    }
    public synchronized void handlePageLoaded(Exception e) {
        String errText;
        if (e instanceof UnknownHostException) {
            errText = this.getApplicationContext().getResources().getString(R.string.error_unable_to_resolve_host);
        } else if (e instanceof SocketTimeoutException) {
            errText = this.getApplicationContext().getResources().getString(R.string.error_connection_timeout);
        } else if (e instanceof  FailedGeminiRequestException.GeminiNotFound) {
            errText = this.getApplicationContext().getResources().getString(R.string.error_gemini_not_found);
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidResponse) {
            errText = this.getApplicationContext().getResources().getString(R.string.error_gemini_invalid_response);
        } else if (e instanceof FailedGeminiRequestException.GeminiUnimplementedCase) {
            errText = this.getApplicationContext().getResources().getString(R.string.error_gemini_unimplemented);
        } else  {
            errText = this.getApplicationContext().getResources().getString(R.string.error_generic);
        }
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageErrorFragment(errText, e))
                .commit();
    }
    public void handlePageLoad(String url) {
        System.out.println("page load");
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageLoadingFragment())
                .commit();
        if (!url.startsWith("gemini://")) {
            url = String.format("gemini://%s", url);
        }
        Uri uri = Uri.parse(url);
        System.out.println(uri.toString());
        ((TextView)findViewById(R.id.browser_url)).setText(uri.toString());
        // TODO: fetch and rendering
        try {
            ArrayList<String> list = (ArrayList<String>) this.gemini.request(this, URI.create(url.toString()));
            handlePageLoaded(list);
        } catch (Exception e) {
            handlePageLoaded(e);
        }
    }

    public void handlePageGo(View view) {
        TextView urlText = findViewById(R.id.browser_url);
        handlePageLoad(urlText.getText().toString());
    }

}