
/*

Name: Abraham Miller
Date: 14.04.2022

This is the main JS file with all the JS functions used in the front end. 
The API calls to back end are made from the relevant functions.
*/

//IMPORTS
import OSM from 'ol/source/OSM';
import 'ol/ol.css';
import Map from 'ol/Map';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import XYZ from 'ol/source/XYZ';
import axios from 'axios';
import Overlay from 'ol/Overlay';
import {toLonLat, transform} from 'ol/proj';
import {toStringHDMS} from 'ol/coordinate';
import ol from 'ol';



//THIS FUNCTION LAUNCHES THE OL MAP WHEN LAUNCH BUTTON IS CLICKED
function launchMap(filename){



  // CLICK EVENT

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
    



  let map_div = document.getElementById('map_div');
  document.getElementById('map').remove()
  let new_map = document.createElement('div');
  new_map.id="map";
  map_div.appendChild(new_map);

  const my_layer = new TileLayer({
    source: new XYZ({
      url:
        "http://127.0.0.1:8080/tiles/?dataset="+ filename +"&z={z}&x={x}&y={y}"
    }),
    
  });
  
  const map = new Map({
    overlays:[overlay],
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


  /**
 * Add a click handler to the map to render the popup.
 */
  map.on('singleclick', function (evt) {
    let coordinate = evt.coordinate;
    //const hdms = toStringHDMS(toLonLat(coordinate));
    //let point1 = project(coordinate[0],coordinate[1])
    //let zoom = evt.map.getView().getZoom();
    //let scaledZoom = zoomScale (zoom);
    //point1 = transform (point1, scaledZoom);
    //content.innerHTML = '<p>You clicked here:</p><code>' + hdms + '</code>';
    //content.innerHTML = '<p>You clicked here:</p><code>' + point1 + '</code>';
    var px = evt.pixel;
    var southwest = transform(map.getCoordinateFromPixel([px[0] - 3, px[1] + 3]), 'EPSG:3857', 'EPSG:4326');
    var northeast = transform(map.getCoordinateFromPixel([px[0] + 3, px[1] - 3]), 'EPSG:3857', 'EPSG:4326');
    var opx = map.getCoordinateFromPixel(evt.pixel) ;
    var mbrText = `${southwest[0]},${southwest[1]},${northeast[0]},${northeast[1]}`;
    

    axios.get(
    'http://127.0.0.1:8080/meta',{
        params: {
        dataset:"SafetyDept",
        mbrString:mbrText
        }
    }).then((response)=>{
        let object_string = "";
        for (const [key,value] of Object.entries(response.data)){
        object_string+=`${key}: ${value} <br>`
        }
        content.innerHTML = '<code>' + object_string + '</code>';
        console.log(response.data);
    }).catch((error)=>{
        console.log("ERROR:"+error);
    });
    overlay.setPosition(coordinate);
  });
}




//UI HANDLER FUNCTIONS BELOW

function handleDataFileSubmit(event) {
  event.preventDefault();
  console.log("Function called");
  let filename = document.getElementById("filename").value;
  let my_data = {
    filename : document.getElementById("filename").value,
    filesource : document.getElementById("filepath").value,
    filestatus : "start",
    filetype : "default"
  }
  let my_headers = {
    "Access-Control-Allow-Origin": "*", //Do not enable in production
    "Access-Control-Allow-Credentials": "true",
    "Access-Control-Allow-Headers": "Authorization, Content-Type, X-Requested-With",
    "Content-Type": "application/json"
  }
   //appendCardDiv(filename);
  axios.post('http://127.0.0.1:8080/files',data=my_data,headers=my_headers)
  .then(function(response){
    console.log(response);
    if(response.data['description']=="created"){
      appendCardDiv(filename);
    }
      

  })
  .catch(function(error){
    console.log(error)
  });
  
}

function appendCardDiv(dataset_name){
  console.log("Appending Div");
  //
  
  let newDiv = document.createElement("div");
  newDiv.className = "card"

  let h5 = document.createElement("h5");
  h5.className = "card-title";
  h5.innerHTML = dataset_name;
  newDiv.appendChild(h5);

  let status = document.createElement("h6");
  status.id="status_"+dataset_name;
  status.innerHTML="started";
  newDiv.appendChild(status);

    //CHECK STATUS BUTTON TODO -> AUTO REFRESH FOR STATUS
  let inputElement = document.createElement('input');
  inputElement.type = "button";
  inputElement.value = "Check Status"
  inputElement.addEventListener('click', function(){
    console.log("CheckingStatus");
    console.log(dataset_name);
    axios.get(`http://127.0.0.1:8080/files/${dataset_name}`)
    .then(
      function(response){
      let data = response.data;
      let status = data['filestatus']
      console.log(status);
      document.getElementById(`status_${dataset_name}`).innerHTML=status;
      if(status=='indexed'){
        document.getElementById(`launch_${dataset_name}`).disabled=false;
      }  
    })
    .catch(
      function(error){
        console.log(error);
      }
    );
  });
    
  newDiv.appendChild(inputElement);

  //LAUNCH BUTTON
  let launch = document.createElement("input");
  launch.type="button"
  launch.id=`launch_${dataset_name}`
  launch.value="Launch Map"
  launch.disabled=true;
  launch.addEventListener('click', function(){
    launchMap(dataset_name);
  });
  
  newDiv.appendChild(launch);    
    
    
  //TODO DELETE FUNCTION
    
    
    
    
    
  //APPENDING NEW DATASET TO DATASETS
  document.getElementById("datasets").appendChild(newDiv);

}


document.addEventListener("DOMContentLoaded",function(){
  axios.get("http://127.0.0.1:8080/files").then(function(response){
    let data = response.data.files;
    console.log(data);
    for (const file of data){
      appendCardDiv(file.filename);
    }
  })
})

document.getElementById('dataset_submit').addEventListener('click',handleDataFileSubmit)

// const  EARTH_RADIUS = 6378137;
// const  MAX_LATITUDE = 85.0511287798;

// function project (lat, lng)
// {
//     var d = Math.PI / 180,
//             max = MAX_LATITUDE,
//             lat = Math.max(Math.min(max, lat), -max),
//             sin = Math.sin(lat * d);

//     return {x: EARTH_RADIUS * lng * d,
//             y: EARTH_RADIUS * Math.log((1 + sin) / (1 - sin)) / 2
//         };
// }
    
    
// function zoomScale (zoom)
// {
//   return 256 * Math.pow(2, zoom);
// }
    
    
// function transform (point, scale) {
//   scale = scale || 1;
//   point.x = scale * (2.495320233665337e-8    * point.x + 0.5);
//   point.y = scale * (-2.495320233665337e-8 * point.y + 0.5);
//   return point;
// }