package com.biglucas.agena.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.biglucas.agena.R;

public class ContentActivity extends AppCompatActivity {
    static final Logger logger = Logger.getLogger(ContentActivity.class.getName());
    private static final int MAX_LINES = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        handleIntentOpen();
    }

    private void handleIntentOpen() {
        if (getIntent().getData() == null) {
            logger.log(Level.SEVERE, "No data in intent, finishing activity");
            finish();
            return;
        }

        try {
            logger.log(Level.FINER, "{}", getIntent().getData());
            InputStream inputStream = getContentResolver().openInputStream(getIntent().getData());
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader dis = new BufferedReader(inputStreamReader);
            ArrayList<String> lines = new ArrayList<>();
            int lineCount = 0;
            while (true) {
                if (lineCount >= MAX_LINES) {
                    logger.log(Level.WARNING, "File exceeds MAX_LINES, truncating");
                    break;
                }
                lineCount++;
                String line = dis.readLine();
                logger.log(Level.FINER, line);
                if (line == null) {
                    break;
                }
                lines.add(line);
            }
            this.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.browser_content, new GeminiPageContentFragment(lines, this.getIntent().getData()))
                    .commit();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to handle intent", e);
        }
    }
}