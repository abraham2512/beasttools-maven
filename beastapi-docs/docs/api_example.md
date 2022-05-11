
## Adding a new feature - API endpoint

In this section, we will create a test endpoint named "/meta" to return a string for a GET request.



#### Routes.scala 

This file contains the akkaHTTP routing logic and also extends the FileRegistry Actor. 

1.Lets declare the controller method in the class Routes. 



```
def getMeta: Future[String] =
  tileActor ? GetMetaData
```
Sends GetMetaData message to the Tile Actor. 
`?` is the operator to ask an actor, and it expects an asynchronous reply.
The return type of the function is a `Future` of type `[String]`

2.Append akkaHTTP routing directive in var fileRoutes  

Add the route with the below syntax. Ensure to use ~ at the end to append underlying route directives. 
```
val fileRoutes: Route = {
  ...
  ...
  ... CORS and Failure Handling functions
  ...
cors(){
  handleErrors {
    pathPrefix("meta")
      {
        pathEnd {
          get{
            complete(StatusCodes.OK,getMeta)
          }
        }
      } ~
  ...
  ...
  ... "/tiles/" and "/files/" endpoints
  ...
    }
  }
}
```

#### TileActor.scala

3.Now lets define a behaviour for the GetMetaData message in the TileActor

In the `TileActor` object, create case class for the behavior and extend TileCommand trait.
```
final case class GetMetaData(replyTo: ActorRef[String]) extends TileCommand
```


4.In the apply() method of the object, add the following case to `Behaviors.ReceiveMessage {}` method 

```
def apply(): Behavior[TileCommand] = Behaviors.setup {
    context: ActorContext[TileCommand] => Behaviors.receiveMessage {
        case GetMetaData(replyTo) =>
            //GET METADATA
            replyTo ! "My new BeastApi endpoint!"
            Behaviors.same
    }
}
```

Lets run a curl test

```
curl http://localhost:8080/meta
```
Response:
```
Status 200

My new BeastApi endpoint!
```

