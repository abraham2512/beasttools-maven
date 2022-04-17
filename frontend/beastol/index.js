
import OSM from 'ol/source/OSM';
import 'ol/ol.css';
import Map from 'ol/Map';
import TileLayer from 'ol/layer/Tile';
import View from 'ol/View';
import XYZ from 'ol/source/XYZ';
import axios from 'axios';
import $ from 'jquery';

function launchMap(filename){
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
    target: 'map',
    layers: [
      new TileLayer({
        source: new OSM()
      }),
      my_layer
    ],
    view: new View({
      center: [0, 0],
      zoom: 0 
    })
  });
  
  
}
  // axios.get("http://127.0.0.1:8080/files/SafetyDept").then(function(response){
  //   console.log(response);
  // })

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
    "Access-Control-Allow-Origin": "*",
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
  //let html_append = `<div class="card-body"> <h5 class="card-title">${dataset_name}</h5></div>`;
  
    let newDiv = document.createElement("div");
    newDiv.className = "card-body"

    let h5 = document.createElement("h5");
    h5.className = "card-title";
    h5.innerHTML = dataset_name;
    newDiv.appendChild(h5);

    let status = document.createElement("h6");
    status.id="status_"+dataset_name;
    status.innerHTML="started";
    newDiv.appendChild(status);

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

    let launch = document.createElement("input");
    launch.type="button"
    launch.id=`launch_${dataset_name}`
    launch.value="Launch Map"
    launch.disabled=true;
    launch.addEventListener('click', function(){
      launchMap(dataset_name);
    });
    
    newDiv.appendChild(launch);    
    
    document.getElementById("datasets").appendChild(newDiv);

  
  //$('#datasets').append(html_append);

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
