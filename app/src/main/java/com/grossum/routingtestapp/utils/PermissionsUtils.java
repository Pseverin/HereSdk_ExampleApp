package com.grossum.routingtestapp.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class PermissionsUtils {

    public final static int REQUEST_PERMISSIONS = 1000;


    public static boolean requestAllPermissions(Activity activity) {
        if (checkReadWriteExtStoragePermissionGranted(activity)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(activity,
                new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_PERMISSIONS);
            return false;
        }
    }


    public static boolean onRequestPermissionsResult(Activity activity, int requestCode,
                                                     String permissions[], int[] grantResults, boolean finishIfDeny) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS: {
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permissions granted!");
                    return true;
                } else {
                    System.out.println("Permissions blocked!");
                    if (finishIfDeny) {
                        activity.finish();
                    }
                    return false;
                }
            }
        }
        return false;
    }


    public static boolean checkReadWriteExtStoragePermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            &&
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
            &&
        ContextCompat.checkSelfPermission(context,
            Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    }
}
