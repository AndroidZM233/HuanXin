package com.speedata.huanxin.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hyphenate.EMError;
import com.hyphenate.EMMessageListener;
import com.hyphenate.EMValueCallBack;
import com.hyphenate.chat.EMChatRoom;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMVoiceMessageBody;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.EasyUtils;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.speedata.huanxin.R;
import com.speedata.huanxin.model.EaseChatRowVoicePlayer;
import com.speedata.huanxin.model.EaseVoiceRecorder;
import com.speedata.huanxin.runtimepermissions.PermissionsManager;
import com.speedata.huanxin.utils.EaseCommonUtils;

import java.io.File;
import java.util.List;

/**
 *
 */
public class ChatActivity extends BaseActivity {
    public static ChatActivity activityInstance;
    String toChatUsername;
    protected EaseVoiceRecorder voiceRecorder;
    protected PowerManager.WakeLock wakeLock;
    private EaseChatRowVoicePlayer voicePlayer;
    private TextView mTv;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.em_activity_chat);
        activityInstance = this;
        toChatUsername = getIntent().getExtras().getString("userId");
        mTv = findViewById(R.id.tv_show);

        EMClient.getInstance().chatroomManager().joinChatRoom(toChatUsername, new EMValueCallBack<EMChatRoom>() {
            @Override
            public void onSuccess(EMChatRoom value) {
                Log.e("ZM", "已进入" + value.getName());
                voiceRecorder = new EaseVoiceRecorder(mHandler);
                wakeLock = ((PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(
                        PowerManager.SCREEN_DIM_WAKE_LOCK, "demo");

                EMClient.getInstance().chatManager().addMessageListener(msgListener);
                voicePlayer = EaseChatRowVoicePlayer.getInstance(getApplicationContext());
            }

            @Override
            public void onError(int error, String errorMsg) {
                ChatActivity.this.finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EMClient.getInstance().chatManager().removeMessageListener(msgListener);
        activityInstance = null;
    }


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

    @Override
    protected void onNewIntent(Intent intent) {
        // make sure only one chat activity is opened
        String username = intent.getStringExtra("userId");
        if (toChatUsername.equals(username))
            super.onNewIntent(intent);
        else {
            finish();
            startActivity(intent);
        }

    }

    @Override
    public void onBackPressed() {
        ChatActivity.this.finish();
    }

    public String getToChatUsername() {
        return toChatUsername;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    private boolean isFirst = true;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F5) {
            if (isFirst) {
                Log.e("ZM", "KEYCODE_F5 onKeyDown: 1");
                mTv.setVisibility(View.VISIBLE);
                EaseChatRowVoicePlayer voicePlayer = EaseChatRowVoicePlayer.getInstance(getApplicationContext());
                if (voicePlayer.isPlaying())
                    voicePlayer.stop();
                startRecording();
                isFirst = false;
            } else {
                mTv.setVisibility(View.GONE);
                try {
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
                    Toast.makeText(getApplicationContext(), R.string.send_failure_please, Toast.LENGTH_SHORT).show();
                }
                isFirst = true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }


    protected Handler mHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            int index = msg.what;
        }
    };

    public void startRecording() {
        if (!EaseCommonUtils.isSdcardExist()) {
            Toast.makeText(getApplicationContext(), R.string.Send_voice_need_sdcard_support, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), R.string.recoding_fail, Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public int stopRecoding() {
        if (wakeLock.isHeld())
            wakeLock.release();
        return voiceRecorder.stopRecoding();
    }

    public String getVoiceFilePath() {
        return voiceRecorder.getVoiceFilePath();
    }


    public synchronized void onBubbleClick(final EMMessage message) {
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
                    EMLog.i("ZM", "Voice body download status: " + voiceBody.downloadStatus());
                    switch (voiceBody.downloadStatus()) {
                        case PENDING:// Download not begin
                        case FAILED:// Download failed
                            asyncDownloadVoice(message);
                            break;
                        case DOWNLOADING:// During downloading
                            Toast.makeText(getApplicationContext(), st, Toast.LENGTH_SHORT).show();
                            break;
                        case SUCCESSED:// Download success
                            play(message);
                            break;
                    }
                }
            } else if (message.status() == EMMessage.Status.INPROGRESS) {
                Toast.makeText(getApplicationContext(), st, Toast.LENGTH_SHORT).show();
            } else if (message.status() == EMMessage.Status.FAIL) {
                Toast.makeText(getApplicationContext(), st, Toast.LENGTH_SHORT).show();
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
}
