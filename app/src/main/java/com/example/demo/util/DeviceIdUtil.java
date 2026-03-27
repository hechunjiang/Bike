package com.example.demo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.util.UUID;

public class DeviceIdUtil {
    private static final String PREFS_NAME = "device_id_prefs";
    private static final String KEY_UUID = "app_instance_uuid";

    public static String getAppUUID(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uuid = sp.getString(KEY_UUID, null);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString().replace("-", "");
            sp.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }
}
