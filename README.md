# Beast Microservice

Akka Actors and Akka HTTP based microservice built for distributed processing and visualization of geo-spatial data using the BEAST library for geospatial data

For more documentation, please check this link https://abraham2512.github.io/beastapi-docs/

### Dependencies

Java 1.8, Scala 2.12 and Spark 3.0

### Instructions to setup development environment
* Open with IntelliJ and use Maven to compile 
* Create an empty folder "data" with sub-folders "indexed" and "viz"
* Start Backend -> Edit configurations to use StartApp as main class and run as java 8 Application. No arguments.
* Start Frontend -> cd into frontend/beastol/ and run `npm run start`

### API Endpoints
* Get all files GET-> http://127.0.0.1:8080/files
* Get details of file GET->  http://127.0.0.1:8080/files/filename
* Delete a file DELETE-> http://127.0.0.1:8080/files/filename

* Load a file into beast POST -> http://127.0.0.1:8080/files
    with body containing a json like
 {
  "filename": "SafetyDept",
  "filetype": "shapefile",
  "filesource": "/Users/abraham/Downloads/PLUS_Survey_Modified/",
  "filestatus": "start"
  }
* Fetch a pre-generated tile or generate one on the fly 
    GET-> http://127.0.0.1:8080/tiles/?dataset=dataset-name&z=Z-value&x=X-value&y=Y-value


### Pending work

* Integration tests with all actors and test them together
* Fix maven build issue when building uber-jar

## Authors

Abraham Miller  
email: apala049@ucr.edu 

## Version History

* 0.1
* Initial Release

## License

Copyright 2018 University of California, Riverside
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Acknowledgments
Beast Library for Spatial Data - https://bitbucket.org/bdlabucr/beast/src/master/
