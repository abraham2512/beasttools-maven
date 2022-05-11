# Maps Interface

The interface is built with [OpenLayers](https://openlayers.org) and JavaScript.  

## Start your own front-end
OpenLayers.js can be imported as a browser script but modern JavaScript works best when using and authoring modules. 
The recommended way of using OpenLayers is installing the ol package. This can be done through node using the command `npx create-ol-app`. 
For more information, see the guide [here](https://openlayers.org/en/latest/doc/tutorials/bundle.html)

## Clone Beast Viz 
To clone the default front-end, run this command to clone the repository
`git clone git@github.com:abraham2512/beasttools-maven.git` and open the frontend/beastol/ project folder.
The project has **index.js**, **index.html**, **styles.css** and a **package.json** configuration file.

### Setup the interface
- First install node modules from package.json with `npm install`. 
- For development run the server using `npm run start`.
- For deployment bundle the files using `npm run build` to create one **index.html** file that can be served.


### index.js

###### function launchMap(filename) 
    
    :Creates a map with the filename ID. 
    :Makes GET request to /tiles/ endpoint in backend with dataset = {filename} and {Z}, {X}, {Y} values.


###### function handleDataFileSubmit(event)
    :Handles form submit with data json.
    :Makes POST request to /files/ endpoint with data payload. This triggers partitioning and indexing of data.

###### function appendCardDiv(filename)
    :Appends a new Card for the dataset in the UI to track its progress and launch when ready.     


### index.html

This file imports the index.js module and contains the main html.