package ie.nasouth.android.naireland;

/**
 * Created by paulnagle on 05/09/2015.
 */
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class AudioInterface {

    Context     mContext;
    MediaPlayer mediaPlayer         = new MediaPlayer();
    boolean     isSomethingPlaying  = false;
    boolean     isSomethingPaused   = false;
    int         length              = 0;
    String      currentUrl          = "";


    AudioInterface(Context c) {
        mContext = c;
    }


    //Play an audio file from the webpage
    @JavascriptInterface
    public int playAudio(String url) { //String url - file name passed from the JavaScript function

        // if we are working with a brand new file, stop any playing audio,
        // un-pause everything, and start from scratch with the new file
        //
        // i.e. someone clicked play on a new track while an old track was playing
        if ((currentUrl != url) && (currentUrl != "")){
            stopAudio();
            isSomethingPaused = false;
            isSomethingPlaying = false;
            currentUrl = url;
        }

        if (isSomethingPlaying) {  //
            if (!isSomethingPaused) {
                stopAudio();
                isSomethingPlaying = false;
                Toast.makeText(mContext, "Playing new track", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "Playing", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "Un-pausing at " + getDurationBreakdown(length), Toast.LENGTH_SHORT).show();
            isSomethingPaused = false;
        }

        isSomethingPlaying = true;
        int totalDuration = mediaPlayer.getDuration();
        Toast.makeText(mContext, "Total track length is " + getDurationBreakdown(totalDuration), Toast.LENGTH_SHORT).show();
        return totalDuration;
    }

    @JavascriptInterface
    public void pauseAudio() {
        Toast.makeText(mContext, "Pause", Toast.LENGTH_SHORT).show();

        if (isSomethingPaused) {        // Here someone has clicked on the pause button a second time
            mediaPlayer.start();        // Restart the audio
            isSomethingPaused = false;  // Set the paused flag to false

        } else {                        // Here someone has clicked on the pause button for the first time
            length = mediaPlayer.getCurrentPosition();  // Store the current position?
            mediaPlayer.pause();
            isSomethingPaused = true;
            Toast.makeText(mContext, "Pausing at " + getDurationBreakdown(length) + ". Press pause again to resume.", Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public void stopAudio() {
        Toast.makeText(mContext, "Stop", Toast.LENGTH_SHORT).show();
        mediaPlayer.stop();
        mediaPlayer.release();
        isSomethingPlaying = false;
    }

    @JavascriptInterface
    public void silentStop() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            isSomethingPlaying = false;
            isSomethingPaused = false;
        }
    }
    /**
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @return A string of the form " X Minutes Y Seconds".
     */
    public static String getDurationBreakdown(long millis)
    {
        if(millis < 0)
        {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);

        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");

        return(sb.toString());
    }

    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
    }
}
