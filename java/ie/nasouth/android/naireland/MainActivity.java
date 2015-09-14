package ie.nasouth.android.naireland;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;
import android.widget.Toast;




public class MainActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // This is the starting html page for the app
        final String url = "file:///android_asset/index.html";

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.activity_main_webview);

        // Needed settings, that are false by default:
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setAllowUniversalAccessFromFileURLs(true);

        // Allows current location access:
        webView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        // Forces web view not to re-direct to a browser when
        // clicking on a link, except for google maps, or any of the
        // area/region websites.
        // Also this forces tel: links to open the android dialer,
        // mailto: links to open the email app, mp3 files to be
        // opened by this apps media player, and meetingmap:
        // links to be opened by this apps google maps activity

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                // Redirect all tel: links to the dialler
                if (url.startsWith("tel:")) {
                    Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(tel);
                    return true;
                }

                // Redirect all mailto: links to the email app
                if (url.startsWith("mailto:")) {
                    Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(i);
                    return true;
                }

                // The mp3 files are currently located on www.nasouth.ie, so
                // handle mp3 extension files before redirecting the area/region
                // websites.
                if (url.endsWith(".mp3")) {
                    Intent intent = new Intent(MainActivity.this, AudioPlayer.class);
                    Bundle b = new Bundle();
                    b.putString("AUDIO_FILE_NAME", url);
                    intent.putExtras(b);
                    startActivity(intent);
                    return true;

                }

                // Redirect all meetingmap: links to the google maps activity MeetingMap.class
                if (url.startsWith("meetingmap:")) {
                    Intent MeetingsMapIntent = new Intent(MainActivity.this, MeetingMap.class);
                    startActivity(MeetingsMapIntent);
                    return true;
                }

                // Redirect all of the external links to the system web browser
                if (    (Uri.parse(url).getHost().equals("maps.google.com")) ||
                        (Uri.parse(url).getHost().equals("www.na-ireland.org")) ||
                        (Uri.parse(url).getHost().equals("www.naeasternarea.org")) ||
                        (Uri.parse(url).getHost().equals("www.nanorthernireland.com")) ||
                        (Uri.parse(url).getHost().equals("www.nasouth.ie")) ||
                        (Uri.parse(url).getHost().equals("github.com"))) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }

                return false;
            }
        });


        // As this app relies heavily on having a working connection to the internet, we will
        // display a message if no internet connection is available
        ConnectivityManager cm =  (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());

        if (!isConnected) {
            Toast.makeText(getApplicationContext(), "No Internet access detected!! This app relies on having a working internet connection.", Toast.LENGTH_LONG).show();
        }

        // Load the web-page:
        webView.loadUrl(url);
    }

    /**
     * Overriding this so that the back button reloads the URL.
     * This for Android versions earlier than 2.0
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0
                && webView.canGoBack()) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Overriding this so that the back button reloads the URL.
     * This for Android versions newer than 2.0
     */
    @Override
    public void onBackPressed() {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform.

        if (webView.canGoBack()) {
            webView.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            MainActivity.this.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
            getParent().finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first

    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

    }

    @Override
    public void onDestroy() {
        super.onDestroy(); // Always call the superclass method first

        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.destroy();
        webView = null;
    }

}
