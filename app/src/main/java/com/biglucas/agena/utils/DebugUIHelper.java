package com.biglucas.agena.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class DebugUIHelper {
    private static final String TAG = "DebugUIHelper";

    private DebugUIHelper() {
        // This is a utility class and should not be instantiated
    }

    public static boolean hasManageExternalStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        // This permission does not exist on versions older than Android 11 (API 30).
        // The previous implementation incorrectly checked the manifest instead of
        // performing a runtime check. Returning false is the correct behavior.
        return false;
    }

    public static void showToast(final Context context, final String message) {
        if (!hasManageExternalStoragePermission(context)) {
            Log.d(TAG, "Toast (release build, hidden): " + message);
            return;
        }

        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
        } else {
            Log.d(TAG, "Toast (no activity): " + message);
        }
    }
}
