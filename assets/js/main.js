// var DEBUG = false;
var DEBUG = true;


$( document ).on( "pagecontainershow", function ( event, ui ) {
	DEBUG && console.log("*=pagecontainershow Event");
	var pageId = $('body').pagecontainer('getActivePage').prop('id');

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


// This function runs the query to the BMLT and displays the results per county
function runSearchCounty(county) {
	var target = document.getElementById('county-results');
	$("#county-results").empty();
	var spinner = new Spinner().spin();
	target.appendChild(spinner.el);

	raw_meeting_json = false;

	var search_url = "http://www.nasouth.ie/bmlt/main_server/client_interface/json/";
	search_url += "?switcher=GetSearchResults";
	search_url += "&data_field_key=meeting_name,weekday_tinyint,start_time,location_text,location_street,location_info,location_sub_province,distance_in_km,latitude,longitude,formats";
//	search_url += "&get_used_formats";
	search_url += "&meeting_key=location_sub_province&meeting_key_value=";
    search_url += county;
	DEBUG && console.log("Search URL = "+ search_url);

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
