# Welcome to Beast API Documentation

Welcome to the documentation of the BEAST API Project! 




## Getting Started to Develop!

* The project requires JDK v1.8 and Spark v3.0.1 for the [BEAST](https://bitbucket.org/bdlabucr/beast/src/master/) library, ensure `SPARK_HOME` and `JAVA_HOME` are in path.
* Pull the repository using `git clone git@github.com:abraham2512/beasttools-maven.git`
* Maven is used to compile dependencies and launch the project
* Compile the project using IntelliJ with compile-time dependencies and run with StartApp as main class
 

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

### Building 
Maven-shade-plugin is used to build into a Uber JAR using `mvn clean package` 

### Deploying
The Uber Jar with all dependencies can be deployed on Spark with `spark-submit`  
*Build dependenices not clean/working at the time of writing*. 
    
## Additional Documentation
- [Beast Documentation](https://bitbucket.org/bdlabucr/beast/src/master/)
- [AkkaHTTP Documentation](https://doc.akka.io/docs/akka-http/current/index.html)
- [Akka Concurrency by Derek Wyatt](https://www.artima.com/shop/akka_concurrency) - A great book that runs through the basics of Akka!