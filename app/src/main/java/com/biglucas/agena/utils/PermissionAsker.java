package com.biglucas.agena.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionAsker {
    public static boolean ensurePermission(Activity activity, String permission, int reason) {
        System.out.printf("Ensuring permission: '%s'\n", permission);
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
