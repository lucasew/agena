package com.biglucas.agena.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;
import com.biglucas.agena.utils.DatabaseController;
import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;

public class HistoricActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply Material You dynamic colors (Android 12+)
        DynamicColors.applyToActivityIfAvailable(this);

        setContentView(R.layout.activity_historic);
        refreshHistoric();
    }

    public void refreshHistoric(View view) {
        refreshHistoric();
    }
    public void refreshHistoric() {
        ArrayList<String> historic = new DatabaseController(DatabaseController.openDatabase(this))
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