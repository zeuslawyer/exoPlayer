/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
* limitations under the License.
 */
package com.example.exoplayer;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashChunkSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity {

  // bandwidth meter to measure and estimate bandwidth
  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

  private SimpleExoPlayer player;
  private SimpleExoPlayerView playerView;

  private long playbackPosition;
  private int currentWindow;
  private boolean playWhenReady = true;

  // https://codelabs.developers.google.com/codelabs/exoplayer-intro/#5
  private ComponentListener componentListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_player);

    playerView = findViewById(R.id.video_view);

    componentListener = new ComponentListener;
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    hideSystemUi();
    if ((Util.SDK_INT <= 23 || player == null)) {
      initializePlayer();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
  }

  private void initializePlayer() {
    if (player == null) {
      // a factory to create an AdaptiveVideoTrackSelection
      TrackSelection.Factory adaptiveTrackSelectionFactory =
          new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
      // let the factory create a player instance with default components
      player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this),
          new DefaultTrackSelector(adaptiveTrackSelectionFactory), new DefaultLoadControl());

      playerView.setPlayer(player);
      player.setPlayWhenReady(playWhenReady);
      player.seekTo(currentWindow, playbackPosition);
      player.addListener(componentListener); //// https://codelabs.developers.google.com/codelabs/exoplayer-intro/#5
      player.addAudioDebugListener((AudioRendererEventListener) componentListener);  // https://codelabs.developers.google.com/codelabs/exoplayer-intro/#5
      player.addVideoDebugListener((VideoRendererEventListener) componentListener);
    }
    MediaSource mediaSource = buildMediaSource(Uri.parse(getString(R.string.media_url_dash)));
    player.prepare(mediaSource, true, false);
  }

  private void releasePlayer() {
    if (player != null) {
      playbackPosition = player.getCurrentPosition();
      currentWindow = player.getCurrentWindowIndex();
      playWhenReady = player.getPlayWhenReady();
      player.removeListener(componentListener);
      player.removeAudioDebugListener((AudioRendererEventListener) componentListener);
      player.removeVideoDebugListener((VideoRendererEventListener) componentListener);
      player.release();
      player = null;
    }
  }

  private MediaSource buildMediaSource(Uri uri) {
    DashChunkSource.Factory dashChunkSourceFactory = new DefaultDashChunkSource.Factory(
        new DefaultHttpDataSourceFactory("ua", BANDWIDTH_METER));
    DataSource.Factory manifestDataSourceFactory = new DefaultHttpDataSourceFactory("ua");
    return new DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory).
        createMediaSource(uri);
  }

  @SuppressLint("InlinedApi")
  private void hideSystemUi() {
    playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
  }

  private class ComponentListener extends Player.DefaultEventListener {
      private static final String TAG = "ComponentListener";

      @Override
      public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
          String stateString;
          switch (playbackState) {
              //case ExoPlayer.STATE_IDLE:   - deprecated
              case Player.STATE_IDLE:
                  stateString = "Player.STATE_IDLE      -";
                  break;
              case Player.STATE_BUFFERING:
                  stateString = "Player.STATE_BUFFERING -";
                  break;
              case Player.STATE_READY:
                  stateString = "Player.STATE_READY     -";
                  break;
              case Player.STATE_ENDED:
                  stateString = "Player.STATE_ENDED     -";
                  break;
              default:
                  stateString = "UNKNOWN_STATE             -";
                  break;
          }
          Log.d(TAG, "changed state to " + stateString
                  + " playWhenReady: " + playWhenReady);
      }
  }
}
