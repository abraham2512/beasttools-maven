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
* To run in Debug mode or to Execute the code from an IDE, edit the execution configuration to use StartApp as the main class. Run with Java 18 and add the following VM options to the configuration:
    * `--add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED`
* Alternatively, Build -> `mvn clean package` (build fat jar) and Deploy with `beast beasttools-1.1-RC.jar`.
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
