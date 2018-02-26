# SimpleVideoPlayerLib
    简单的视频播放器
##### 1、添加build.gradle 

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
##### 2、添加dependency

```
dependencies {
    compile 'com.github.shwenjin:SimpleVideoPlayerLib:V1.0'
}
```
##### 3、在XML布局中添加自定义View

```
<com.wj.videolib.VideoView
    android:id="@+id/videoView"
    android:layout_centerInParent="true"
    android:layout_width="match_parent"
    android:layout_height="180dp">
</com.wj.videolib.VideoView>
```
##### 4、调用

1. 执行
    ```
    DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int mDesHeight =  displayMetrics.widthPixels * 9 / 16;
    RelativeLayout.LayoutParams layoutParams= (RelativeLayout.LayoutParams) mVideoView.getLayoutParams();
    layoutParams.width=RelativeLayout.LayoutParams.MATCH_PARENT;
    layoutParams.height= (int)mDesHeight;
    mVideoView.setLayoutParams(layoutParams);
    mVideoView.setVideoUrl("xxx.mp4",0);
    ```
2. 生命周期
    
    ```
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
    ```
3. 横竖屏切换
    
    ```
    private ScreenSwitchUtils instance;
    
    instance = ScreenSwitchUtils.init(this.getApplicationContext());
    
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
            
    @Override
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
        
    <activity
        android:name=".MainActivity"
        android:theme="@android:style/Theme.Black.NoTitleBar"
        android:screenOrientation="portrait"
        android:configChanges="screenSize|keyboardHidden|orientation" />
    ```
##### 5、权限

```
<uses-permission android:name="android.permission.INTERNET" />
```

