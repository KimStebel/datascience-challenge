function getColor(d) {
    return d > 90 ? '#d73027' :
           d > 60 ? '#f46d43' :
           d > 45 ? '#fdae61' :
           d > 30 ? '#fee08b' :
           d > 20 ? '#ffffbf' :
           d > 10 ? '#d9ef8b' :
           d > 5 ? '#a6d96a' :
           d > 2  ? '#66bd63' :
                    '#1a9850';
}












function style(feature) {
    return {
        fillColor: getColor(feature.properties.waitTime),
        weight: 0,
        opacity: 0,
        border: 'none',
        color: 'white',
        dashArray: '3',
        fillOpacity: 0.7
    };
}

var map = L.map('map').setView([40.7893, -73.9635], 12);
L.tileLayer('http://a.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors',
    maxZoom: 18
}).addTo(map);

var legend = L.control({position: 'bottomright'});

legend.onAdd = function (map) {

    var div = L.DomUtil.create('div', 'info legend'),
        grades = [2, 5, 10, 20, 30, 45, 60, 90],
        labels = [];

    // loop through our density intervals and generate a label with a colored square for each interval
    for (var i = 0; i < grades.length; i++) {
        div.innerHTML +=
            '<i style="background:' + getColor(grades[i] + 1) + '"></i> ' +
            grades[i] + (grades[i + 1] ? '&ndash;' + grades[i + 1] + '<br>' : '+');
    }

    return div;
};

legend.addTo(map);

L.geoJson(waitData, {style: style}).addTo(map);