package com.ak93.exoplayerexample;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import java.util.Formatter;
import java.util.Locale;

public class ServiceActivity extends AppCompatActivity implements callback {
    private  SimpleExoPlayer exoPlayer;
    private  final String TAG = "MainActivity";
    private  Intent serviceIntent;
    private  SeekBar seekPlayerProgress;
    private  Handler handler;
    private ImageButton btnPlay;
    private TextView txtCurrentTime, txtEndTime;
    private  boolean isPlaying = false;

    public ExoPlayer.EventListener eventListener = new ExoPlayer.EventListener() {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            Log.i(TAG, "onTimelineChanged");
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i(TAG, "onTracksChanged");
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            Log.i(TAG, "onLoadingChanged");
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged: playWhenReady = " + String.valueOf(playWhenReady)
                    + " playbackState = " + playbackState);
            switch (playbackState) {
                case ExoPlayer.STATE_ENDED:
                    Log.i(TAG, "Playback ended!");
                    //Stop playback and return to start position
                    setPlayPause(false);
                    exoPlayer.seekTo(0);
                    break;
                case ExoPlayer.STATE_READY:
                    Log.i(TAG, "ExoPlayer ready! pos: " + exoPlayer.getCurrentPosition() + " max: " + stringForTime((int) exoPlayer.getDuration()));
                    setProgress();
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    Log.i(TAG, "Playback buffering!");
                    break;
                case ExoPlayer.STATE_IDLE:
                    Log.i(TAG, "ExoPlayer idle!");
                    break;
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.i(TAG, "onPlaybackError: " + error.getMessage());
        }

        @Override
        public void onPositionDiscontinuity() {
            Log.i(TAG, "onPositionDiscontinuity");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prepareExoPlayerFromURL(Uri.parse("http://traffic.cast.plus/58fffce4b02aad05f6e24c5f/http://feeds.soundcloud.com/stream/473244558-allegedly-final-episode.mp3"));
        startService();
        findViewById(R.id.buttons).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ServiceActivity.this,MainActivity.class));
            }
        });
    }

    public void startService() {
        serviceIntent = new Intent(this, NotificationService.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
    }

    public void stopService() {
        stopService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService();
    }

    private void prepareExoPlayerFromURL(Uri uri) {
        TrackSelector trackSelector = new DefaultTrackSelector();
        LoadControl loadControl = new DefaultLoadControl();
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "exoplayer2example"), null);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        MediaSource audioSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
        exoPlayer.addListener(eventListener);
        exoPlayer.prepare(audioSource);
        initMediaControls();
    }


    private void initMediaControls() {
        initPlayButton();
        initSeekBar();
        initTxtTime();
    }

    private void initPlayButton() {
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPlay.requestFocus();
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setPlayPause(!isPlaying);
            }
        });
    }

    /**
     * Starts or stops playback. Also takes care of the Play/Pause button toggling
     *
     * @param play True if playback should be started
     */
    public void setPlayPause(boolean play) {
        isPlaying = play;
        exoPlayer.setPlayWhenReady(play);
        if (!isPlaying) {
            stopService();
            btnPlay.setImageResource(android.R.drawable.ic_media_play);
        } else {
            startService(serviceIntent);
//            bindService(serviceIntent,serviceConnection,Context.BIND_AUTO_CREATE);
            NotificationService.setCallbacks(this);
            setProgress();
            btnPlay.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private void initTxtTime() {
        txtCurrentTime = (TextView) findViewById(R.id.time_current);
        txtEndTime = (TextView) findViewById(R.id.player_end_time);
    }

    public static String stringForTime(int timeMs) {
        StringBuilder mFormatBuilder;
        Formatter mFormatter;
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
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

    public  void setProgress() {
        seekPlayerProgress.setProgress(0);
        seekPlayerProgress.setMax((int) exoPlayer.getDuration() / 1000);
        txtCurrentTime.setText(stringForTime((int) exoPlayer.getCurrentPosition()));
        txtEndTime.setText(stringForTime((int) exoPlayer.getDuration()));
        if (handler == null) handler = new Handler();
        //Make sure you update Seekbar on UI thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (exoPlayer != null && isPlaying) {
                    seekPlayerProgress.setMax((int) exoPlayer.getDuration() / 1000);
                    int mCurrentPosition = (int) exoPlayer.getCurrentPosition() / 1000;
                    seekPlayerProgress.setProgress(mCurrentPosition);
                    txtCurrentTime.setText(stringForTime((int) exoPlayer.getCurrentPosition()));
                    txtEndTime.setText(stringForTime((int) exoPlayer.getDuration()));
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }
// public static void setProgress() {
//        seekPlayerProgress.setProgress(0);
//        seekPlayerProgress.setMax((int) exoPlayer.getDuration() / 1000);
//        txtCurrentTime.setText(stringForTime((int) exoPlayer.getCurrentPosition()));
//        txtEndTime.setText(stringForTime((int) exoPlayer.getDuration()));
//
//        if (handler == null) handler = new Handler();
//        //Make sure you update Seekbar on UI thread
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (exoPlayer != null && isPlaying) {
//                    seekPlayerProgress.setMax((int) exoPlayer.getDuration() / 1000);
//                    int mCurrentPosition = (int) exoPlayer.getCurrentPosition() / 1000;
//                    seekPlayerProgress.setProgress(mCurrentPosition);
//                    txtCurrentTime.setText(stringForTime((int) exoPlayer.getCurrentPosition()));
//                    txtEndTime.setText(stringForTime((int) exoPlayer.getDuration()));
//
//                    handler.postDelayed(this, 1000);
//                }
//            }
//        });
//    }

    private void initSeekBar() {
        seekPlayerProgress = (SeekBar) findViewById(R.id.mediacontroller_progress);
        seekPlayerProgress.requestFocus();
        seekPlayerProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }
                exoPlayer.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekPlayerProgress.setMax(0);
        seekPlayerProgress.setMax((int) exoPlayer.getDuration() / 1000);
    }

    @Override
    public void doSomething() {
//        Toast.makeText(this, "doSomething", Toast.LENGTH_SHORT).show();
        setPlayPause(false);
    }
}
