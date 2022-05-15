package com.biglucas.agena.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;
import com.biglucas.agena.utils.DatabaseController;

import java.util.ArrayList;

public class HistoricActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historic);
        refreshHistoric();
    }

    public void refreshHistoric(View view) {
        refreshHistoric();
    }
    public void refreshHistoric() {
        ArrayList<String> historic = new DatabaseController(openOrCreateDatabase("history", Context.MODE_PRIVATE, null))
                .getHistoryLines();
        try {
            System.out.println(historic);
            GeminiPageContentFragment contentFragment = new GeminiPageContentFragment(historic, Uri.parse("gemini://example.com"));
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.historic_content, contentFragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}