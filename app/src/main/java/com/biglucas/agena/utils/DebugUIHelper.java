package com.biglucas.agena.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

public class DebugUIHelper {
    private static final String TAG = "DebugUIHelper";
    private static Boolean hasManageExternalStorage = null;

    private DebugUIHelper() {
        // This is a utility class and should not be instantiated
    }

    public static boolean hasManageExternalStoragePermission(Context context) {
        if (hasManageExternalStorage != null) {
            return hasManageExternalStorage;
        }
        try {
            String[] permissions = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                .requestedPermissions;

            if (permissions != null) {
                for (String permission : permissions) {
                    if ("android.permission.MANAGE_EXTERNAL_STORAGE".equals(permission)) {
                        hasManageExternalStorage = true;
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage());
        }
        hasManageExternalStorage = false;
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
