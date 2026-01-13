package com.biglucas.agena.ui;

import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.biglucas.agena.R;

public class ContentActivity extends AppCompatActivity {
    static final Logger logger = Logger.getLogger(ContentActivity.class.getName());
    private static final int MAX_LINES = 10000;
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

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
            Uri incomingUri = getIntent().getData();

            // VULNERABILITY: Before reading the file, we must check its size to prevent a DoS attack
            // from a malicious application providing a massive file, which could cause an OutOfMemoryError.
            // We must also handle cases where the size cannot be determined and fail safely.
            try (Cursor cursor = getContentResolver().query(incomingUri, null, null, null, null)) {
                if (cursor == null || !cursor.moveToFirst()) {
                    // If the cursor is null or empty, we cannot determine the file size.
                    logger.log(Level.WARNING, "Could not determine file size from ContentResolver");
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.cannot_determine_file_size_title)
                            .setMessage(R.string.cannot_determine_file_size_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex == -1 || cursor.isNull(sizeIndex)) {
                    // If the size column doesn't exist or is null, we cannot proceed.
                    logger.log(Level.WARNING, "File size column is missing or null");
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.cannot_determine_file_size_title)
                            .setMessage(R.string.cannot_determine_file_size_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }

                long fileSize = cursor.getLong(sizeIndex);
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    logger.log(Level.WARNING, "File size " + fileSize + " exceeds limit of " + MAX_FILE_SIZE_BYTES);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.file_too_large)
                            .setMessage(R.string.file_too_large_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }
            }

            logger.log(Level.FINER, "{}", incomingUri);
            InputStream inputStream = getContentResolver().openInputStream(incomingUri);
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
            finish();
        }
    }
}