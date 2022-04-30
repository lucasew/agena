package com.biglucas.demos;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class PageActivity extends AppCompatActivity {
    private URI url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        this.url = URI.create(this.getIntent().getData().toString());
        TextView urlText = findViewById(R.id.browser_url);
        urlText.setText(this.url.toString());
        handlePageReload(null);
    }

    public void handlePageGo(View view) { // this method is called from the XML
        TextView urlText = findViewById(R.id.browser_url);
        String urlToGoTo = urlText.getText().toString();
        URI destURL = this.url.resolve(URI.create(urlToGoTo));
        System.out.printf("scheme: '%s'", destURL.getScheme());
        new Invoker(this, destURL).invoke();
    }
    public void handlePageReload(View view) {
        handlePageLoad();
    }
    public void handleLoad(ArrayList<String> content) {
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageContentFragment(content, this.url))
                .commit();
    }
    private void handleLoad(Exception e) {
        String errText;
        Context appctx = this.getApplicationContext();
        if (e instanceof UnknownHostException) {
            errText = appctx.getString(R.string.error_unable_to_resolve_host);
        } else if (e instanceof SocketTimeoutException) {
            errText = appctx.getResources().getString(R.string.error_connection_timeout);
        } else if (e instanceof  FailedGeminiRequestException.GeminiNotFound) {
            errText = appctx.getResources().getString(R.string.error_gemini_not_found);
        } else if (e instanceof FailedGeminiRequestException.GeminiInvalidResponse) {
            errText = appctx.getResources().getString(R.string.error_gemini_invalid_response);
        } else if (e instanceof FailedGeminiRequestException.GeminiUnimplementedCase) {
            errText = appctx.getResources().getString(R.string.error_gemini_unimplemented);
        } else  {
            errText = appctx.getResources().getString(R.string.error_generic);
        }
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageErrorFragment(errText, e))
                .commit();
    }

    public void handlePageLoad() {
        handlePageLoad(this.url.toString());
    }
    public void handlePageLoad(String url) {
        System.out.println("page load");
        if (this.getSupportFragmentManager().isDestroyed()) return;
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageLoadingFragment())
                .commit();
        Uri uri = Uri.parse(url);
        System.out.println(uri.toString());
        ((TextView)this.findViewById(R.id.browser_url)).setText(uri.toString());
        PageActivity that = this;
        // TODO: fetch and rendering
        AsyncTask<String, Integer, ArrayList<String>> task = new AsyncTask<String, Integer, ArrayList<String>>() {
            private Exception exception;
            private ArrayList<String> list;

            @Override
            protected ArrayList<String> doInBackground(String ..._) {
                try {
                    System.out.println("* request na thread *");
                    this.list = (ArrayList<String>) GeminiSingleton.getGemini().request(that, that.url); // gambiarra alert
                } catch (Exception e) {
                    this.exception = e;
                }
                return this.list;
            }

            @Override
            protected void onPostExecute(ArrayList<String> _) {
                System.out.println("* post execute *");
                if (list != null) {
                    for (String item : list) {
                        System.out.println(item);
                    }
                }
                if (list != null) {
                    that.handleLoad(list);
                } else {
                    that.handleLoad(exception);
                }
            }
        };
        task.execute("");
    }
}