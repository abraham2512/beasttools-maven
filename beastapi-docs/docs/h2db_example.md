## Adding a new feature H2DB

H2 database is used as a metadata store in our server to track the status of Datasets.
The database is connected with JDBC and all actors importing the driver will have access to it. 

We can define entity relationships using a Functional Relational Mapping (FRM) using Scala Lang Integrated Connection Kit or [Slick](https://scala-slick.org/doc/3.3.3/introduction.html)

It allows you to work with stored data almost as if you were using Scala collections while at the same time giving you full control over when database access happens and which data is transferred. You can also use SQL directly. Execution of database actions is done asynchronously, making Slick a perfect fit for reactive applications based on Akka.

In the *Models* folder, there are two files `DataFileDAO.scala` which contains the entity expressions and the accessor methods.

The access methods to these entities are in the `DataFileDAL.scala`. Some of the methods have an `Await` keyword to enforce a synchronous response when accessing the database. You can define your new database function in this file and access it in the other actors. 