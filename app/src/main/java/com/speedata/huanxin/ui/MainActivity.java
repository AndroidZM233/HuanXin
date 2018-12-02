package com.speedata.huanxin.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.speedata.huanxin.DemoApplication;
import com.speedata.huanxin.DemoHelper;
import com.speedata.huanxin.R;
import com.speedata.huanxin.db.DemoDBManager;
import com.speedata.huanxin.utils.DeviceUtils;
import com.speedata.huanxin.utils.EaseCommonUtils;
import com.speedata.huanxin.utils.SharedXmlUtil;

/**
 * Created by 张明_ on 2018/11/29.
 * Email 741183142@qq.com
 */
public class MainActivity extends BaseActivity {

    private TextView tv1;
    private CheckBox cbStart;
    private TextView tvTitle;
    private TextView tvRoomId;
    private TextView tvId;
    private TextView tvStatus;
    private TextView tvKey;
    private ConstraintLayout viewSetting;
    private boolean isRegister;
    private KProgressHUD kProgressHUD;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_main);
        initView();

        kProgressHUD = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);
        isRegister = SharedXmlUtil.getInstance(this).read("isRegister", false);
        if (!isRegister) {
            kProgressHUD.show();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final String imei = DeviceUtils.getIMEI(getApplicationContext());
                        // call method in SDK
                        EMClient.getInstance().createAccount(imei, imei + "speedata");
                        runOnUiThread(new Runnable() {
                            public void run() {
                                // save current user
                                DemoHelper.getInstance().setCurrentUserName(imei);
                                kProgressHUD.dismiss();
                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.Registered_successfully), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (final HyphenateException e) {
                        e.printStackTrace();

                    }
                }
            }).start();
        }
    }

    private void initView() {
        tv1 = (TextView) findViewById(R.id.tv1);
        cbStart = (CheckBox) findViewById(R.id.cb_start);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvRoomId = (TextView) findViewById(R.id.tv_room_id);
        tvId = (TextView) findViewById(R.id.tv_id);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvKey = (TextView) findViewById(R.id.tv_key);
        viewSetting = (ConstraintLayout) findViewById(R.id.view_setting);

        cbStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    viewSetting.setVisibility(View.VISIBLE);
                } else {
                    viewSetting.setVisibility(View.GONE);
                }
            }
        });
    }


    /**
     * login
     */
    public void login() {
        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUsername = DeviceUtils.getIMEI(getApplicationContext());
        String currentPassword = currentUsername + "speedata";

        if (TextUtils.isEmpty(currentUsername)) {
            Toast.makeText(this, R.string.User_name_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, R.string.Password_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        kProgressHUD.show();
        DemoDBManager.getInstance().closeDB();
        DemoHelper.getInstance().setCurrentUserName(currentUsername);

        final long start = System.currentTimeMillis();
        // call login method
        Log.d("ZM", "EMClient.getInstance().login");
        EMClient.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.d("ZM", "login: onSuccess");
                EMClient.getInstance().groupManager().loadAllGroups();
                EMClient.getInstance().chatManager().loadAllConversations();

                boolean updatenick = EMClient.getInstance().pushManager().updatePushNickname(
                        DemoApplication.currentUserNick.trim());
                if (!updatenick) {
                    Log.e("LoginActivity", "update current user nick fail");
                }

                DemoHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();

                tvStatus.setText("状态：已登录");
            }

            @Override
            public void onProgress(int progress, String status) {
                Log.d("ZM", "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d("ZM", "login: onError: " + code);
                runOnUiThread(new Runnable() {
                    public void run() {
                        kProgressHUD.dismiss();
                        Toast.makeText(getApplicationContext(), getString(R.string.Login_failed) + message,
                                Toast.LENGTH_SHORT).show();
                        tvStatus.setText("状态：登录失败");
                    }
                });
            }
        });
    }
}
