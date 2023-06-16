
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


let map;
//Launches OL MAP on launch_button click
function launchMap(filename){
  //Creating Popup overlay before map
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
   
  //  /**
  //    * Add a click handler to hide the popup.
  //    * @return {boolean} Don't follow the href.
  //    */
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

  // const my_layer = new TileLayer({
  //   source: new XYZ({
  //     url:
  //       "http://127.0.0.1:8080/tiles/?dataset="+ filename +"&z={z}&x={x}&y={y}"
  //   }),
    
  // });
  
  map = new Map({
    overlays:[overlay],
    target: 'map',
    layers: [
      new TileLayer({
        source: new OSM()
      }),
      // my_layer
    ],
    view: new View({
      center: [0, 0],
      zoom: 2 
    })
  });
  
  // CLICK EVENT

  //document.getElementById("popup").hidden=false;

  /**
 * Add a click handler to the map to render the popup.
 */
  map.on('singleclick', function (evt) {
    let coordinate = evt.coordinate;
    var px = evt.pixel;
    var southwest = transform(map.getCoordinateFromPixel([px[0] - 3, px[1] + 3]), 'EPSG:3857', 'EPSG:4326');
    var northeast = transform(map.getCoordinateFromPixel([px[0] + 3, px[1] - 3]), 'EPSG:3857', 'EPSG:4326');
    var opx = map.getCoordinateFromPixel(evt.pixel) ;
    var mbrText = `${southwest[0]},${southwest[1]},${northeast[0]},${northeast[1]}`;
    
    var filename = document.getElementById("dataset_current").content;
    axios.get(
    'http://127.0.0.1:8080/meta',{
        params: {
        dataset:filename,
        mbrString:mbrText
        }
    }).then((response)=>{
        let object_string = "";
        for (const [key,value] of Object.entries(response.data)){
        object_string+=`${key}: ${value} <br>`
        }
        content.innerHTML = '<code>' + object_string + '</code>';
        document.getElementById('popup').style.display="block";
        console.log(response.data);
    }).catch((error)=>{
        console.log("ERROR:"+error);
    });
    overlay.setPosition(coordinate);
  });
}

function launchDataset(filename){
  
  // Setting Meta Value for current dataset

  document.getElementById("dataset_current").content=filename;

  const my_layer = new TileLayer({
    source: new XYZ({
      url:
        "http://127.0.0.1:8080/tiles/?dataset="+ filename +"&z={z}&x={x}&y={y}"
    }),  
  });
  let layers = map.getLayers();
  if(layers.array_.length == 2){
    map.removeLayer(map.getLayers().array_[1])
  }
  map.addLayer(my_layer);
  
}  


//UI HANDLER FUNCTIONS BELOW

function deleteDataset(dataset_name) {
  if (confirm("Do you want to delete this dataset?")==true){
    axios.delete(`http://127.0.0.1:8080/files/${dataset_name}`)
    .then((resp)=>{
      if (resp.status==202) {
        document.getElementById("div_"+dataset_name).remove();
        if (document.getElementById('map')!=undefined){
          document.getElementById('map').remove()
        }
      }
      else{
        console.log("Could not delete dataset");
        console.log(resp.data);
      }
    }
    );
  }
 
}
function indexDataset(dataset_name) {
  axios.post('http://127.0.0.1:8080/index', {
    filename : dataset_name,
    filesource : "test",
    filestatus : "partitioned",
    filetype : "geojson"
  })
  .then((resp)=>{
    console.log(resp.status)
    console.log('started r-tree indexing')
  })
  let indexing_status = null
  let indexing_status_interval = setInterval(function(){
    axios.get("http://127.0.0.1:8080/files").then(function(response){
    //console.log(response.data);
    let files = response.data['files'];
    for (const file of Object.values(files)){
      if (file.filename == dataset_name && file.filestatus == "rtree-indexed") {
        console.log(file.filename,file.filestatus);
        indexing_status = file.filestatus;
        axios.put('http://127.0.0.1:8080/files',{
            filename : dataset_name,
            filesource : "test",
            filestatus : "start",
            filetype : "geojson"
        })
        .then((resp)=>{
            console.log(resp.status)
            console.log('r-tree partitioning and index creation started')

        });
        clearInterval(indexing_status_interval);
      }
    }
    });
  },5000)

    // axios.put('http://127.0.0.1:8080/files',{
    //     filename : dataset_name,
    //     filesource : "test",
    //     filestatus : "start",
    //     filetype : "geojson"
    // })
    // .then((resp)=>{
    //     console.log(resp.status)
    //     console.log('r-tree partitioning and index creation started')

    // })


}
// CREATE DATASET
function handleDataFileSubmit(event) {
  event.preventDefault();
  console.log("Function called");
  let filename = document.getElementById("filename").value;
  let filesource = document.getElementById("filepath").value
   //appendCardDiv(filename);
  axios.post('http://127.0.0.1:8080/files',{
    filename : filename,
    filesource : filesource,
    filestatus : "start",
    filetype : "default"
  })
  .then(function(response){
    console.log(response);
    if(response.status==202){
      appendCardDiv(filename);
    }
  })
  .catch(function(error){
    console.log("Error submitting dataset "+error)
  });
  
}


// RUN QUERY
function handleRunQuery(event){
  event.preventDefault();
  console.log("Function called");
  let dataset_name = document.getElementById("query_dataset_name").value;
  let query_run = document.getElementById("query_run").value
  let save_as_new = $("#save_as_new_dataset").is(":checked") ? "true" : "false";
  //document.getElementById("save_as_new_dataset").value;
  //appendCardDiv(filename);
  console.log(dataset_name,query_run);
   axios.post('http://127.0.0.1:8080/query',{
    dataset : dataset_name,
    query : query_run,
    saveMode : save_as_new
  })
  .catch(function(error){
    console.log("Error submitting dataset "+error)
  });
}


//APPENDING DATASETS
function appendCardDiv(dataset_name){
  console.log("Appending "+dataset_name);

  let newDiv = document.createElement("div");
  newDiv.className = "my_card"
  newDiv.id="div_"+dataset_name
  let h5 = document.createElement("h6");
  h5.className = "card-title";
  h5.innerHTML = dataset_name;
  newDiv.appendChild(h5);

  
  //PROGRESS BAR
  let progress_div = document.createElement("div");
  progress_div.style="margin-left:10%; margin-right:10%";
  let progress_bar = document.createElement("div");
  progress_bar.className="progress-bar progress-bar-striped progress-bar-animated";
  progress_bar.style="width:10%; height:20px";
  progress_bar.innerHTML="Enqueued";
  progress_bar.id="progress_"+dataset_name;
  progress_div.appendChild(progress_bar);

  newDiv.appendChild(progress_div);

    //QUERY BUTTON
  //   let query_button = document.createElement("input");
  //   query_button.type="button"
  //   query_button.id=`delete_button_${dataset_name}`
  //   query_button.value="query"
  //   query_button.className="btn btn-info"
  //   query_button.disabled=false;
  //   query_button.addEventListener('click', function(){
  //     console.log("Running query");
  //   });

  // newDiv.appendChild(query_button);

    //DELETE BUTTON
    let delete_button = document.createElement("input")
    delete_button.type="button"
    delete_button.id=`delete_button_${dataset_name}`
    delete_button.value="delete"
    delete_button.className="btn btn-danger"
    delete_button.disabled=false
    delete_button.addEventListener('click', function(){
    deleteDataset(dataset_name)
    });

  newDiv.appendChild(delete_button);

  //INDEX BUTTON
  let index_button = document.createElement("input")
  index_button.type="button"
  index_button.id=`index_button_${dataset_name}`
  index_button.value="index"
  index_button.className="btn btn-primary"
  index_button.disabled=false
  index_button.addEventListener('click', function(){
    indexDataset(dataset_name);
  });

  newDiv.appendChild(index_button);

  //LAUNCH BUTTON
  let launch_button = document.createElement("input");
  launch_button.type="button"
  launch_button.id=`launch_button_${dataset_name}`
  launch_button.value="launch"
  launch_button.className="btn btn-success"
  launch_button.disabled=true;
  launch_button.addEventListener('click', function(){
    launchDataset(dataset_name);
  });
  
  newDiv.appendChild(launch_button);

  //APPENDING NEW DATASET TO DATASETS
  document.getElementById("datasets").appendChild(newDiv);

}

function updateStatus(dataset,status){
  if(status=='start'){
    //document.getElementById('progress_'+dataset).style="width"
  }
  else if (status=='partitioned'){
    document.getElementById('progress_'+dataset).style="width:50%"
    document.getElementById('progress_'+dataset).innerHTML="partitioned"
  }
  else if(status=='indexed'){
    document.getElementById('progress_'+dataset).style="width:100%"
    document.getElementById('progress_'+dataset).innerHTML="indexed"
    document.getElementById('progress_'+dataset).className ="progress-bar bg-success"
    document.getElementById(`launch_button_${dataset}`).disabled=false;
    document.getElementById(`delete_button_${dataset}`).disabled=false;
  }
  else if (status=='error'){
    document.getElementById('progress_'+dataset).style="width:100%"
    document.getElementById('progress_'+dataset).className ="progress-bar bg-error"
    document.getElementById('progress_'+dataset).innerHTML="pls delete and resubmit"
    document.getElementById(`launch_button_${dataset}`).disabled=true;
    document.getElementById(`delete_button_${dataset}`).disabled=false;
  }

}

document.addEventListener("DOMContentLoaded",function(){
  //Create divs for exisiting datasets
  axios.get("http://127.0.0.1:8080/files").then(function(response){
    let data = response.data.files;
    console.log(data);
    for (const file of data){
      appendCardDiv(file.filename);
      updateStatus(file.filename,file.filestatus);
    }
  });

  //Auto refresh for dataset status
  setInterval(function(){
    axios.get("http://127.0.0.1:8080/files").then(function(response){
    //console.log(response.data);
    let files = response.data['files'];
    for (const file of Object.values(files)){
      console.log(file.filename,file.filestatus);
      updateStatus(file.filename,file.filestatus);
    }
    });
  },10000)
});

document.getElementById('dataset_submit').addEventListener('click',handleDataFileSubmit);
document.getElementById('query_submit').addEventListener('click',handleRunQuery);
window.onload=launchMap();