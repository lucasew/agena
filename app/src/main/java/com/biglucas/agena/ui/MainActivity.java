package com.biglucas.agena.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.biglucas.agena.R;
import com.biglucas.agena.utils.Invoker;

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

    private void requestStoragePermissionIfNeeded() {
        // Only request permission in debug builds
        boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        Log.d(TAG, "requestStoragePermissionIfNeeded - isDebug: " + isDebug);

        if (!isDebug) {
            Log.d(TAG, "Not debug build, skipping permission request");
            return;
        }

        // Only needed for Android 6-12 (API 23-32)
        Log.d(TAG, "SDK_INT: " + Build.VERSION.SDK_INT + " (M=" + Build.VERSION_CODES.M + ", TIRAMISU=" + Build.VERSION_CODES.TIRAMISU + ")");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "Android version < 6.0, no runtime permissions needed");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+, Downloads access very restricted, skipping");
            return;
        }

        // Check if permission is already granted
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED;

        Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission check: " + hasPermission + " (code: " + permissionCheck + ")");

        if (!hasPermission) {
            Log.d(TAG, "Permission not granted, requesting...");

            // Show explanation if needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.d(TAG, "Showing rationale to user");
                Toast.makeText(this,
                    "Storage permission needed to persist history across app reinstalls (debug mode)",
                    Toast.LENGTH_LONG).show();
            }

            // Request the permission
            Log.d(TAG, "Calling requestPermissions()");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            Log.d(TAG, "Permission already granted!");
            Toast.makeText(this, "Storage permission already granted!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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