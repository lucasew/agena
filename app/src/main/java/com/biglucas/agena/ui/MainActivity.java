package com.biglucas.agena.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.Invoker;

public class MainActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request storage permission for debug builds on Android 6-12
        requestStoragePermissionIfNeeded();
    }

    private void requestStoragePermissionIfNeeded() {
        // Only request permission in debug builds
        boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebug) {
            return;
        }

        // Only needed for Android 6-12 (API 23-32)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Show explanation if needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this,
                    "Storage permission needed to persist history across app reinstalls (debug mode)",
                    Toast.LENGTH_LONG).show();
            }

            // Request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission granted! History will persist across uninstalls.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission denied. History will use private storage.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void onClickHistory(View view) {
        Intent intent = new Intent(this, HistoricActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        }
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