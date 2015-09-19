package ie.nasouth.android.naireland;

import android.app.ProgressDialog;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MeetingMap extends FragmentActivity implements
        ClusterManager.OnClusterClickListener<MyMeetingLocation>,
        ClusterManager.OnClusterInfoWindowClickListener<MyMeetingLocation>,
        ClusterManager.OnClusterItemClickListener<MyMeetingLocation>,
        ClusterManager.OnClusterItemInfoWindowClickListener<MyMeetingLocation>
{
    private static final String TAG = "MeetingMap";

    public ProgressDialog ringProgressDialog = null;

    private ClusterManager<MyMeetingLocation> mClusterManager;
    private Cluster<MyMeetingLocation> clickedCluster;
    private MyMeetingLocation clickedClusterItem;

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
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyMeetingLocation>(this, mMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraChangeListener(mClusterManager);
        mMap.setOnMarkerClickListener(mClusterManager);
        mMap.setOnInfoWindowClickListener(mClusterManager);
        mMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());

        mClusterManager.setRenderer(new MyClusterRenderer(this, mMap, mClusterManager));
        mClusterManager.getClusterMarkerCollection().setOnInfoWindowAdapter(new MyCustomAdapterForClusters());
        mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new MyCustomAdapterForItems());
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);

        mClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<MyMeetingLocation>() {
            @Override
            public boolean onClusterClick(Cluster<MyMeetingLocation> cluster) {
                clickedCluster = cluster;
                return false;
            }
        });

        mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyMeetingLocation>() {
            @Override
            public boolean onClusterItemClick(MyMeetingLocation item) {
                clickedClusterItem = item;
                return false;
            }
        });

        mMap.setMyLocationEnabled(true);
        mMap.setBuildingsEnabled(true);

        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);

        ringProgressDialog = ProgressDialog.show(MeetingMap.this, "Please wait ...", "Downloading Meeting Info...", true);
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

        }  catch (IOException ex) {
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

            BMLTJson = getJSON("http://bmlt.nasouth.ie/main_server/client_interface/json/?switcher=GetSearchResults&data_field_key=weekday_tinyint,start_time,longitude,latitude,meeting_name,location_text,location_sub_province,location_street,location_info&sort_keys=longitude,latitude", 30000);
           // BMLTJson = getJSON("http://bmlt.nasouth.ie/main_server/client_interface/json/?switcher=GetSearchResults", 30000);

            try {

                return new JSONArray(BMLTJson);

            } catch (JSONException e) {

                Log.d(TAG, "Gone wrong there!" + e);

            }
        return null;
        }

        protected void onPostExecute(JSONArray BMLTResults) {
            double meetingLongitude;
            double meetingLatitude;
            String meetingName;
            String meetingLocation;
            String meetingStreet;
            String meetingCounty;
            String meetingInfo;
            int meetingDay;
            String meetingStart;
            ArrayList<MyMeetingLocation> meetingLocations = new ArrayList<>();
            float [] dist = new float[1];

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


                    MyMeetingLocation addMeeting = new MyMeetingLocation(meetingLatitude, meetingLongitude);
                    addMeeting.setMeetingName(meetingName);
                    addMeeting.setMeetingAddress((meetingLocation + "\n"
                                                    + meetingStreet + "\n"
                                                    + "Co. " + meetingCounty + "\n"
                                                    + meetingInfo));

                    if (meetingLocations.size() == 0) {
                        // First meeting, so just add it
                        meetingLocations.add(addMeeting);
                        // Add the time of the meeting too!
                        meetingLocations.get(0).addMeetingTime(meetingDay, meetingStart);
                    } else {
                        Location.distanceBetween(meetingLatitude,
                                meetingLongitude,
                                meetingLocations.get(meetingLocations.size() - 1).getLat(),
                                meetingLocations.get(meetingLocations.size() - 1).getLong(),
                                dist );
                        Log.d(TAG, "testing what is dist  " + dist[0]);
                        // If dist[0] is greater than 20 meters then this meeting and the last one are more than 20 meters apart, so they are
                        // in seperate locations! We only need to check against the last location because
                        // we got the list from the BMLT sorted by lat and long!
                        if (dist[0] >= 20) {
                            meetingLocations.add(addMeeting);
                        }
                        meetingLocations.get(meetingLocations.size() - 1).addMeetingTime(meetingDay, meetingStart);
                    }


                } catch (JSONException e) {
                    Log.d(TAG, "Gone wrong here!" + e);
                }
            }

            // Now loop through the list of meeting locations, adding them to the map.
            for (MyMeetingLocation mapMeetingLocation: meetingLocations) {
                mClusterManager.addItem(mapMeetingLocation);
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 8));

            ringProgressDialog.dismiss();
        }

    }


    class MyClusterRenderer extends DefaultClusterRenderer<MyMeetingLocation> {

        public MyClusterRenderer(Context context, GoogleMap map, ClusterManager<MyMeetingLocation> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(MyMeetingLocation item, MarkerOptions markerOptions) {
            super.onBeforeClusterItemRendered(item, markerOptions);
        }

        @Override
        protected void onClusterItemRendered(MyMeetingLocation clusterItem, Marker marker) {
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
            TextView tvTitle   = ((TextView) myContentsView.findViewById(R.id.title));
            TextView tvSnippet = ((TextView) myContentsView.findViewById(R.id.snippet));

            if (clickedClusterItem != null) {
                tvTitle.setText(clickedClusterItem.getMeetingName());
                tvSnippet.setText(clickedClusterItem.getlocationMeetingTimes());

            }
            return myContentsView;
        }
    }
    // class for Main Clusters.

    public class MyCustomAdapterForClusters implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyCustomAdapterForClusters() {
            myContentsView = getLayoutInflater().inflate(R.layout.popup, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            TextView tvTitle   = ((TextView) myContentsView.findViewById(R.id.title));
            TextView tvSnippet = ((TextView) myContentsView.findViewById(R.id.snippet));
            tvSnippet.setVisibility(View.GONE);

            if (clickedCluster != null) {
                tvTitle.setText("Zoom in for " + String.valueOf(clickedCluster.getItems().size())
                        + "\nmore meeting locations.");
            }
            return myContentsView;
        }
    }


    @Override
    public void onClusterItemInfoWindowClick(MyMeetingLocation item) {

    }

    @Override
    public boolean onClusterItemClick(MyMeetingLocation item) {
        return false;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<MyMeetingLocation> cluster) {

    }

    @Override
    public boolean onClusterClick(Cluster<MyMeetingLocation> cluster) {
        return false;
    }
}

