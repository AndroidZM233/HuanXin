package com.speedata.huanxin.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.ViewConfiguration;

import java.io.File;

/**
 * 设备信息获取
 * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 * Created by 张明_ on 2017/8/22.
 * Email 741183142@qq.com
 */

public class DeviceUtils {

    /**
     * 获得手机型号简称(GT-N7102)
     *
     * @return String
     */
    public static String getModel() {
        return Build.MODEL;
    }

    /**
     * 手机型号(n7102)
     *
     * @return String
     */
    public static String getDeviceName() {
        return Build.DEVICE;
    }

    /**
     * 手机厂家(samsung)
     *
     * @return String
     */
    public static String getManufacturer() {
        // Build.BRAND 也可以
        return Build.MANUFACTURER;
    }

    /**
     * 硬件序列号(4dfd09d082156009)
     *
     * @return String
     */
    public static String getSerial() {
        return Build.SERIAL;
    }

    /**
     * 获得Mac地址(34:23:ba:14:07:62)
     * Android M(6.0)开始不允许获取Mac地址及蓝牙地址
     * so一般情况下不要用Mac地址做为任何判定
     * 避免系统升级导致的Bug
     *
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            @SuppressLint("MissingPermission") WifiInfo info = wifiManager.getConnectionInfo();
            return info.getMacAddress();
        }
        return null;
    }

    /**
     * 设备IMEI(355546057471164)
     * 6.0上没有权限会崩溃
     * IMEI是International Mobile Equipment Identity （国际移动设备标识）的简称
     * IMEI由15位数字组成的"电子串号"，它与每台手机一一对应，而且该码是全世界唯一的
     * 其组成为：
     * 1. 前6位数(TAC)是"型号核准号码"，一般代表机型
     * 2. 接着的2位数(FAC)是"最后装配号"，一般代表产地
     * 3. 之后的6位数(SNR)是"串号"，一般代表生产顺序号
     * 4. 最后1位数(SP)通常是"0"，为检验码，目前暂备用
     *
     * @return String
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getIMEI(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);

        String imei = null;
        if (mTelephonyManager != null) {
            try {
                imei = mTelephonyManager.getDeviceId();
            } catch (Exception e) {
                return "";
            }
        }
        return imei;
    }

    /**
     * SIM卡序列号(89860113871048601206)
     *
     * @return String
     */
    @SuppressLint({"HardwareIds", "MissingPermission"})
    public static String getSIMCardSerial(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            return mTelephonyManager.getSimSerialNumber();
        }
        return null;
    }

    /**
     * SIM卡Id(460013242301689)
     *
     * @return String
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String getSIMCardId(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            return mTelephonyManager.getSubscriberId();
        }
        return null;
    }

    /**
     * 手机号(+8615623240890)
     *
     * @return String
     */
    public static String getPhoneNum(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager) context.getApplicationContext().
                getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            @SuppressLint({"MissingPermission", "HardwareIds"})
            String phoneNumStr = mTelephonyManager.getLine1Number();
            if (phoneNumStr != null && phoneNumStr.startsWith("+86")) {
                phoneNumStr = phoneNumStr.substring(3);
            }
            long phoneNum = 0;
            try {
                phoneNum = Long.valueOf(phoneNumStr);
            } catch (Exception e) {
            }

            if (phoneNum > 0) {
                return phoneNumStr;
            }
        }

        return null;
    }

    /**
     * 设备唯一ID(cd2e5f0b4f365e41)
     *
     * @return String
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceUniqueId(Context context) {
        return Settings.Secure.getString(
                context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getOSName() {
        return Build.PRODUCT;
    }

    /**
     * 系统版本号(17)
     *
     * @return int
     */
    public static int getOSVersionCode() {
        return Build.VERSION.SDK_INT;
    }


    /**
     * 判断是否有物理按键
     *
     * @return boolean
     */
    public static boolean hasPermanentMenuKey(Context context) {
        return ViewConfiguration.get(context.getApplicationContext())
                .hasPermanentMenuKey();
    }

    /**
     * 是否透明底栏
     *
     * @return boolean
     */
    public static boolean isTranslucentNavigation(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                && !DeviceUtils.hasPermanentMenuKey(context);

    }

    /**
     * 获得SD卡存储大小
     *
     * @return long
     */
    public static long getSDCardTotalSize() {
        String sDcString = Environment.getExternalStorageState();
        if (sDcString.equals(Environment.MEDIA_MOUNTED)) {
            // 取得sdcard文件路径
            File pathFile = Environment.getExternalStorageDirectory();
            StatFs statfs = new StatFs(pathFile.getPath());
            // 获取SDCard上BLOCK总数
            long nTotalBlocks;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                nTotalBlocks = statfs.getBlockCountLong();
            } else {
                nTotalBlocks = statfs.getBlockCount();
            }
            // 获取SDCard上每个block的SIZE
            long nBlocSize;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                nBlocSize = statfs.getBlockSizeLong();
            } else {
                nBlocSize = statfs.getBlockSize();
            }
            // 计算SDCard 总容量大小MB
            return nTotalBlocks * nBlocSize / 1024 / 1024;
        } else {
            return 0;
        }
    }

    /**
     * 获得SD卡可用存储空间，单位是M
     *
     * @return long
     */
    public static long getSDCardFreeSize() {
        String sDcString = Environment.getExternalStorageState();
        if (sDcString.equals(Environment.MEDIA_MOUNTED)) {
            // 取得sdcard文件路径
            File pathFile = Environment.getExternalStorageDirectory();
            StatFs statfs = new StatFs(pathFile.getPath());
            // 获取可供程序使用的Block的数量
            long nAvailaBlock;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                nAvailaBlock = statfs.getAvailableBlocksLong();
            } else {
                nAvailaBlock = statfs.getAvailableBlocks();
            }
            // 获取SDCard上每个block的SIZE
            long nBlocSize;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                nBlocSize = statfs.getBlockSizeLong();
            } else {
                nBlocSize = statfs.getBlockSize();
            }
            return nAvailaBlock * nBlocSize / 1024 / 1024;
        } else {
            return 0;
        }
    }

    /**
     * 检查某个包是否安装
     *
     * @param packageName String
     * @return boolean
     */
    public static boolean checkApkExist(String packageName, Context context) {
        if (TextUtils.isEmpty(packageName)) return false;
        try {
            context.getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 安装APK
     *
     * @param apkFile apk地址
     * @param context 上下文
     */
    public static void installAPK(File apkFile, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(intent);
    }
}
