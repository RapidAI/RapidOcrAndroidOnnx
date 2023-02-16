package com.benjaminwan.ocr.java.utils;

import android.app.Activity;
import android.widget.Toast;

public class ToastUtils {
    public static void showToast(Activity activity, CharSequence msg, int duration) {
        Toast.makeText(activity.getApplicationContext(), msg, duration).show();
    }
}
