# Welcome to Beast API Documentation

Welcome to the documentation of the BEAST API Project.

## Getting Started to Develop!

* The project requires JDK v1.8, Scala 2.12, npm 8.8.0 and Spark v3.0.3 for the [BEAST](https://bitbucket.org/bdlabucr/beast/src/master/) library, ensure `SPARK_HOME` and `JAVA_HOME` are in path.
* Pull the repository using `git clone git@github.com:abraham2512/beasttools-maven.git`
* Compile the project using IntelliJ/Maven and run with StartApp as main class
 

### Project layout
    src/
        main/
            scala/
                StartApp.scala  # Main function and entry point to the server.
                actors  # Contains all the actors
                    Routes.scala       # ScalaDSL routing logic.
                    FileRegistry.scala  # Helper object for the HTTP routing Actor
                    HdfsActor.scala     # Actor for indexing and partitioning of data using BEAST 
                    TileActor.scala     # Actor for rendering tiles on the fly using index 
                models
                    DataFileDAL.scala   # Relational Mapping for H2 metadata database
                    DataFileDAO.scala
                utils
                    JsonFormats.scala
        resources/
            application.conf #Configuration File for the server
    pom.xml # Maven dependencies

### Building and running JAR
Download beast binaries from this link [beast-bitbucket](https://bitbucket.org/bdlabucr/beast/downloads/?tab=downloads)
Build the frontend static files first with `cd frontend/beastol && npm install && npm run build`
Move the static files to resources folder `mv dist/* ../../src/main/resources/`
Run `cd ../../ && mvn clean package` to build a deployable fat-jar
Deploy the jar with `beast beast-1.1-RC.jar`
    
## Additional Documentation
- [Beast Documentation](https://bitbucket.org/bdlabucr/beast/src/master/)
- [AkkaHTTP Documentation](https://doc.akka.io/docs/akka-http/current/index.html)
- [Akka Concurrency by Derek Wyatt](https://www.artima.com/shop/akka_concurrency) - A great book that runs through the basics of Akka!