package ie.nasouth.android.naireland;

/**
 * Created by paulnagle on 05/09/2015.
 */
import java.io.IOException;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class AudioInterface {
    Context mContext;
    MediaPlayer mediaPlayer;
    boolean isSomethingPlaying;
    boolean isSomethingPaused;
    int length;

    AudioInterface(Context c) {
        mContext = c;
    }

    //Play an audio file from the webpage
    @JavascriptInterface
    public void playAudio(String url) { //String url - file name passed
        //from the JavaScript function
        if (isSomethingPlaying) {
            if (!isSomethingPaused) {
                stopAudio();
                isSomethingPlaying = false;
                Toast.makeText(mContext, "Playing new track", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mContext, "Playing", Toast.LENGTH_LONG).show();
        }


        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();
        if (isSomethingPaused){
            mediaPlayer.seekTo(length);
            Toast.makeText(mContext, "Un-pausing at " + length, Toast.LENGTH_LONG).show();
            isSomethingPaused = false;
        }

        isSomethingPlaying = true;
    }

    @JavascriptInterface
    public void pauseAudio() {
        if (isSomethingPaused) {
            unPauseAudio();
        } else {
            length = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            isSomethingPaused = true;
            Toast.makeText(mContext, "Pausing at " + length, Toast.LENGTH_LONG).show();
        }
    }

    public void unPauseAudio() {
        if (isSomethingPaused) {
            mediaPlayer.start();
        }
    }

    @JavascriptInterface
    public void stopAudio() {
        Toast.makeText(mContext, "Stopping", Toast.LENGTH_LONG).show();
        mediaPlayer.stop();
        isSomethingPlaying = false;
    }
}
