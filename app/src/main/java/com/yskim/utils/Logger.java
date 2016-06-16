package com.yskim.utils;

import android.util.Log;

/**
 * Created by DevYoung on 16. 3. 5..
 */
public class Logger {
    private static final boolean DEBUG = true;
    private static final String TAG = "Logger";

    public static void e(String tag, String message) {
        println(Log.ERROR, tag, message);
    }

    public static void e(String message) {
        e(TAG, message);
    }

    public static void d(String tag, String message) {
        println(Log.DEBUG, tag, message);
    }

    public static void d(String message) {
        d(TAG, message);
    }


    public static void w(String tag, String message) {
        println(Log.WARN, tag, message);
    }

    public static void w(String message) {
        w(TAG, message);
    }

    public static void a(String tag, String message) {
        println(Log.ASSERT, tag, message);
    }

    public static void a(String message) {
        a(TAG, message);
    }


    public static void println(int priority, String tag, String message) {
        if ( DEBUG ) {
            Log.println(priority, tag, message);
        }
    }
}
