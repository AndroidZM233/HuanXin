package com.speedata.huanxin.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
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
import com.speedata.huanxin.ui.dialog.WaveViewDialog;
import com.speedata.huanxin.utils.DeviceUtils;
import com.speedata.huanxin.utils.EaseCommonUtils;
import com.speedata.huanxin.utils.OpenAccessibilitySettingHelper;
import com.speedata.huanxin.utils.SharedXmlUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.List;

public class VoiceService extends Service {
    private boolean isRegister;
    private String toChatUsername;

    protected EaseVoiceRecorder voiceRecorder;
    protected PowerManager.WakeLock wakeLock;
    private EaseChatRowVoicePlayer voicePlayer;
    private boolean isFirst = true;
    private KeyReceiver keyReceiver;
    private WaveViewDialog waveViewDialog;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @SuppressLint("InvalidWakeLockTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.KEYCODE);
        keyReceiver = new KeyReceiver();
        registerReceiver(keyReceiver, intentFilter);

        voiceRecorder = new EaseVoiceRecorder(mHandler);
        wakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_DIM_WAKE_LOCK, "demo");
        voicePlayer = EaseChatRowVoicePlayer.getInstance(getApplicationContext());

        isRegister = SharedXmlUtil.getInstance(this).read(Constant.EVENT_IS_REGISTER, false);
        open();

        if (!OpenAccessibilitySettingHelper.isAccessibilitySettingsOn(this,
                VoiceAccessibilityService.class.getName())) {// 判断服务是否开启
            OpenAccessibilitySettingHelper.jumpToSettingPage(this);// 跳转到开启页面
        }

        return super.onStartCommand(intent, flags, startId);
    }

    class KeyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constant.KEYCODE)) {
                if (isFirst) {
                    start();
                } else {
                    stop();
                }
            }
        }
    }

    /**
     * 结束录音操作
     */
    private void stop() {
        try {
            waveViewDialog.dismiss();
            int length = stopRecoding();
            if (length > 0) {
                Log.e("ZM", "录音完毕");
                //filePath为语音文件路径，length为录音时间(秒)
                EMMessage message = EMMessage.createVoiceSendMessage(getVoiceFilePath(), length, toChatUsername);
                //如果是群聊，设置chattype，默认是单聊
                message.setChatType(EMMessage.ChatType.ChatRoom);
                EMClient.getInstance().chatManager().sendMessage(message);
            } else if (length == EMError.FILE_INVALID) {
                Toast.makeText(getApplicationContext(), R.string.Recording_without_permission, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.The_recording_time_is_too_short, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            EventBus.getDefault().post(new MsgEvent(Constant.EVENT_ERROR, e.toString()));
        }
        isFirst = true;
    }

    /**
     * 开始录音操作
     */
    private void start() {
        waveViewDialog = new WaveViewDialog(this);
        waveViewDialog.setCancelable(false);
        waveViewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        waveViewDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        waveViewDialog.show();
        Log.e("ZM", "KEYCODE_F5 onKeyDown: 1");
        EaseChatRowVoicePlayer voicePlayer = EaseChatRowVoicePlayer.getInstance(getApplicationContext());
        if (voicePlayer.isPlaying())
            voicePlayer.stop();
        startRecording();
        isFirst = false;
    }


    /**
     * 开启网络对讲
     */
    public void open() {
        if (!isRegister) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final String imei = DeviceUtils.getIMEI(getApplicationContext());
                        // call method in SDK
                        EMClient.getInstance().createAccount(imei, imei + "speedata");
                        // save current user
                        DemoHelper.getInstance().setCurrentUserName(imei);
                        SharedXmlUtil.getInstance(getApplicationContext()).write(Constant.EVENT_IS_REGISTER, true);
                        SharedXmlUtil.getInstance(getApplicationContext()).write(Constant.EVENT_ID, imei);
                        login();
                    } catch (final HyphenateException e) {
                        e.printStackTrace();
                        Log.e("ZM", "HyphenateException: " + e.toString());
                        int errorCode = e.getErrorCode();
                        if (errorCode == EMError.USER_ALREADY_EXIST) {
                            SharedXmlUtil.getInstance(getApplicationContext()).write(Constant.EVENT_IS_REGISTER, true);
                            login();
                        } else {
                            EventBus.getDefault().post(new MsgEvent(Constant.EVENT_ERROR, "注册出错" + e.toString()));
                        }

                    }
                }
            }).start();
        } else {
            login();
        }
    }


    /**
     * login
     */
    public void login() {
        String currentUsername = DeviceUtils.getIMEI(getApplicationContext());
        String currentPassword = currentUsername + "speedata";

        DemoDBManager.getInstance().closeDB();
        DemoHelper.getInstance().setCurrentUserName(currentUsername);

        Log.e("ZM", "EMClient.getInstance().login");
        EMClient.getInstance().login(currentUsername, currentPassword, new EMCallBack() {

            @Override
            public void onSuccess() {
                Log.e("ZM", "login: onSuccess");
                DemoHelper.getInstance().getUserProfileManager().asyncGetCurrentUserInfo();
                EventBus.getDefault().post(new MsgEvent(Constant.EVENT_LOGIN, "状态：已登陆"));

                toChatUsername = SharedXmlUtil.getInstance(getApplicationContext()).read(Constant.EVENT_ROOM_ID, "");
                if (TextUtils.isEmpty(toChatUsername)) {
                    Intent intent = new Intent(VoiceService.this, PublicChatRoomsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    startActivity(intent);
                } else {
                    joinChatRoom(toChatUsername);
                }
            }

            @Override
            public void onProgress(int progress, String status) {
                Log.d("ZM", "login: onProgress");
            }

            @Override
            public void onError(final int code, final String message) {
                Log.d("ZM", "login: onError: " + code);
                EventBus.getDefault().post(new MsgEvent(Constant.EVENT_ERROR, message));
            }
        });
    }


    /**
     * 加入聊天室
     */
    private void joinChatRoom(String roomId) {
        EMClient.getInstance().chatroomManager().joinChatRoom(roomId, new EMValueCallBack<EMChatRoom>() {
            @SuppressLint("InvalidWakeLockTag")
            @Override
            public void onSuccess(final EMChatRoom value) {
                Log.e("ZM", "已进入" + value.getName());
                EventBus.getDefault().post(new MsgEvent(Constant.EVENT_ROOM_NAME, "当前RoomID：" + value.getName()));
                EMClient.getInstance().chatManager().addMessageListener(msgListener);
            }

            @Override
            public void onError(int error, String errorMsg) {
                EventBus.getDefault().post(new MsgEvent(Constant.EVENT_ERROR, errorMsg));
            }
        });
    }


    /**
     * 关闭网络对讲
     */
    public void close() {
        EMClient.getInstance().chatroomManager().leaveChatRoom(toChatUsername);
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("ZM", "Service onDestroy");
        close();
        unregisterReceiver(keyReceiver);
    }


    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            int index = msg.what;
        }
    };

    EMMessageListener msgListener = new EMMessageListener() {

        @Override
        public void onMessageReceived(List<EMMessage> messages) {
            //收到消息
            Log.e("ZM", "收到消息" + messages.size());
            for (EMMessage message : messages) {
                long msgTime = message.getMsgTime();
                if (msgTime + 10000 > System.currentTimeMillis()) {
                    Log.e("zm", "播放声音 ");
                    onBubbleClick(message);
                }
            }
        }

        @Override
        public void onCmdMessageReceived(List<EMMessage> messages) {
            //收到透传消息
        }

        @Override
        public void onMessageRead(List<EMMessage> messages) {
            //收到已读回执
        }

        @Override
        public void onMessageDelivered(List<EMMessage> message) {
            //收到已送达回执
        }

        @Override
        public void onMessageRecalled(List<EMMessage> messages) {
            //消息被撤回
        }

        @Override
        public void onMessageChanged(EMMessage message, Object change) {
            //消息状态变动
        }
    };


    private void startRecording() {
        if (!EaseCommonUtils.isSdcardExist()) {
            showToast(getApplicationContext().getResources().getString(R.string.Send_voice_need_sdcard_support));
            return;
        }
        try {
            wakeLock.acquire();
            voiceRecorder.startRecording(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
            if (wakeLock.isHeld())
                wakeLock.release();
            if (voiceRecorder != null)
                voiceRecorder.discardRecording();
            showToast(getApplicationContext().getResources().getString(R.string.recoding_fail));
            return;
        }
    }

    private int stopRecoding() {
        if (wakeLock.isHeld())
            wakeLock.release();
        return voiceRecorder.stopRecoding();
    }

    private String getVoiceFilePath() {
        return voiceRecorder.getVoiceFilePath();
    }


    private synchronized void onBubbleClick(final EMMessage message) {
        String msgId = message.getMsgId();

        if (voicePlayer.isPlaying()) {
            // Stop the voice play first, no matter the playing voice item is this or others.
            voicePlayer.stop();
            // If the playing voice item is this item, only need stop play.
            String playingId = voicePlayer.getCurrentPlayingId();
            if (msgId.equals(playingId)) {
                return;
            }
        }

        if (message.direct() == EMMessage.Direct.SEND) {
            Log.e("ZM", "播放1");
            // Play the voice
            String localPath = ((EMVoiceMessageBody) message.getBody()).getLocalUrl();
            Log.e("zm", "localPath: " + localPath);
            File file = new File(localPath);
            SystemClock.sleep(300);
            Log.e("zm", "localPath: " + file.exists() + file.isFile());
            if (file.exists() && file.isFile()) {
                playVoice(message);
                Log.e("ZM", "播放2");
            } else {
                asyncDownloadVoice(message);
                Log.e("ZM", "播放3");
            }
        } else {
            Log.e("ZM", "播放4");
            final String st = getApplicationContext().getResources().getString(R.string.Is_download_voice_click_later);
            if (message.status() == EMMessage.Status.SUCCESS) {
                if (EMClient.getInstance().getOptions().getAutodownloadThumbnail()) {
                    play(message);
                } else {
                    EMVoiceMessageBody voiceBody = (EMVoiceMessageBody) message.getBody();
                    EMLog.e("ZM", "Voice body download status: " + voiceBody.downloadStatus());
                    switch (voiceBody.downloadStatus()) {
                        case PENDING:// Download not begin
                        case FAILED:// Download failed
                            asyncDownloadVoice(message);
                            break;
                        case DOWNLOADING:// During downloading
                            showToast(st);
                            break;
                        case SUCCESSED:// Download success
                            play(message);
                            break;
                    }
                }
            } else if (message.status() == EMMessage.Status.INPROGRESS) {
                showToast(st);
            } else if (message.status() == EMMessage.Status.FAIL) {
                showToast(st);
                asyncDownloadVoice(message);
            }
        }
    }

    private void playVoice(EMMessage msg) {
        voicePlayer.play(msg, new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.e("ZM", "声音播放完毕");
            }
        });
    }

    private void play(EMMessage message) {
        String localPath = ((EMVoiceMessageBody) message.getBody()).getLocalUrl();
        File file = new File(localPath);
        SystemClock.sleep(300);
        if (file.exists() && file.isFile()) {
            ackMessage(message);
            playVoice(message);
        } else {
            EMLog.e("ZM", "file not exist");
        }
    }

    private void ackMessage(EMMessage message) {
        EMMessage.ChatType chatType = message.getChatType();
        if (!message.isAcked() && chatType == EMMessage.ChatType.Chat) {
            try {
                EMClient.getInstance().chatManager().ackMessageRead(message.getFrom(), message.getMsgId());
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
        if (!message.isListened()) {
            EMClient.getInstance().chatManager().setVoiceMessageListened(message);
        }
    }

    private void asyncDownloadVoice(final EMMessage message) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                EMClient.getInstance().chatManager().downloadAttachment(message);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
            }
        }.execute();
    }


    private void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VoiceService.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
