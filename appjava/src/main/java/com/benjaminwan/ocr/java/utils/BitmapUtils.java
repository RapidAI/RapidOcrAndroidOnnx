package com.benjaminwan.ocr.java.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;

public class BitmapUtils {
    public static Bitmap decodeUri(Context context, Uri imgUri) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(context.getContentResolver().openInputStream(imgUri), null, options);
    }
}
