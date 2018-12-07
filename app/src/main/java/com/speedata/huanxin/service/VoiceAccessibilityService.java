package com.speedata.huanxin.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import com.speedata.huanxin.Constant;
import com.speedata.huanxin.DemoHelper;
import com.speedata.huanxin.R;
import com.speedata.huanxin.db.DemoDBManager;
import com.speedata.huanxin.domain.MsgEvent;
import com.speedata.huanxin.model.EaseChatRowVoicePlayer;
import com.speedata.huanxin.model.EaseVoiceRecorder;
import com.speedata.huanxin.ui.PublicChatRoomsActivity;
import com.speedata.huanxin.utils.DeviceUtils;
import com.speedata.huanxin.utils.EaseCommonUtils;
import com.speedata.huanxin.utils.SharedXmlUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.List;

public class VoiceAccessibilityService extends AccessibilityService {

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int key = event.getKeyCode();
        int repeatCount = event.getRepeatCount();
        int read = SharedXmlUtil.getInstance(getApplicationContext()).read(Constant.EVENT_BTN, 135);
        Log.e("ZM", "onKeyEvent" + action + key + repeatCount + read);
        if (read == key) {
            if (repeatCount == 0 && action == KeyEvent.ACTION_DOWN) {
                Intent intent = new Intent(Constant.KEYCODE);
                sendBroadcast(intent);
                Log.e("ZM", "发送广播");
            }
        }
        return super.onKeyEvent(event);
    }

    /**
     * 启动
     */
    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.e("ZM", "VoiceAccessibilityService onServiceConnected");
    }

}
