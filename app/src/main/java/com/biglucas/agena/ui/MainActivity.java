package com.biglucas.agena.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.Invoker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity onCreate - SDK_INT: " + Build.VERSION.SDK_INT);

        // Request storage permission for debug builds on Android 6-12
        requestStoragePermissionIfNeeded();
    }

    private boolean hasManageExternalStoragePermission() {
        try {
            String[] permissions = getPackageManager()
                .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS)
                .requestedPermissions;

            if (permissions != null) {
                for (String permission : permissions) {
                    if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
        }
        return false;
    }

    private void requestStoragePermissionIfNeeded() {
        // Detect debug build by checking if MANAGE_EXTERNAL_STORAGE permission exists in manifest
        // This permission is only declared in src/debug/AndroidManifest.xml
        boolean isDebug = hasManageExternalStoragePermission();
        Log.d(TAG, "requestStoragePermissionIfNeeded - isDebug: " + isDebug);

        if (!isDebug) {
            Log.d(TAG, "Not debug build, skipping permission request");
            return;
        }

        Toast.makeText(this, "🔍 DEBUG: Checking permissions... SDK=" + Build.VERSION.SDK_INT, Toast.LENGTH_LONG).show();

        Log.d(TAG, "SDK_INT: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "Android version < 6.0, no runtime permissions needed");
            Toast.makeText(this, "⚠️ DEBUG: Android < 6.0, no permissions needed", Toast.LENGTH_LONG).show();
            return;
        }

        // Android 13+ (API 33+): Use MANAGE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Android 11+ detected, checking MANAGE_EXTERNAL_STORAGE");

            if (Environment.isExternalStorageManager()) {
                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE already granted!");
                Toast.makeText(this, "✅ DEBUG: All files access granted! DB will use Downloads/AGENA", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "MANAGE_EXTERNAL_STORAGE not granted, opening settings");
                Toast.makeText(this,
                    "📁 DEBUG: Need 'All files access' permission.\n\nOpening Settings...\n\nEnable: Allow management of all files",
                    Toast.LENGTH_LONG).show();

                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open settings: " + e.getMessage());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivity(intent);
                }
            }
            return;
        }

        // Android 6-12 (API 23-32): Use WRITE_EXTERNAL_STORAGE
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission check: " + hasPermission + " (code: " + permissionCheck + ")");

        if (!hasPermission) {
            Log.d(TAG, "Permission not granted, requesting...");
            Toast.makeText(this, "🔐 DEBUG: Requesting storage permission...", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Permission already granted!");
            Toast.makeText(this, "✅ DEBUG: Storage permission already granted!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.d(TAG, "onRequestPermissionsResult - requestCode: " + requestCode);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0) {
                boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                Log.d(TAG, "Permission result: " + (granted ? "GRANTED" : "DENIED"));

                if (granted) {
                    Toast.makeText(this, "Storage permission granted! History will persist across uninstalls.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Storage permission denied. History will use private storage.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "grantResults array is empty!");
            }
        }
    }
    public void onClickHistory(View view) {
        Intent intent = new Intent(this, HistoricActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        startActivity(intent);
    }
    public void onClickEnter(View view) {
        EditText input = findViewById(R.id.url);
        String userInput = input.getText().toString().trim();

        if (userInput.isEmpty()) {
            return;
        }

        Uri uri;
        try {
            uri = Uri.parse(userInput);
        } catch (Exception e) {
             new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.error_invalid_uri)
                .setMessage(e.getMessage())
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        String finalUriString;
        if (uri.getScheme() == null) {
            finalUriString = "gemini://" + userInput;
        } else if ("gemini".equalsIgnoreCase(uri.getScheme())) {
            finalUriString = userInput;
        } else {
            Toast.makeText(this, getString(R.string.error_unsupported_scheme, uri.getScheme()), Toast.LENGTH_LONG).show();
            return;
        }

        Invoker.invokeNewWindow(this, finalUriString);
    }
}
