package com.biglucas.demos;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.Future;

public class PageActivity extends AppCompatActivity {
    private PageHandler ph;

    private PageHandler getPageHandler() {
        if (this.ph == null) {
            this.ph = new PageHandler(this);
        }
        return this.ph;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        TextView urlText = findViewById(R.id.browser_url);
        getPageHandler();
        Intent intent = getIntent();
        Uri openUri = intent.getData();
        urlText.setText(openUri.toString());
        handlePageGo(null);
        System.out.println(openUri.toString());
    }

    public void handlePageGo(View view) { // this method is called from the XML
        TextView urlText = findViewById(R.id.browser_url);
        getPageHandler().handlePageLoad();
    }

}