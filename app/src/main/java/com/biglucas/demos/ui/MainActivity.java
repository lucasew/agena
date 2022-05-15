package com.biglucas.demos.ui;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.demos.utils.Invoker;
import com.biglucas.demos.R;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void onClickHistory(View view) {
        Intent intent = new Intent(this, HistoricActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }
    public void onClickEnter(View view) {
        EditText input = (EditText) findViewById(R.id.url);
        String txt = input.getText().toString();
        if (!txt.startsWith("gemini://")) {
            txt = String.format("gemini://%s", txt);
        }
        new Invoker(this, txt).invokeNewWindow();
    }
}