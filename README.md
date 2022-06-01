# Beast Microservice

Akka Actors and Akka HTTP based microservice built for distributed processing and visualization of geo-spatial data using the [BEAST](https://bitbucket.org/bdlabucr/beast/src/master/) library

For more documentation, please check this link https://abraham2512.github.io/beastapi-docs/

### Dependencies
JDK8 Maven Scala2.12 Spark3.0 NodeJS 

### Instructions for development environment
#### Frontend
* cd into frontend/beastol/ and install dependencies with `npm install`
* Start Frontend -> run `npm run start`
* Build -> `npm run build` (outputs to maven resources folder)

#### Backend
* Open with IntelliJ and use Maven to compile 
* Create an empty folder "data" with sub-folders "indexed" and "viz"
* Start Backend -> Edit configurations to use StartApp as main class to run as java 8 Application. 
* Build -> `mvn clean package` (build fat jar)

Deploy with `beast beasttools-1.1-RC.jar` 
Download the latest beast binary [here.](https://bitbucket.org/bdlabucr/beast/downloads/?tab=downloads)

## Authors

Abraham Miller  
email: apala049@ucr.edu 

## Version History

* 1.1 RC
* Stable_RC

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
