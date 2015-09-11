package ie.nasouth.android.naireland;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MeetingMap extends FragmentActivity {
    private static final String TAG = "MeetingMap";
    public ProgressDialog ringProgressDialog = null;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_map);

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        LatLng center = new LatLng(53.341318, -6.270205);

        mMap.setMyLocationEnabled(true);
        mMap.setBuildingsEnabled(true);

        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 7));

        ringProgressDialog = ProgressDialog.show(MeetingMap.this, "Please wait ...", "Downloading Meetings...", true);
        ringProgressDialog.setCancelable(true);

        new retrieveAndAddMeetings().execute();
    }


    public String getJSON(String url, int timeout) {
        HttpURLConnection c = null;
        try {
            URL u = new URL(url);
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("Content-length", "0");
            c.setUseCaches(false);
            c.setAllowUserInteraction(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.connect();
            int status = c.getResponseCode();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    return sb.toString();
            }

        } catch (MalformedURLException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return null;
    }


    class retrieveAndAddMeetings extends AsyncTask< Void, Void, JSONArray > {
        JSONObject meeting;

        protected JSONArray doInBackground(Void... args) {
            List params = new ArrayList();
            // getting JSON string from URL

            final String BMLTJson;
            JSONObject meeting;

            BMLTJson = getJSON("http://bmlt.nasouth.ie/main_server/client_interface/json/?switcher=GetSearchResults", 30000);

            try {

                return new JSONArray(BMLTJson);

            } catch (JSONException e) {

                Log.d(TAG, "Gone wrong there!" + e);

            }
        return null;
        }

        protected void onPostExecute(JSONArray BMLTResults) {
            Double meetingLongitude;
            Double meetingLatitude;
            String meetingName;
            String meetingLocation;
            String meetingStreet;
            String meetingCounty;
            int meetingDay;
            String meetingStart;
            String meetingDayString = null;

            LatLng center = new LatLng(53.341318, -6.270205);

            mMap.addMarker(new MarkerOptions().position(center).title("NA Ireland Service Office"));

            for (int i=0; i < BMLTResults.length(); i++) {
                try {
                    meeting = BMLTResults.getJSONObject(i);

                    meetingLongitude = meeting.getDouble("longitude");
                    meetingLatitude  = meeting.getDouble("latitude");
                    meetingName      = meeting.getString("meeting_name");
                    meetingLocation  = meeting.getString("location_text");
                    meetingStreet    = meeting.getString("location_street");
                    meetingCounty    = meeting.getString("location_sub_province");
                    meetingDay       = meeting.getInt("weekday_tinyint");
                    meetingStart     = meeting.getString("start_time");

                    switch (meetingDay) {
                        case 0:
                            meetingDayString = "Sunday";
                            break;
                        case 1:
                            meetingDayString = "Monday";
                            break;
                        case 2:
                            meetingDayString = "Tuesday";
                            break;
                        case 3:
                            meetingDayString = "Wednesday";
                            break;
                        case 4:
                            meetingDayString = "Thursday";
                            break;
                        case 5:
                            meetingDayString = "Friday";
                            break;
                        case 6:
                            meetingDayString = "Saturday";
                            break;
                    }

 //                   Log.d(TAG, meetingLongitude + " " + meetingLatitude + " " + meetingName + " " + meetingLocation + " " + meetingStreet + " " + meetingCounty);

                    mMap.addMarker(new MarkerOptions().position(new LatLng(meetingLatitude, meetingLongitude))
                                                        .title(meetingName)
                                                        .snippet(meetingStart.substring(0, 5) + "  " + meetingDayString + " \n" + meetingLocation + "\n" + meetingStreet + "\n Co. " + meetingCounty));


                } catch (JSONException e) {
                    Log.d(TAG, "Gone wrong here!" + e);
                }
            }
            mMap.setInfoWindowAdapter(new PopupAdapter(getLayoutInflater()));
            ringProgressDialog.dismiss();
        }
    }
}
