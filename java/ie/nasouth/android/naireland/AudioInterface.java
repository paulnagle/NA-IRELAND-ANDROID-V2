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

public class AudioInterface {
    Context mContext;
    MediaPlayer mediaPlayer;

    AudioInterface(Context c) {
        mContext = c;
    }

    //Play an audio file from the webpage
    @JavascriptInterface
    public void playAudio(String url) { //String url - file name passed
        //from the JavaScript function

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepare(); // might take long! (for buffering, etc)
        } catch  (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.start();

    }

    public void stopAudio() {

        mediaPlayer.stop();
    }
}
