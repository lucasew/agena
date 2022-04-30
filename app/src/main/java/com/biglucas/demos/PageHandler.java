package com.biglucas.demos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class PageHandler {
    AppCompatActivity activity;
    private Gemini gemini;
    public URI getURI() {
        URI uri = URI.create(this.activity.getIntent().getData().toString());
        TextView tv = (TextView) this.activity.findViewById(R.id.browser_url);
        if (tv == null) {
            uri = URI.create(tv.getText().toString());
        }
        System.out.printf("gemini URI = '%s'", uri);
        return uri;
    }
    public Gemini getGemini() {
        if (gemini == null) {
            this.gemini = new Gemini(getURI());
        }
        return gemini;
    }

    public PageHandler(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void handleLoad(ArrayList<String> content) {
        this.activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageContentFragment(this, content))
                .addToBackStack(null)
                .commit();
    }
    private void handleLoad(Exception e) {
        String errText;
        Context appctx = this.activity.getApplicationContext();
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
        this.activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageErrorFragment(errText, e))
                .commit();
    }

    public void handlePageLoad() {
        handlePageLoad(getURI().toString());
    }
    public void handlePageLoad(String url) {
        System.out.println("page load");
        this.activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.browser_content, new PageLoadingFragment())
                .commit();
        Uri uri = Uri.parse(url);
        System.out.println(uri.toString());
        ((TextView)this.activity.findViewById(R.id.browser_url)).setText(uri.toString());
        PageHandler that = this;
        // TODO: fetch and rendering
        AsyncTask<String, Integer, ArrayList<String>> task = new AsyncTask<String, Integer, ArrayList<String>>() {
            private Exception exception;
            private ArrayList<String> list;

            @Override
            protected ArrayList<String> doInBackground(String ..._) {
                try {
                    System.out.println("* request na thread *");
                    this.list = (ArrayList<String>) that.getGemini().request(that.activity, URI.create(uri.toString())); // gambiarra alert
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
