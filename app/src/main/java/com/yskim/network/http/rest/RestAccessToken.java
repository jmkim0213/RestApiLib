package com.yskim.network.http.rest;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by DevYoung on 16. 4. 24..
 */
public class RestAccessToken {
    private static final String PREF_ACCESS_TOKEN = "ACCESS_TOKEN";

    public static void store(Context context, String accessToken) {
        getPref(context).edit().putString(PREF_ACCESS_TOKEN, accessToken).commit();
    }

    public static String load(Context context) {
        return getPref(context).getString(PREF_ACCESS_TOKEN, "");
    }

    public static void clear(Context context) {
        getPref(context).edit().clear().commit();
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(RestAccessToken.class.getName(), Context.MODE_PRIVATE);
    }
}
