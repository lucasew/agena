package com.biglucas.demos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class PageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page_activity);
        TextView urlText = findViewById(R.id.browsing_url);
        Intent intent = getIntent();
        Uri openUri = intent.getData();
        urlText.setText(openUri.toString());
        System.out.println(openUri.toString());
    }

}