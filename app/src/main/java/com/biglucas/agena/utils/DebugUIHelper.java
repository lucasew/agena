package com.biglucas.agena.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class DebugUIHelper {
    private static final String TAG = "DebugUIHelper";

    // Cache the permission status to avoid repeated lookups.
    // The permission is granted at install time and won't change at runtime.
    private static Boolean hasManageExternalStoragePermission = null;

    private DebugUIHelper() {
        // This is a utility class and should not be instantiated
    }

    public static synchronized boolean hasManageExternalStoragePermission(Context context) {
        if (hasManageExternalStoragePermission != null) {
            return hasManageExternalStoragePermission;
        }

        try {
            // Use application context to avoid memory leaks
            Context appContext = context.getApplicationContext();
            String[] permissions = appContext.getPackageManager()
                    .getPackageInfo(appContext.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;

            if (permissions != null) {
                for (String permission : permissions) {
                    if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(permission)) {
                        hasManageExternalStoragePermission = true;
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
        }

        hasManageExternalStoragePermission = false;
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
