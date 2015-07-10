# Fare Gap

## Highlights

- Analyze NYC taxi data to see how long it takes to get a fare after a dropoff

## Spark

- Apache Spark (using Scala) for processing the data
- Import csv, parse each trip into an instance of a class
- Filter ridiculous coordinates out (GPS errors? Data corruption?) & limit to a Manhattan box
- Ordered data by car and then pickup time
- Calculate the time between the dropoff and the next pickup
- Divide the box into a 100x100 grid and group the trips into the boxes
- Average the wait time for each box
- Output the data as a series of GeoJSON objects

## Leaflet

- Leaflet (javascript library) for displaying the output on a map
- Based on the tutorial http://leafletjs.com/examples/choropleth.html
- Display a map centered on Manhattan, using OpenStreetMap tiles
- Add a color scheme and legend
- Load the GeoJSON data from the file output by Spark
- Apply the correct colour style to each GeoJSON object based on the wait times
