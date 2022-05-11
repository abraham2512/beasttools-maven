## Adding a new feature - OpenLayers event

In this section we use a openlayers click event to create a popup handler.

First we create the HTML elements that make the popup inside the `map_div` in `index.html`.

```
<div id="map_div" class="child2">
        <div id="popup" class="ol-popup">
          <a href="#" id="popup-closer" class="ol-popup-closer"></a>
          <div id="popup-content"></div>
        </div>
<div id="map"></div>  
```

Now we can bind an event handler for a 'singleClick' event in OpenLayers. In `launchMap()` function in OpenLayers,
first we define the characteristics of the popup.

```
/**
   * Elements that make up the popup.
   */
  const container = document.getElementById('popup');
  const content = document.getElementById('popup-content');
  const closer = document.getElementById('popup-closer');
  
  /**
    * Create an overlay to anchor the popup to the map.
    */
  const overlay = new Overlay({
    element: container,
    autoPan: {
      animation: {
        duration: 250,
      },
    },
  });
  
  /**
    * Add a click handler to hide the popup.
    * @return {boolean} Don't follow the href.
    */
  closer.onclick = function () {
    overlay.setPosition(undefined);
    closer.blur();
    return false;
  };
```

Now we can bind an event handler to this popup function

```
/**
 * Add a click handler to the map to render the popup.
 */
  map.on('singleclick', function (evt) {
    let coordinate = evt.coordinate;
    const hdms = toStringHDMS(toLonLat(coordinate));
    content.innerHTML = '<p>You clicked here:</p><code>' + hdms + '</code>';
    
    axios.get(`http://127.0.0.1:8080/meta`)
    .then(
      function(response){
      let data = response.data;
      console.log(data);
      content.innerHTML = '<p>This is the data:</p><code>' + data + '</code>';
    })
    .catch(
      function(error){
        console.log(error);
      }
    );

    overlay.setPosition(coordinate);
  });
```