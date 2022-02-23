

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.{Await, Future}
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import FileRegistry._

class FileRoutes(fileRegistry: ActorRef[FileRegistry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._
  //#file-route-class

  //#implicit default timeout value for all requests
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getFiles: Future[Files] =
    fileRegistry.ask(GetFiles)

  def createFile(file: File): Future[FileActionPerformed] = {

    println(system.printTree)

    fileRegistry.ask(CreateFile(file, _))
    //Await.result(hdfsRegistry.ask(startHDFS),)
  }

  def getFile(filename: String): Future[File] =
    fileRegistry.ask(GetFile(filename, _))


  //#all-routes

  val fileRoutes: Route =
    pathPrefix("files") {
      concat(
        //#files-create-and-list
        pathEnd {
          concat(
            get {
              complete(getFiles)
            },
            post {
              entity(as[File]) { file =>
                onSuccess(createFile(file)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
//        pathPrefix("query"){
//          path(Segment) {query =>
//            concat(
//              get {
//                rejectEmptyResponse {
//                  onSuccess(runQuery(query)) { response =>
//                    complete(StatusCodes.OK, response)
//              }
//            )
//          }
//        },
        // #return file requested
        path(Segment) { filename =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getFile(filename)) { response =>
                  complete(StatusCodes.OK, response)
                }
              }
            }
          )
        })
    }
}
