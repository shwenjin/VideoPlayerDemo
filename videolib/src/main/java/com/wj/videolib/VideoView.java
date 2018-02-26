package com.wj.videolib;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * Created by wenjin on 2018/2/26.
 */

public class VideoView extends FrameLayout implements View.OnClickListener{
    private FrameLayout frameLayout;
    private LinearLayout mLinearBar;
    private ImageView mImagePlay;
    private TextView mTimeCurrent;
    private SeekBar mSeekBar;
    private TextView mTxtMaxTime;
    private ImageView mEntireScreen;
    private LinearLayout mLinearNetworkFast;
    private ImageView btn_play;
    private Disposable mDisposable;
    private SurfaceView mSurfaceView;
    private Context mContext;
    private boolean isDestroyed=false;
    private MediaPlayer mMediaPlayer;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private String url="";
    private int mMaxTime=0;
    private boolean isPlaying;
    private int mHistoryPosition=0;
    private int mUserTime=0;
    private int startPress=0;
    private int maxPress=0;
    private boolean states=true;
    private int mPausePosition=-1;
    private boolean isShowBar=true;
    private AudioManager mAudioMgr;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = null;
    private OnVideoViewListener mOnVideoViewListener;
    public VideoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context){
        View.inflate(context, R.layout.view_video_layout,this);
        this.mContext=context;
        mLinearNetworkFast=findViewById(R.id.linearNetworkFast);
        mEntireScreen=findViewById(R.id.img_entire_screen);
        frameLayout=findViewById(R.id.frame_content);
        mLinearBar=findViewById(R.id.linear_bar);
        mImagePlay=findViewById(R.id.btn_play);
        mTimeCurrent=findViewById(R.id.time_current);
        mSeekBar=findViewById(R.id.seekBar);
        mTxtMaxTime=findViewById(R.id.time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        mImagePlay.setOnClickListener(this);
        mEntireScreen.setOnClickListener(this);
        frameLayout.setOnClickListener(this);
    }

    public void setVideoUrl(String url,int position){
        this.url=url;
        this.mHistoryPosition=position;
        mLinearNetworkFast.setVisibility(VISIBLE);
        frameLayout.removeAllViews();
        mSurfaceView =new SurfaceView(mContext);
        mSurfaceView.getHolder().addCallback(callback);
        frameLayout.addView(mSurfaceView);
        mSeekBar.setOnSeekBarChangeListener(change);
        requestAudioFocus();
    }

    public void setOnVideoViewListener(OnVideoViewListener onVideoViewListener){
        this.mOnVideoViewListener=onVideoViewListener;
    }

    /**
     * 获取进度条的时间
     * @return
     */
    public int getUserTime(){
        return mUserTime;
    }

    /**
     * 获取滑动的总时间
     * @return
     */
    public int getMaxPress(){
        return maxPress;
    }

    /**
     * 获取实际观看时间
     * @return
     */
    public int getReadingTime(){
        return mUserTime-maxPress;
    }

    /**
     * 获取视频的总时间
     */
    public int getDuration(){
        return mMaxTime;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.img_entire_screen) {
            mOnVideoViewListener.onEntireScreenListener();
        } else if (i == R.id.btn_play) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mImagePlay.setImageResource(R.drawable.wj_play);
                mMediaPlayer.pause();
            } else {
                mImagePlay.setImageResource(R.drawable.wj_pause);
                mMediaPlayer.start();
            }
        } else if (i==R.id.frame_content){
            //隐藏底部控制栏
            if(isShowBar){
                outAnimation();
            }else{
                inAnimation();
            }
        }
    }

    public void onPause(){
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mImagePlay.setImageResource(R.drawable.wj_play);
            isPlaying=false;
            mMediaPlayer.pause();
        }
    }

    public void onStart(){
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mImagePlay.setImageResource(R.drawable.wj_pause);
            isPlaying=true;
            mMediaPlayer.start();
//            mSeekBar.setOnSeekBarChangeListener(change);
            requestAudioFocus();
        }
    }

    public void setStates(boolean states){
        Log.d("tag","setStates->"+states);
        if(states){
            if(mPausePosition!=-1&&!TextUtils.isEmpty(url)){
                this.mHistoryPosition=mPausePosition;
                if(mMediaPlayer==null){
                    setVideoUrl(url,mPausePosition);
                }else{
                    onStart();
                }
                mPausePosition=-1;
            }
        }else{
            if(mMediaPlayer!=null){
                mPausePosition=mMediaPlayer.getCurrentPosition();
            }
            onPause();
        }
        this.states=states;
    }

    /**
     * 开始播放
     *
     * @param msec 播放初始位置
     */
    private void play(final int msec) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // 设置显示视频的SurfaceHolder
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            // 设置播放的视频源
            if(url.startsWith("http")){
                Uri uri=Uri.parse(url);
                mMediaPlayer.setDataSource(mContext,uri);
            }else{
                mMediaPlayer.setDataSource(url);
            }
            Log.d("tag","开始装载");
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.d("tag","装载完成");
                    mLinearNetworkFast.setVisibility(GONE);
                    mOnVideoViewListener.onPrepared();
                    mMediaPlayer.start();
                    // 按照初始位置播放
                    mMediaPlayer.seekTo(msec);
                    // 设置进度条的最大进度为视频流的最大播放时长
                    mMaxTime=mMediaPlayer.getDuration();
                    mSeekBar.setMax(mMediaPlayer.getDuration());
                    // 开始线程，更新进度条的刻度
                    isPlaying = true;
                    mDisposable = Observable.interval(0,500, TimeUnit.MILLISECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .filter(new Predicate<Long>() {
                                @Override
                                public boolean test(Long aLong) throws Exception {
                                    return isPlaying&&states;
                                }
                            })
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) throws Exception {
                                    if (mMediaPlayer!=null&&mMediaPlayer.isPlaying()&&isPlaying){
                                        int current = mMediaPlayer.getCurrentPosition();
                                        mUserTime=current;
                                        mSeekBar.setProgress(current);
//                                        Log.d("tag","mSeekBar->"+current);
                                    }
                                }
                            });
                    mImagePlay.setImageResource(R.drawable.wj_pause);
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 在播放完毕被回调
//                    btn_play.setEnabled(true);
                    //backFinish();
                    Log.d("tag","播放完成");
                    mImagePlay.setImageResource(R.drawable.wj_play);
                    mTimeCurrent.setText(stringForTime(mMaxTime));
                    mOnVideoViewListener.onCompletionListener();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {

                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    // 发生错误重新播放
                    mMediaPlayer.reset();
                    play(mHistoryPosition);
                    Log.d("tag","发生错误重新播放");
                    isPlaying = false;
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        // SurfaceHolder被修改的时候回调
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // 销毁SurfaceHolder的时候记录当前的播放位置并停止播放
            Log.d("tag","surfaceDestroyed");
            isDestroyed=true;
            if(mMediaPlayer!=null){
                if(mMediaPlayer.isPlaying()){
                    mMediaPlayer.stop();
                }
                mMediaPlayer.release();
                mMediaPlayer=null;
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("tag","surfaceCreated");
            isDestroyed=false;
            mOnVideoViewListener.onStart();
            play(mHistoryPosition);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("tag","surfaceChanged");
        }
    };

    private SeekBar.OnSeekBarChangeListener change = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // 当进度条停止修改的时候触发
            // 取得当前进度条的刻度
            int progress = seekBar.getProgress();
            if (mMediaPlayer != null ) {
                if(!mMediaPlayer.isPlaying()){
                    mImagePlay.setImageResource(R.drawable.wj_pause);
                    mMediaPlayer.start();
                }
                // 设置当前播放的位置
                mMediaPlayer.seekTo(progress);
                mUserTime=progress;
                if(progress>startPress){
                    maxPress+=progress-startPress;
                }else{
                    maxPress-=startPress-progress;
                }
                if(!isDestroyed){
                    try{
                        if(mMediaPlayer!=null&&mMediaPlayer.isPlaying()){
                            mTimeCurrent.setText(stringForTime(progress));
                            mTxtMaxTime.setText(stringForTime(mMaxTime));
                        }
                    }catch (Exception e){
                        Log.d("",e.toString());
                    }
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            startPress=seekBar.getProgress();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            if(!isDestroyed){
                try{
                    if(mMediaPlayer!=null&&mMediaPlayer.isPlaying()){
                        mTimeCurrent.setText(stringForTime(progress));
                        mTxtMaxTime.setText(stringForTime(mMaxTime));
                    }
                }catch (Exception e){
                    Log.d("",e.toString());
                }
            }
        }
    };

    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /*
 * 停止播放
 */
    public void stop() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            isPlaying = false;
        }
    }

    /**
     * 重新开始播放
     */
    public void replay() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(0);
            return;
        }
        isPlaying = false;
        play(0);
    }

    public void back(){
        if(mDisposable!=null&&!mDisposable.isDisposed()){
            mDisposable.dispose();
        }
        isPlaying=false;
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if(frameLayout!=null){
            frameLayout.removeAllViews();
        }
        mSurfaceView=null;
    }

    private void requestAudioFocus() {
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ECLAIR_MR1){
            return;
        }
        if (mAudioMgr == null)
            mAudioMgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        //Build.VERSION.SDK_INT表示当前SDK的版本，Build.VERSION_CODES.ECLAIR_MR1为SDK 7版本 ，
        //因为AudioManager.OnAudioFocusChangeListener在SDK8版本开始才有。
        mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if(focusChange == AudioManager.AUDIOFOCUS_LOSS){
                    //失去焦点之后的操作
                }else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){
                    //获得焦点之后的操作
                }
            }
        };
        if (mAudioMgr != null) {
            Log.d("tag", "Request audio focus");
            int ret = mAudioMgr.requestAudioFocus(mAudioFocusChangeListener,AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (ret != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d("tag", "request audio focus fail. " + ret);
            }
        }
    }

    public void onDestroyed(){
        back();
    }

    private void inAnimation(){
        mLinearBar.clearAnimation();
        isShowBar=true;
        AlphaAnimation animation=new AlphaAnimation(0,1);
        animation.setDuration(300);
        mLinearBar.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mLinearBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLinearBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private void outAnimation(){
        mLinearBar.clearAnimation();
        isShowBar=false;
        AlphaAnimation animation=new AlphaAnimation(1,0);
        animation.setDuration(300);
        mLinearBar.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mLinearBar.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    public interface OnVideoViewListener{
        public void onCompletionListener();
        public void onEntireScreenListener();
        public void onStart();
        public void onPrepared();
    }
}