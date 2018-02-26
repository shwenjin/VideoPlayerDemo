package com.ecache.videodemo;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.wj.videolib.ScreenSwitchUtils;
import com.wj.videolib.VideoView;

public class MainActivity extends Activity {
    private VideoView mVideoView;
    private ScreenSwitchUtils mScreenSwitchUtils;
    private int mDesHeight;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVideoView=findViewById(R.id.videoView);
        mScreenSwitchUtils = ScreenSwitchUtils.init(this.getApplicationContext());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mDesHeight =  displayMetrics.widthPixels * 9 / 16;
        RelativeLayout.LayoutParams layoutParams= (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
        layoutParams.width=RelativeLayout.LayoutParams.MATCH_PARENT;
        layoutParams.height= (int)mDesHeight;
        mVideoView.setLayoutParams(layoutParams);
        mVideoView.setVideoUrl("http://obor.ecache.cn:80/Uplode/Course/Data/4d49f61889384f04959d8dce55438451.mp4",0);
        mVideoView.setOnVideoViewListener(new VideoView.OnVideoViewListener() {
            @Override
            public void onCompletionListener() {
                //播放完成
            }

            @Override
            public void onEntireScreenListener() {
                //横竖屏切换
                mScreenSwitchUtils.toggleScreen();
            }

            @Override
            public void onStart() {
                //启动横竖屏切换
                mScreenSwitchUtils.start(MainActivity.this);
            }

            @Override
            public void onPrepared() {
                //初始化缓冲完成
            }
        });
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSwitchUtils.isPortrait()) {
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            RelativeLayout.LayoutParams layoutParams= (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
            layoutParams.width=RelativeLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height= (int)mDesHeight;
            mVideoView.setLayoutParams(layoutParams);
            mVideoView.requestLayout();
        } else  {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            RelativeLayout.LayoutParams layoutParams= (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
            layoutParams.width=RelativeLayout.LayoutParams.MATCH_PARENT;
            layoutParams.height= RelativeLayout.LayoutParams.MATCH_PARENT;
            mVideoView.setLayoutParams(layoutParams);
            mVideoView.requestLayout();
        }
    }

    @Override
    protected void onPause() {
        mVideoView.setStates(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mVideoView.onDestroyed();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.setStates(true);
    }
}
