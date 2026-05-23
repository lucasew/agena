package com.biglucas.agena.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Utility class for handling Android runtime permission requests.
 * <p>
 * Abstracts the boilerplate of checking permissions, showing rationale, and requesting
 * permissions into a single flow.
 */
public final class PermissionAsker {
    private static final String TAG = "PermissionAsker";
    private PermissionAsker() {
        // This class is not meant to be instantiated.
    }

    /**
     * Checks for a permission and requests it if not granted.
     * <p>
     * <b>Flow:</b>
     * <ol>
     *   <li>Checks if permission is already granted. If so, returns {@code true}.</li>
     *   <li>If denied previously (rationale needed), displays a Toast with the provided reason
     *       and returns {@code false}. This pauses the request to educate the user.
     *       Note: This implementation does not automatically request the permission after the Toast;
     *       the user may need to retry the action.</li>
     *   <li>If never asked or "Don't ask again" is not checked, requests the permission
     *       system dialog and returns {@code false}.</li>
     * </ol>
     *
     * @param activity The target activity context.
     * @param permission The manifest permission string (e.g., {@code Manifest.permission.WRITE_EXTERNAL_STORAGE}).
     * @param reason Resource ID for the explanation string (shown if rationale is triggered).
     * @return {@code true} if permission is already granted; {@code false} if a request is initiated or rationale shown.
     */
    public static boolean ensurePermission(Activity activity, String permission, int reason) {
        Log.d(TAG, "Ensuring permission: " + permission);
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            Toast.makeText(activity, activity.getResources().getString(reason), Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 0);
        }
        return false;
    }
}
