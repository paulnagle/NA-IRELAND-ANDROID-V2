package ie.nasouth.android.naireland;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.media.MediaPlayer.OnPreparedListener;
import android.view.MotionEvent;
import android.widget.MediaController;
import android.widget.TextView;

import java.io.IOException;
import java.util.HashMap;

public class AudioPlayer extends Activity implements OnPreparedListener, MediaController.MediaPlayerControl{
    private static final String TAG = "AudioPlayer";

    public ProgressDialog ringProgressDialog = null;

    private MediaPlayer mediaPlayer;
    private MediaController mediaController;
    private String audioFile;
    private int pausedPosition = 0;
    private MediaMetadataRetriever retreiver = new MediaMetadataRetriever();

    private Handler handler = new Handler();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        Bundle b = getIntent().getExtras();
        audioFile = b.getString("AUDIO_FILE_NAME");
        ((TextView)findViewById(R.id.now_playing_text)).setText("Loading ....");

        ringProgressDialog = ProgressDialog.show(AudioPlayer.this, "Please wait ...", "Downloading speaker mp3...", true);
        ringProgressDialog.setCancelable(true);

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);

        mediaController = new MediaController(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            Log.d(TAG, " Audion filename = " + audioFile);
            mediaPlayer.setDataSource(audioFile);
            retreiver.setDataSource(audioFile, new HashMap<String, String>());
            mediaPlayer.prepareAsync();
  //          mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Could not open file " + audioFile + " for playback.", e);
        }
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, " onStop ");
        super.onStop();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mediaController.show(0);
        return false;
    }

    //--MediaPlayerControl methods----------------------------------------------------
    public void start() {
        mediaPlayer.start();
    }

    public void pause() {
        mediaPlayer.pause();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public void seekTo(int i) {
        mediaPlayer.seekTo(i);
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public int getBufferPercentage() {
        return 0;
    }

    public boolean canPause() {
        return true;
    }

    public boolean canSeekBackward() {
        return true;
    }

    public boolean canSeekForward() {
        return true;
    }
    //--------------------------------------------------------------------------------

    public void onPrepared(final MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared");
        mediaController.setMediaPlayer(this);
        mediaController.setAnchorView(findViewById(R.id.main_audio_view));
        mediaController.setKeepScreenOn(true);

        handler.post(new Runnable() {
            public void run() {
                //((TextView) findViewById(R.id.now_playing_text)).setText(audioFile);
                ringProgressDialog.dismiss();
                ((TextView) findViewById(R.id.now_playing_text)).setText(retreiver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                mediaPlayer.start();
                mediaController.setEnabled(true);
                mediaController.show(0);
            }
        });
    }

    @Override
    public void onPause() {
        Log.d(TAG, " onPause ");
        super.onPause();  // Always call the superclass method first

        mediaPlayer.pause();
        pausedPosition = mediaPlayer.getCurrentPosition();
        Log.d(TAG, " onPause : pausedPosition = " + pausedPosition);
    }

    @Override
    public void onResume() {
        Log.d(TAG, " onResume ");
        super.onResume();  // Always call the superclass method first

        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.seekTo(pausedPosition);
            mediaController.show(0);
        }
    }
}
