package com.speedata.huanxin.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.speedata.huanxin.R;
import com.speedata.huanxin.view.WaveView;

/**
 * Created by 张明_ on 2018/12/17.
 * Email 741183142@qq.com
 */
public class WaveViewDialog extends Dialog {
    private WaveView mWave;

    public WaveViewDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        mWave = findViewById(R.id.wave);
//        mWave.setVolume(10);
        mWave.startAnim();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWave.stopAnim();
    }
}
