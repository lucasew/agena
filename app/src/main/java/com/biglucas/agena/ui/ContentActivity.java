package com.biglucas.agena.ui;

import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.biglucas.agena.protocol.gemini.GeminiPageContentFragment;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.biglucas.agena.R;

public class ContentActivity extends AppCompatActivity {
    private static final String TAG = "ContentActivity";
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
            Log.e(TAG, "No data in intent, finishing activity");
            finish();
            return;
        }

        try {
            Uri incomingUri = getIntent().getData();

            // VULNERABILITY: Before reading the file, we must check its size to prevent a DoS attack
            // from a malicious application providing a massive file, which could cause an OutOfMemoryError.
            // The check must be "fail-safe" - if the size cannot be determined, we must abort.
            try (Cursor cursor = getContentResolver().query(incomingUri, null, null, null, null)) {
                // If the cursor is null or empty, we cannot determine the size. Abort.
                if (cursor == null || !cursor.moveToFirst()) {
                    Log.e(TAG, "Could not determine file size: cursor is null or empty.");
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.error_reading_file)
                            .setMessage(R.string.error_reading_file_message_size)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size column doesn't exist or is null, we cannot determine the size. Abort.
                if (sizeIndex == -1 || cursor.isNull(sizeIndex)) {
                    Log.e(TAG, "Could not determine file size: size column not found or is null.");
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.error_reading_file)
                            .setMessage(R.string.error_reading_file_message_size)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }

                long fileSize = cursor.getLong(sizeIndex);
                if (fileSize > MAX_FILE_SIZE_BYTES) {
                    Log.w(TAG, "File size " + fileSize + " exceeds limit of " + MAX_FILE_SIZE_BYTES);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.file_too_large)
                            .setMessage(R.string.file_too_large_message)
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
                            .setOnCancelListener(dialogInterface -> finish())
                            .show();
                    return;
                }
            }

            Log.d(TAG, incomingUri.toString());
            InputStream inputStream = getContentResolver().openInputStream(incomingUri);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader dis = new BufferedReader(inputStreamReader);
            ArrayList<String> lines = new ArrayList<>();
            int lineCount = 0;
            while (true) {
                if (lineCount >= MAX_LINES) {
                    Log.w(TAG, "File exceeds MAX_LINES, truncating");
                    break;
                }
                lineCount++;
                String line = dis.readLine();
                if (line != null) {
                    Log.v(TAG, line);
                }
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
            Log.e(TAG, "Failed to handle intent", e);
            finish();
        }
    }
}
