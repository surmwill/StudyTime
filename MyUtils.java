package com.example.st;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.support.design.widget.Snackbar;
import android.text.InputFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;

public class MyUtils {
    public static long ONE_MEGABYTE = 1024 * 1024;;

    public static int convDpToPx(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    public static float convPxToDp(Context context, float px) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static void shitDebug(String message) {
        Log.d("Shit", message);
    }

    public static void shitDebug(int message) {
        Log.d("Shit", Integer.toString(message));
    }



    public static String getLongString(int size) {
        String long_string = "";
        for(int i = 0; i < size; i++) {
            long_string += "A";
        }

        return long_string;
    }

    public static void CheckMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        final long maxHeapSizeInMB = runtime.maxMemory() / 1048576L;
        final long availHeapSizeInMB = maxHeapSizeInMB - usedMemInMB;

        shitDebug("usedMemInMB: " + Long.toString(usedMemInMB));
        shitDebug("maxHeapSizeInMB: " + Long.toString(maxHeapSizeInMB));
        shitDebug("availHeapSizeInMB: " + Long.toString(availHeapSizeInMB));
    }

    // Resize a bitmap to reqWidth x reqHeight
    public static Bitmap getResizedBitmap(Bitmap bitmap, float reqWidth, float reqHeight) {
        // holds a 3x3 matrix for transforming coordinates
        Matrix matrix = new Matrix();

        RectF src = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF dest = new RectF(0, 0, reqWidth, reqHeight);

        // Set the matrix to scale and translate values that map the source rectangle to the destination rectangle
        matrix.setRectToRect(src, dest, Matrix.ScaleToFit.CENTER);

        // returns the bitmap transformed by the matrix
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static void simpleSnackBar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public static void addUpperCaseFilter(EditText editText) {
        InputFilter[] editFilters = editText.getFilters();
        InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
        newFilters[editFilters.length] = new InputFilter.AllCaps();
        editText.setFilters(newFilters);
    }

    // find the index of the first word of every uppercase letter in the alphabet
    // use this with the list of subjects so we have an easier time filtering the nav menu
    public static void alphabetIndices(String [] array) {
        char letter = 65;

        for(; letter <= 90; letter++) {
            for(int i = 0; i < array.length; i++) {
                final String word = array[i];
                if(word.charAt(0) == letter) {
                    shitDebug(Character.toString(letter) + " " + Integer.toString(i));
                    break;
                }
            }
        }
    }
}
