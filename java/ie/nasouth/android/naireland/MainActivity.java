package ie.nasouth.android.naireland;


import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
//import android.content.Intent;
//import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.GeolocationPermissions;

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
public class MainActivity extends ActionBarActivity {

    private WebView webView;

    // The URL for the BMLT smart phone emulator web page:
    private final String url = "http://android.nasouth.ie";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);

        // Needed settings, that are false be default:
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);

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

                // Override everything but the google maps, which will open up the
                // map app on the phone:
                if (Uri.parse(url).getHost().equals("maps.google.com")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;

            }

        });

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

        // Can do this below once bug is fixed:
        if(webView.canGoBack()){
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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
