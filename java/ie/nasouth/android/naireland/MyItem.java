package ie.nasouth.android.naireland;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * An instance of this class stores everything about an individual meeting that we need to
 * draw a marker withan infowindow on the map
 */
public class MyItem implements ClusterItem {
    private final LatLng mPosition;
    public String meetingName;
    public String meetingDetails;

    public MyItem(double lat, double lng) {

        mPosition = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public String getMeetingName() {
        return meetingName;
    }

    public LatLng getMeetingOverlapIndex() { return mPosition; }

    public void setMeetingName(String meetingName) {
        this.meetingName = meetingName;
    }

    public String getMeetingDetails() {
        return meetingDetails;
    }

    public void setMeetingDetails(String meetingDetails) {
        this.meetingDetails = meetingDetails;
    }

    public void addColocatedMeetingDetails(String colocatedMeetingDetails) {
        this.meetingDetails = this.meetingDetails + "\n\n-------------------------------\n\n" + colocatedMeetingDetails;
    }

}
