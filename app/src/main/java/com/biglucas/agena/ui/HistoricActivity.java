package com.biglucas.agena.ui;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.R;
import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;
import com.biglucas.agena.utils.DatabaseController;
import com.biglucas.agena.utils.StacktraceDialogHandler;

import java.util.ArrayList;
import java.util.List;

public class HistoricActivity extends AppCompatActivity {

    private static final String TAG = HistoricActivity.class.getSimpleName();

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
        List<String> historic = new DatabaseController(DatabaseController.openDatabase(this))
                .getHistoryLines();
        try {
            Log.d(TAG, historic.toString());
            GeminiPageContentFragment contentFragment = new GeminiPageContentFragment(historic, Uri.parse("gemini://example.com"));
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.historic_content, contentFragment)
                    .commit();
        } catch (Exception e) {
            StacktraceDialogHandler.show(this, e);
        }
    }
}
