package com.biglucas.demos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void onClickEnter(View view) {
        EditText input = (EditText) findViewById(R.id.url);
        String txt = input.getText().toString();
        if (!txt.startsWith("gemini://")) {
            txt = String.format("gemini://%s", txt);
        }
        Uri uri = Uri.parse(txt);
        System.out.println(uri.toString());
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }
}