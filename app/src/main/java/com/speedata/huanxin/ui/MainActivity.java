package com.speedata.huanxin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.kaopiz.kprogresshud.KProgressHUD;
import com.speedata.huanxin.Constant;
import com.speedata.huanxin.R;
import com.speedata.huanxin.domain.MsgEvent;
import com.speedata.huanxin.runtimepermissions.PermissionsManager;
import com.speedata.huanxin.service.VoiceService;
import com.speedata.huanxin.utils.DeviceUtils;
import com.speedata.huanxin.utils.EaseCommonUtils;
import com.speedata.huanxin.utils.OpenAccessibilitySettingHelper;
import com.speedata.huanxin.utils.SharedXmlUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by 张明_ on 2018/11/29.
 * Email 741183142@qq.com
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private TextView tv1;
    private CheckBox cbStart;
    private TextView tvTitle;
    private TextView tvRoomId;
    private TextView tvId;
    private TextView tvStatus;
    private TextView tvKey;
    private ConstraintLayout viewSetting;
    private KProgressHUD kProgressHUD;
    private boolean setKey = false;

    @org.greenrobot.eventbus.Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MsgEvent mEvent) {
        String type = mEvent.getType();
        String msg = (String) mEvent.getMsg();
        if (Constant.EVENT_ERROR.equals(type)) {
            handleErrors();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else if (Constant.EVENT_LOGIN.equals(type)) {
            tvStatus.setText(msg);
        } else if (Constant.EVENT_ROOM_NAME.equals(type)) {
            tvRoomId.setText(msg);
            if (kProgressHUD != null) {
                kProgressHUD.dismiss();
            }
        }

    }


    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);
        kProgressHUD = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setCancellable(false)
                .setAnimationSpeed(2)
                .setDimAmount(0.5f);

        initView();

        if (!EaseCommonUtils.isNetWorkConnected(this)) {
            Toast.makeText(this, R.string.network_isnot_available, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isChecked = SharedXmlUtil.getInstance(getApplicationContext()).read("isChecked", false);
        cbStart.setChecked(isChecked);
    }


    private void initView() {
        tv1 = (TextView) findViewById(R.id.tv1);
        cbStart = (CheckBox) findViewById(R.id.cb_start);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvRoomId = (TextView) findViewById(R.id.tv_room_id);
        tvRoomId.setOnClickListener(this);
        tvId = (TextView) findViewById(R.id.tv_id);
        tvId.setText(DeviceUtils.getIMEI(getApplicationContext()));
        tvStatus = (TextView) findViewById(R.id.tv_status);
        tvKey = (TextView) findViewById(R.id.tv_key);
        tvKey.setOnClickListener(this);
        viewSetting = (ConstraintLayout) findViewById(R.id.view_setting);

        cbStart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            private Intent service;

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e("ZM", "onCheckedChanged: " + isChecked);
                SharedXmlUtil.getInstance(getApplicationContext()).write("isChecked", isChecked);
                if (isChecked) {
                    viewSetting.setVisibility(View.VISIBLE);
                    kProgressHUD.show();
                    service = new Intent(MainActivity.this, VoiceService.class);
                    startService(service);

                } else {
                    viewSetting.setVisibility(View.GONE);
                    if (service == null) {
                        stopService(service);
                    }

                }
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_room_id:
                stopService(new Intent(MainActivity.this, VoiceService.class));
                Intent intent = new Intent(MainActivity.this, PublicChatRoomsActivity.class);
                startActivity(intent);
                break;
            case R.id.tv_key:
                setKey = true;
                tvKey.setText("请按下按键....");
                break;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (setKey) {
            SharedXmlUtil.getInstance(getApplicationContext()).write(Constant.EVENT_BTN, keyCode);
            tvKey.setText("对讲物理按键：" + keyCode);
            setKey = false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int read = SharedXmlUtil.getInstance(getApplicationContext()).read(Constant.EVENT_BTN, 135);
        tvKey.setText("对讲物理按键：" + read);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (kProgressHUD != null) {
            kProgressHUD.dismiss();
        }
    }


    /**
     * 处理错误
     */
    private void handleErrors() {
        if (kProgressHUD != null) {
            kProgressHUD.dismiss();
        }
        cbStart.setChecked(false);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }


}
