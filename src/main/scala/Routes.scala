import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{RejectionHandler, Route, ValidationRejection}

import scala.concurrent.Future
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import FileRegistry._
import TileActor._
import akka.http.javadsl.server.PathMatcher4


class Routes(fileRegistry: ActorRef[FileRegistry.Command], tileActor: ActorRef[TileActor.TileCommand])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import JsonFormats._

  //#implicit default timeout value for all requests
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))


  def getFiles: Future[Files] =
    fileRegistry.ask(GetFiles)
  def createFile(file: File): Future[FileActionPerformed] = {
    println(system.printTree)
    fileRegistry.ask(CreateFile(file, _))
  }
  def getFile(filename: String): Future[File] =
    fileRegistry.ask(GetFile(filename, _))
  def getTile(tilename: String): Future[String] =
    tileActor.ask(GetTile(tilename,_))



  private val tileOnTheFlyHandler = RejectionHandler.newBuilder
    .handleNotFound { complete((StatusCodes.NotFound, "Going to get that on the fly!")) }
    .handle { case ValidationRejection(msg, _) => complete((StatusCodes.InternalServerError, msg)) }
    .result()


  //#all-routes

  val fileRoutes: Route =
  /* ROUTES TO RETRIEVE TILES */
    pathPrefix("tiles"){
      handleRejections(tileOnTheFlyHandler){
        parameters("dataset","z","x","y") { (dataset,z,x,y) =>
          println("viz/"+dataset+"/tile-"+z+"-"+x+"-"+y+".png")
          getFromDirectory(directoryName="viz/"+dataset+"/tile-"+z+"-"+x+"-"+y+".png")
        }
      }
    } ~
    pathPrefix("files") {
      concat(
        /* ROUTES TO CREATE AND GET BIG DATA FILES */
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
        /* RETURN STATUS OF REQUESTED FILE */
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
        }
      )
    }
}
