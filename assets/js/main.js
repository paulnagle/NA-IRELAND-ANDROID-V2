// var DEBUG = false;
var DEBUG = true;
var map = null;
var myLatLng = new L.latLng(53.341318, -6.270205); // Irish Service Office
var circle = null;
var currentLocationMarker = null;
var searchRadius = 25;  // default to 25km
var markerClusterer = null;
var firstLoad = 1;

var audio;
var playlist;
var tracks;
var current;
var trackLength = 0;

// Extend the Default marker class
// Each one of these markers on the map represents a meeting
var NaIcon = L.Icon.Default.extend({ options: {	iconUrl: 'img/marker-na.png', shadowUrl: 'img/marker-shadow.png' } });
var naIcon = new NaIcon();

// https://www.mapbox.com/maki/
// There should only be one of these markers on the map, representing where the meeting search
// is centered.
var markerIcon = L.MakiMarkers.icon({ icon: "embassy", color: "#0a0", size: "l" });

// This function cleans up and deletes any old Maps
function deleteMap() {
	DEBUG && console.log("****Running deleteMap()***");

	if (circle) { delete circle; }
	if (currentLocationMarker) { delete currentLocationMarker; }
	if (markerClusterer) { map.removeLayer(markerClusterer); }
	if (map) { delete map; map.remove(); }

	// now delete the old Map container
	var oldMapContainer = document.getElementById("map_canvas");
	var mapContainerParent = oldMapContainer.parentNode;
	mapContainerParent.removeChild(oldMapContainer);

	// recreate this <div id="map_canvas" style="width: 100%"></div>
	var newMapContainer = document.createElement('div');
	newMapContainer.setAttribute("id", "map_canvas");
	newMapContainer.style.cssText = 'width: 100%';
	mapContainerParent.appendChild(newMapContainer);

	var headerHeight = document.getElementById('map-header').clientHeight;
	var footerHeight = document.getElementById('map-footer').clientHeight;
	var newHeight = window.innerHeight - (headerHeight + footerHeight + 4);
	document.getElementById("map_canvas").style.height = newHeight + "px";

	var $mapSwitch  = $( "#map-switch"  ),
        $listSwitch = $( "#list-switch" ),
        $map_div    = $( "#map_canvas"  ),
        $list_div   = $( "#list_canvas" );
	$list_div.hide();
	$map_div.show();

    $mapSwitch.on( "click", function( e ){
        $map_div.show();
        $list_div.hide();
    });

    $listSwitch.on( "click", function( e ){
        $list_div.show();
        $map_div.hide();
    });
}

// This function creates a new map, adds the Circle, the current location marker and
// then runs a new search.
function newMap() {
	DEBUG && console.log("Running newMap()");

	var headerHeight = document.getElementById('map-header').clientHeight;
	var footerHeight = document.getElementById('map-footer').clientHeight;
	var newHeight = window.innerHeight - (headerHeight + footerHeight + 4) ;
	document.getElementById("map_canvas").style.height = newHeight + "px";

	DEBUG && console.log("*==Creating Map");
	map = L.map('map_canvas'); // Create new map
	L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png').addTo(map);
	map.setView(myLatLng, 9);
}

function fillMap() {
	DEBUG && console.log("Running fillMap()");
	deleteMap();	// Delete all traces of any previous maps

	map = L.map('map_canvas'); // Create new map

	map.on('load', function(e) {  // This is called when the map center and zoom are set
		DEBUG && console.log("****map load event****");

		circle = L.circle(myLatLng, searchRadius * 1000, {fillOpacity: 0.1});
		circle.addTo(map);
		var circleBounds = new L.LatLngBounds;
		circleBounds = circle.getBounds();
		map.fitBounds(circleBounds);

		currentLocationMarker = new L.marker(myLatLng, {draggable: true, icon: markerIcon}).addTo(map);
		currentLocationMarker.bindPopup("This is where you are searching from. Drag this marker to search in another location", {className: 'custom-popup'});
		currentLocationMarker.on('dragend', function(e){
			myLatLng = e.target.getLatLng();
			fillMap();
		});
		runSearch();
	});

	L.control.locate({
		position: 'topleft',  // set the location of the control
		drawCircle: false,  // controls whether a circle is drawn that shows the uncertainty about the location
		follow: false,  // follow the user's location
		setView: true, // automatically sets the map view to the user's location, enabled if `follow` is true
		keepCurrentZoomLevel: true, // keep the current map zoom level when displaying the user's location. (if `false`, use maxZoom)
		stopFollowingOnDrag: true, // stop following when the map is dragged if `follow` is true (deprecated, see below)
		remainActive: false, // if true locate control remains active on click even if the user's location is in view.
		markerClass: L.circleMarker, // L.circleMarker or L.marker
		circleStyle: {},  // change the style of the circle around the user's location
		markerStyle: {},
		followCircleStyle: {},  // set difference for the style of the circle around the user's location while following
		followMarkerStyle: {},
		icon: 'fa fa-map-marker',  // class for icon, fa-location-arrow or fa-map-marker
		iconLoading: 'fa fa-spinner fa-spin',  // class for loading icon
		circlePadding: [0, 0], // padding around accuracy circle, value is passed to setBounds
		metric: true,  // use metric or imperial units
		onLocationError: function(err) {alert(err.message)},  // define an error callback function
		onLocationFound: function() {alert("Location found")}
	}).addTo(map);

	map.on('locationfound', function(e) {
		DEBUG && console.log("Latitude  = " + e.latitude );
		DEBUG && console.log("Longitude = " + e.longitude);
		myLatLng = L.latLng(e.latitude, e.longitude);
		fillMap();
    });

	DEBUG && console.log("****Adding tile Layer to Map****");
	L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png').addTo(map);

	DEBUG && console.log("****map.setView****" + myLatLng);
	map.setView(myLatLng, 9);

	slider = L.control.slider(
		function(value) {
			searchRadius = value;
			DEBUG && console.log("Search Radius updated to : ", searchRadius)
			map.removeLayer(circle);
			circle = L.circle(myLatLng, searchRadius * 1000, {fillOpacity: 0.1});
			circle.addTo(map);
			var circleBounds = new L.LatLngBounds;
			circleBounds = circle.getBounds();
			map.fitBounds(circleBounds);
			runSearch();
		}, {
		max: 150,
		min: 5,
		value: searchRadius,
		step: 5,
		position: 'topleft',
		width: '15px',
		collapsed: false,
		size: '75%',
		orientation:'vertical'
	}).addTo(map);
}

$( document ).on( "pagecontainershow", function ( event, ui ) {
	DEBUG && console.log("*=pagecontainershow Event");
	var pageId = $('body').pagecontainer('getActivePage').prop('id');

	if (pageId == "search-map") {
		DEBUG && console.log("*==pageId = search-map");
		map.invalidateSize(false);
		fillMap();
	}

	if (pageId == "justfortoday"){
		$(".JFTLoader").empty();
		$.get("http://www.jftna.org/jft/", function( data ) {
			parsedHTML = $.parseHTML( data );
			$.each(parsedHTML, function (i, el) {
				if ( el.nodeName == "TABLE") {
					$(".JFTLoader").append(el.innerHTML);
				}
			});
		});
	}

	if (pageId == "events"){
		$(".UpcomingEvents").empty();
//		$.get("http://android.nasouth.ie/proxy.php?url=http://www.na-ireland.org/for-our-members/conventions-events/", function( data ) {
		$.get("http://www.na-ireland.org/category/upcoming-events/", function( data ) {
			elements = $( data );
			found = $('.site-main', elements);
			$(".UpcomingEvents").append(found);
			$(".entry-meta").remove();
		});
	}

	if (pageId == "speakers") {
		populateConventions();
	}
});


//$(document).on("pagecontainerbeforechange", function (e ,data) {
//    AndroidAudio.silentStop();      // overkill much?
//});

$(document).on("pagecreate", "#search-map", function() {
	$(map).on('load', function () {
		DEBUG && console.log("Running map on load event()");
		fillMap();
	});
});

// Load the spinner if an ajaxStart occurs; stop when it is finished
$(document).on({
  ajaxStart: function() {
    $.mobile.loading('show');
  },
  ajaxStop: function() {
    $.mobile.loading('hide');
  }
});

$( document ).on( "mobileinit", function() {
	$.support.cors = true;
    $.mobile.allowCrossDomainPages = true;
});

// This function converts a number to a day of the week
function dayOfWeekAsString(dayIndex) {
	return ["not a day?", "Sun", "Mon","Tue","Wed","Thu","Fri","Sat"][dayIndex];
}

// This function either starts the AJAX spinner on the map, or stops it.. depending on the flag passed
function spinMap(spinFlag) {
	if (spinFlag == true ) {
		map.spin(true);
		isMapSpinning = true;
		if (currentLocationMarker) {
			currentLocationMarker.setOpacity(0);
		}
	} else {
		map.spin(false);
		isMapSpinning = false;
		if (currentLocationMarker) {
			currentLocationMarker.setOpacity(1);
		}
	}
}

// This function uses the browser function to find our current GPS location, and update our position
function getCurrentGPSLocation() {
    DEBUG && console.log("****getCurrentGPSLocation()****");
    function success(location) {
		DEBUG && console.log("****GPS location found");
		myLatLng = L.latLng(location.coords.latitude, location.coords.longitude);
//		$.mobile.changePage('#yesgeoDialog');
    }
    function fail(error) {
		DEBUG && console.log("****GPS location NOT found");  // Failed to find location, show default map
//		$.mobile.changePage('#nogeoDialog');
	}
	// Find the users current position.  Cache the location for 5 minutes, timeout after 6 seconds
	navigator.geolocation.getCurrentPosition(success, fail, {maximumAge: 500000, enableHighAccuracy:true, timeout: 6000});
}


// This function generates a URL to query the BMLT, based on current location and a search radius
function buildSearchURL () {
	DEBUG && console.log("****Running buildSearchURL()****");
	search_url = "http://www.nasouth.ie/bmlt/main_server/client_interface/json/";
	search_url += "?switcher=GetSearchResults";
	search_url += "&geo_width_km=" + searchRadius;
	search_url += "&long_val=" + myLatLng.lng;
	search_url += "&lat_val=" + myLatLng.lat;
	search_url += "&sort_key=sort_results_by_distance";
	search_url += "&data_field_key=meeting_name,weekday_tinyint,start_time,location_text,location_street,location_info,distance_in_km,latitude,longitude";
	DEBUG && console.log("Search URL = "+ search_url);
}

// This function runs the query to the BMLT and displays the results on the map/list page
function runSearch() {
	DEBUG && console.log("****Running runSearch()****");
	buildSearchURL();

	$.getJSON(search_url, function( data) {
		if (markerClusterer) {
			map.removeLayer(markerClusterer);
		}
		$("#list-results").empty();
		markerClusterer = new L.markerClusterGroup({showCoverageOnHover: false,
													removeOutsideVisibleBounds: false});

        var sunCount =0, monCount =0, tueCount = 0, wedCount = 0, thuCount = 0, friCount = 0, satCount = 0;
        var sunExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='sunMapHead'>Sunday</h4>";
        var monExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='monMapHead'>Monday</h4>";
        var tueExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='tueMapHead'>Tuesday</h4>";
        var wedExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='wedMapHead'>Wednesday</h4>";
        var thuExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='thuMapHead'>Thursday</h4>";
        var friExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='friMapHead'>Friday</h4>";
        var satExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='satMapHead'>Saturday</h4>";

		$.each( data, function( key, val) {
			markerContent = "<li style='list-style-type: none !important'><h4>" + val.meeting_name + "</h4>";
			markerContent += "<p><i>" + dayOfWeekAsString(val.weekday_tinyint)
			markerContent += "&nbsp;" + val.start_time.substring(0, 5) + "</i>&nbsp;&nbsp;";
			markerContent += val.location_text + "&nbsp;" + val.location_street + "<br>";
			markerContent += "<i>" + val.location_info + "</i></p>";

			fromHere = "'" + myLatLng.lat + ',' + myLatLng.lng + "'";
			toHere   = "'" + val.latitude + ',' + val.longitude + "'";
			markerContent += '<i class="fa fa-map-o"></i>&nbsp;<a href="http://maps.google.com/maps?daddr=';
			markerContent += val.latitude + ',' + val.longitude;
			markerContent +='">Directions</a></li>';
			markerContent += '<br><hr>';

			switch (val.weekday_tinyint) {
				case "1": sunCount++; sunExpandLi = sunExpandLi + markerContent; break;
				case "2": monCount++; monExpandLi = monExpandLi + markerContent; break;
				case "3": tueCount++; tueExpandLi = tueExpandLi + markerContent; break;
				case "4": wedCount++; wedExpandLi = wedExpandLi + markerContent; break;
				case "5": thuCount++; thuExpandLi = thuExpandLi + markerContent; break;
				case "6": friCount++; friExpandLi = friExpandLi + markerContent; break;
				case "7": satCount++; satExpandLi = satExpandLi + markerContent; break;
			}

			// Add markers to the markerClusterer Layer
			var aMarker = L.marker([val.latitude, val.longitude], {icon: naIcon});
			aMarker.bindPopup(markerContent, {className: 'custom-popup'});
			markerClusterer.addLayer(aMarker);
		});

		sunExpandLi += "</ul>";
		monExpandLi += "</ul>";
		tueExpandLi += "</ul>";
		wedExpandLi += "</ul>";
		thuExpandLi += "</ul>";
		friExpandLi += "</ul>";
		satExpandLi += "</ul>";

		$("#list-results").append(sunExpandLi);
		$("#list-results").append(monExpandLi);
		$("#list-results").append(tueExpandLi);
		$("#list-results").append(wedExpandLi);
		$("#list-results").append(thuExpandLi);
		$("#list-results").append(friExpandLi);
		$("#list-results").append(satExpandLi);


		$("#sunMapHead").text("Sunday ("    + sunCount + (sunCount == 1 ? " meeting)" : " meetings)"));
		$("#monMapHead").text("Monday ("    + monCount + (monCount == 1 ? " meeting)" : " meetings)"));
		$("#tueMapHead").text("Tuesday ("   + tueCount + (tueCount == 1 ? " meeting)" : " meetings)"));
		$("#wedMapHead").text("Wednesday (" + wedCount + (wedCount == 1 ? " meeting)" : " meetings)"));
		$("#thuMapHead").text("Thursday ("  + thuCount + (thuCount == 1 ? " meeting)" : " meetings)"));
		$("#friMapHead").text("Friday ("    + friCount + (friCount == 1 ? " meeting)" : " meetings)"));
		$("#satMapHead").text("Saturday ("  + satCount + (satCount == 1 ? " meeting)" : " meetings)"));

		map.addLayer(markerClusterer);
		var div = $('#list-results');
		div.enhanceWithin();
	});
}

// This function runs the query to the BMLT and displays the results per county
function runSearchCounty(county) {
	var target = document.getElementById('county-results');
	$("#county-results").empty();
	var spinner = new Spinner().spin();
	target.appendChild(spinner.el);

	raw_meeting_json = false;

	DEBUG && console.log("****Running buildSearchURL()****");
	var search_url = "http://www.nasouth.ie/bmlt/main_server/client_interface/json/";
	search_url += "?switcher=GetSearchResults";
	search_url += "&data_field_key=meeting_name,weekday_tinyint,start_time,location_text,location_street,location_info,location_sub_province,distance_in_km,latitude,longitude,formats";
//	search_url += "&get_used_formats";
	search_url += "&meeting_key=location_sub_province&meeting_key_value=";
    search_url += county;
	DEBUG && console.log("Search URL = "+ search_url);
	DEBUG && console.log("****Running runSearch()****");

	$.getJSON(search_url, function( data) {

		var sunCount =0, monCount =0, tueCount = 0, wedCount = 0, thuCount = 0, friCount = 0, satCount = 0;
		var sunExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='sunHead'>Sunday</h4>";
		var monExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='monHead'>Monday</h4>";
		var tueExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='tueHead'>Tuesday</h4>";
		var wedExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='wedHead'>Wednesday</h4>";
		var thuExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='thuHead'>Thursday</h4>";
		var friExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='friHead'>Friday</h4>";
		var satExpandLi = "<ul style='padding: 0px !important'><div data-role='collapsible' data-autodividers='true' ><h4 id='satHead'>Saturday</h4>";

		$.each( data, function( key, val) {
			markerContent = "<li style='list-style-type: none !important'><h4>" + val.meeting_name + "</h4>";
			markerContent += "<p><i>" + dayOfWeekAsString(val.weekday_tinyint)
			markerContent += "&nbsp;" + val.start_time.substring(0, 5) + "</i>&nbsp;&nbsp;";
			markerContent += val.location_text + "&nbsp;" + val.location_street + "<br>";
			markerContent += "<i>" + val.location_info + "</i></p>";

			fromHere = "'" + myLatLng.lat + ',' + myLatLng.lng + "'";
			toHere   = "'" + val.latitude + ',' + val.longitude + "'";
			markerContent += '<i class="fa fa-map-o"></i>&nbsp;<a href="http://maps.google.com/maps?daddr=';
			markerContent += val.latitude + ',' + val.longitude;
			markerContent +='">Directions</a></li>';
			markerContent += '<br><hr>';

			switch (val.weekday_tinyint) {
				case "1": sunCount++; sunExpandLi = sunExpandLi + markerContent; break;
				case "2": monCount++; monExpandLi = monExpandLi + markerContent; break;
				case "3": tueCount++; tueExpandLi = tueExpandLi + markerContent; break;
				case "4": wedCount++; wedExpandLi = wedExpandLi + markerContent; break;
				case "5": thuCount++; thuExpandLi = thuExpandLi + markerContent; break;
				case "6": friCount++; friExpandLi = friExpandLi + markerContent; break;
				case "7": satCount++; satExpandLi = satExpandLi + markerContent; break;
			}
		});

		sunExpandLi += "</ul>";
		monExpandLi += "</ul>";
		tueExpandLi += "</ul>";
		wedExpandLi += "</ul>";
		thuExpandLi += "</ul>";
		friExpandLi += "</ul>";
		satExpandLi += "</ul>";

		$("#county-results").append(sunExpandLi);
		$("#county-results").append(monExpandLi);
		$("#county-results").append(tueExpandLi);
		$("#county-results").append(wedExpandLi);
		$("#county-results").append(thuExpandLi);
		$("#county-results").append(friExpandLi);
		$("#county-results").append(satExpandLi);

		$("#sunHead").text("Sunday ("    + sunCount + (sunCount == 1 ? " meeting)" : " meetings)"));
		$("#monHead").text("Monday ("    + monCount + (monCount == 1 ? " meeting)" : " meetings)"));
		$("#tueHead").text("Tuesday ("   + tueCount + (tueCount == 1 ? " meeting)" : " meetings)"));
		$("#wedHead").text("Wednesday (" + wedCount + (wedCount == 1 ? " meeting)" : " meetings)"));
		$("#thuHead").text("Thursday ("  + thuCount + (thuCount == 1 ? " meeting)" : " meetings)"));
		$("#friHead").text("Friday ("    + friCount + (friCount == 1 ? " meeting)" : " meetings)"));
		$("#satHead").text("Saturday ("  + satCount + (satCount == 1 ? " meeting)" : " meetings)"));

		spinner.stop();
		var div = $('#county-results');
		div.enhanceWithin();
	});
}

function populateConventions() {

	conventionsURL = "http://android.nasouth.ie/conventions.json";

	$.getJSON(conventionsURL, function( data) {
		var target = document.getElementById('fillSpeakers');
		$("#fillSpeakers").empty();
		var spinner = new Spinner().spin();
		target.appendChild(spinner.el);

		var conventionLi = "<ul id='playlist' data-role='listview' class='ui-listview-outer' data-inset='true' >";
		$.each(data, function() {
			$.each(this, function(k, v) {
				conventionLi += "<li data-role='collapsible' data-iconpos='right' data-shadow='false' data-corners='false'>";
				conventionLi += "<h2>" + v.convention_name + "</h2>";
				conventionLi += "<ul id='test2' data-role='listview' data-shadow='false' data-inset='true' data-corners='true'>";
//				DEBUG && console.log("Convention = " + v.convention_name);
				var ords = v.speakers;
				var speakerLi = "";
				$.each(ords, function(k2, v2) {
//					DEBUG && console.log("	Filename = " + v2.fileName);
//					DEBUG && console.log("	Title = " + v2.Title);
//					speakerLi += "<li><a href='#' class='ui-btn ui-shadow ui-corner-all' > ";
//					speakerLi += v2.Title;
//                    speakerLi += "<br>";
//					speakerLi += "<i class='fa fa-stop  fa-lg icon-4x' style='float: right;' onClick='AndroidAudio.stopAudio();return false;'>&nbsp;&nbsp;&nbsp;</i>";
//					speakerLi += "<i class='fa fa-pause fa-lg icon-4x' style='float: right;' onClick='AndroidAudio.pauseAudio();return false;'>&nbsp;&nbsp;&nbsp;</i>";
//					speakerLi += "<i class='fa fa-play  fa-lg icon-4x' style='float: right;' onClick='trackLength = AndroidAudio.playAudio(\"" +  v2.fileName + "\");return false;'>&nbsp;&nbsp;&nbsp;</i>";
//					speakerLi += "</a></li>";
					speakerLi += "<li><a href='" +  v2.fileName + "' class='ui-btn ui-shadow ui-corner-all' > ";
					speakerLi += v2.Title;
					speakerLi += "</a></li>";



				});

				conventionLi += speakerLi;
				conventionLi += "</ul></li>";
			});
		});
		conventionLi += "</ul>";
		spinner.stop();
		$('#fillSpeakers').append( conventionLi );

		var div = $('#fillSpeakers');
		div.enhanceWithin();

	});
}

$( document ).ready(function() {
    DEBUG && console.log( "ready!" );
	newMap();
	getCurrentGPSLocation();
});
