package ie.nasouth.android.naireland;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.ActionBarActivity;
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

/**
 * Simple WebApp that creates a WebView for BMLT smart phone
 * webpage.
 *
 * Overloads URL loading for everything but google maps so
 * that it stays inside the webview, until a meeting is mapped,
 * so that redirection to the phones google map app can occur.
 *
 * Overloads the back button behavior to go back within the
 * webview history when possible.
 *
 * @author anonymous
 *
 */
public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private final String url = "file:///android_asset/index.html";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.activity_main_webview);

        // Needed settings, that are false be default:
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
        // clicking on a link, except for google maps:
        // TODO Dont override links to area websites linked from the app?
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                // Override everything but external links
                if ((Uri.parse(url).getHost().equals("maps.google.com")) ||
                    (Uri.parse(url).getHost().equals("www.na-ireland.org")) ||
                    (Uri.parse(url).getHost().equals("www.naeasternarea.org")) ||
                    (Uri.parse(url).getHost().equals("www.nanorthernireland.com")) ||
                    (Uri.parse(url).getHost().equals("github.com")) ) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                if (url.endsWith(".mp3")) {

                    Intent intent = new Intent(MainActivity.this, AudioPlayer.class);
                    Bundle b = new Bundle();
                    b.putString("AUDIO_FILE_NAME", url);
                    intent.putExtras(b);
                    startActivity(intent);
                    return true;
                }
                return false;
            }

        });

        ConnectivityManager cm =  (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());

        if (!isConnected) {
            Toast.makeText(getApplicationContext(), "No Internet access detected!!", Toast.LENGTH_LONG).show();
        }

  //      webView.addJavascriptInterface(new AudioInterface(this), "AndroidAudio");

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

        webView.loadUrl("about:blank");
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.destroy();
        webView = null;
    }

}
