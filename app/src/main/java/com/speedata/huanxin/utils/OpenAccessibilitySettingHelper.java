package com.speedata.huanxin.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

public class OpenAccessibilitySettingHelper {
    private static final String ACTION = "action";
    private static final String ACTION_START_ACCESSIBILITY_SETTING = "action_start_accessibility_setting";

    public static void jumpToSettingPage(Context context) {
        try {
//            Intent intent = new Intent(context, AccessibilityOpenHelperActivity.class);
//            intent.putExtra(ACTION, ACTION_START_ACCESSIBILITY_SETTING);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);

            String enabledServicesSetting = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            ComponentName selfComponentName = new ComponentName(context.getPackageName(),
                    "Your AccessibilityService Class Name");
            String flattenToString = selfComponentName.flattenToString();
            if (enabledServicesSetting == null ||
                    !enabledServicesSetting.contains(flattenToString)) {
                enabledServicesSetting += flattenToString;
            }
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    enabledServicesSetting);
            Settings.Secure.putInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 1);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }
}
