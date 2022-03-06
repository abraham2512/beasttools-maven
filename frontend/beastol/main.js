// import {Map, View} from 'ol';
// import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';

import 'ol/ol.css';
import Map from 'ol/Map';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import XYZ from 'ol/source/XYZ';
import VectorLayer from 'ol/layer/Vector';

const my_layer = new TileLayer({
  source: new XYZ({
    url:
      "http://127.0.0.1:8080/tiles/?dataset=SafetyDept&z={z}&x={x}&y={y}"
  }),
  
});

const map = new Map({
  target: 'map',
  layers: [
    new TileLayer({
      source: new OSM()
    }),
    my_layer
  ],
  view: new View({
    center: [0, 0],
    zoom: 2
  })
});



//map.addLayer(my_layer)


// const map = new Map({
//   target: 'map',
//   layers: [
//     new TileLayer({
      
//       source: new XYZ({
//         url:
//           "http://127.0.0.1:8081/tile-{z}-{x}-{y}.png"
//       }),
      
//     }),
   
//   ],
//   view: new View({
//     center: [0, 0],
//     zoom: 2
//   })
// });