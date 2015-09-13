package ie.nasouth.android.naireland;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

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


public class MeetingMap extends FragmentActivity implements
        ClusterManager.OnClusterClickListener<MyItem>,
        ClusterManager.OnClusterInfoWindowClickListener<MyItem>,
        ClusterManager.OnClusterItemClickListener<MyItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<MyItem>
{
    private static final String TAG = "MeetingMap";

    public ProgressDialog ringProgressDialog = null;

    private ClusterManager<MyItem> mClusterManager;
    private Cluster<MyItem> clickedCluster;
    private MyItem clickedClusterItem;

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

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this, mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);

        mClusterManager = new ClusterManager<MyItem>(this, mMap);
        mMap.setOnCameraChangeListener(mClusterManager);
        mClusterManager.setRenderer(new MyClusterRenderer(this, mMap, mClusterManager));

        mMap.setOnInfoWindowClickListener(mClusterManager);
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());

        mClusterManager.getClusterMarkerCollection().setOnInfoWindowAdapter(new MyCustomAdapterForClusters());
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MyCustomAdapterForItems());

        mMap.setOnMarkerClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);

        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyItem>() {
            @Override
            public boolean onClusterClick(Cluster<MyItem> cluster) {
                clickedCluster = cluster;
                return false;
            }
        });

        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyItem>() {
            @Override
            public boolean onClusterItemClick(MyItem item) {
                clickedClusterItem = item;
                return false;
            }
        });

        mMap.setMyLocationEnabled(true);
        mMap.setBuildingsEnabled(true);

        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);



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
            final String BMLTJson;

            BMLTJson = getJSON("http://bmlt.nasouth.ie/main_server/client_interface/json/?switcher=GetSearchResults&data_field_key=weekday_tinyint,start_time,longitude,latitude,meeting_name,location_text,location_sub_province,location_street,location_info", 30000);
           // BMLTJson = getJSON("http://bmlt.nasouth.ie/main_server/client_interface/json/?switcher=GetSearchResults", 30000);

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
            String meetingInfo;
            int meetingDay;
            String meetingStart;
            String meetingDayString = null;

            LatLng center = new LatLng(53.341318, -6.270205);

            for (int i=0; i < BMLTResults.length(); i++) {
                try {
                    meeting = BMLTResults.getJSONObject(i);

                    meetingLongitude = meeting.getDouble("longitude");
                    meetingLatitude  = meeting.getDouble("latitude");
                    meetingName      = meeting.getString("meeting_name");
                    meetingLocation  = meeting.getString("location_text");
                    meetingStreet    = meeting.getString("location_street");
                    meetingCounty    = meeting.getString("location_sub_province");
                    meetingInfo      = meeting.getString("location_info");
                    meetingDay       = meeting.getInt("weekday_tinyint");
                    meetingStart     = meeting.getString("start_time");

                    switch (meetingDay) {
                        case 1:
                            meetingDayString = "Sunday";
                            break;
                        case 2:
                            meetingDayString = "Monday";
                            break;
                        case 3:
                            meetingDayString = "Tuesday";
                            break;
                        case 4:
                            meetingDayString = "Wednesday";
                            break;
                        case 5:
                            meetingDayString = "Thursday";
                            break;
                        case 6:
                            meetingDayString = "Friday";
                            break;
                        case 7:
                            meetingDayString = "Saturday";
                            break;
                    }

                    MyItem addMeeting = new MyItem(meetingLatitude, meetingLongitude);
                    addMeeting.setMeetingName(meetingName);
                    addMeeting.setMeetingDetails((meetingStart.substring(0, 5) + "  "
                            + meetingDayString + " \n"
                            + meetingLocation + "\n"
                            + meetingStreet + "\n"
                            + "Co. " + meetingCounty + "\n"
                            + meetingInfo));

                    mClusterManager.addItem(addMeeting);

                } catch (JSONException e) {
                    Log.d(TAG, "Gone wrong here!" + e);
                }
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 8));

            ringProgressDialog.dismiss();
        }

    }


    class MyClusterRenderer extends DefaultClusterRenderer<MyItem> {

        public MyClusterRenderer(Context context, GoogleMap map, ClusterManager<MyItem> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(MyItem item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
        }

        @Override
        protected void onClusterItemRendered(MyItem clusterItem, Marker marker) {
            super.onClusterItemRendered(clusterItem, marker);
        }
    }


    // Custom adapter info view :

    public class MyCustomAdapterForItems implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyCustomAdapterForItems() {
            myContentsView = getLayoutInflater().inflate(R.layout.popup, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // TODO Auto-generated method stub


            TextView tvTitle   = ((TextView) myContentsView.findViewById(R.id.title));
            TextView tvSnippet = ((TextView) myContentsView.findViewById(R.id.snippet));

            if (clickedClusterItem != null) {
                tvTitle.setText(clickedClusterItem.getMeetingName());
                tvSnippet.setText(clickedClusterItem.getMeetingDetails());

            }
            return myContentsView;
        }
    }
    // class for Main Clusters.

    public class MyCustomAdapterForClusters implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyCustomAdapterForClusters() {
            myContentsView = getLayoutInflater().inflate(
                    R.layout.popup, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // TODO Auto-generated method stub


            TextView tvTitle   = ((TextView) myContentsView.findViewById(R.id.title));
            TextView tvSnippet = ((TextView) myContentsView.findViewById(R.id.snippet));
            tvSnippet.setVisibility(View.GONE);


            if (clickedCluster != null) {
                tvTitle.setText("Zoom in for" + String.valueOf(clickedCluster.getItems().size())
                        + "\nmore meetings.");
            }
            return myContentsView;
        }
    }


    @Override
    public void onClusterItemInfoWindowClick(MyItem item) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onClusterItemClick(MyItem item) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<MyItem> cluster) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onClusterClick(Cluster<MyItem> cluster) {
        // TODO Auto-generated method stub
        return false;
    }
}

