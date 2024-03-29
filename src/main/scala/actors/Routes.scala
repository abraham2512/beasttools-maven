package actors

import actors.FileRegistry._
import actors.TileActor._
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives
import org.apache.spark.sql.SaveMode

import scala.concurrent.Future
import scala.reflect.io.File

//#case classes
case class DataFile(filename:String, filetype:String, filesource:String, filestatus:String) {
  def apply(filename:String,filetype:String,filesource:String,filestatus:String): DataFile = { DataFile(filename,filetype,filesource,filestatus)}
}
final case class DataFiles(files: Seq[DataFile])

case class Query(dataset:String, query:String, saveMode:String){
  def apply(dataset:String, query:String, saveMode: String): Query = { Query(dataset,query,saveMode) }
}

case class GeneratedSummary(size: Long, num_features: Long, num_points: Long, geometry_type: String, extent: Array[Double], avg_sidelength: Array[Double], attributes: Array[Map[String, String]]) {
  def apply(size: Long, num_features: Long, num_points: Long, geometry_type: String, extent: Array[Double], avg_sidelength: Array[Double], attributes: Array[Map[String, String]]): GeneratedSummary = { GeneratedSummary(size, num_features, num_points, geometry_type, extent, avg_sidelength, attributes)}
}

case class SourceQuery(query:String, path:String, filename:String){
  def apply(query:String, path: String, filename:String): SourceQuery = { SourceQuery(query, path, filename) }
}

//The Routing Logic class
class Routes(fileRegistry: ActorRef[FileRegistry.Command], tileActor: ActorRef[TileActor.TileCommand])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import utils.JsonFormats._

  //#implicit default timeout value for all requests
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  //The methods below send messages to actors respective to the API request
  def getFiles: Future[DataFiles] =
    fileRegistry.ask(GetFiles)

  def createFile(file: DataFile): Future[FileActionPerformed] =
    fileRegistry.ask(CreateFile(file, _))

  def deleteFile(filename:String): Future[FileActionPerformed] =
    fileRegistry.ask(DeleteFile(filename,_))

  def getFile(filename: String): Future[DataFile] =
    fileRegistry.ask(GetFile(filename, _))

  def createRTreeIndex(file: DataFile): Future[FileActionPerformed] =
    fileRegistry.ask(CreateRTreeIndex(file,_))

  def createIndex(file: DataFile): Future[FileActionPerformed] =
    fileRegistry.ask(CreateIndex(file,_))

  def generateSummary(filename: String): Future[FileActionPerformed] =
    fileRegistry.ask(StartSummaryGen(filename,_))

  def getSummaryStatus(filename: String): Future[String] =
    fileRegistry.ask(GetSummaryStatus(filename,_))

  def getSummary(filename: String): Future[GeneratedSummary] =
    fileRegistry.ask(GetSummary(filename,_))

  def getTile(dataset: String,tile: (String,String,String)): Future[Array[Byte]] =
    tileActor.ask(GetTile(dataset,tile,_))

  def getTileMeta(dataset: String, mbrString: String): Future[String] =
    tileActor.ask(GetMetaData(dataset,mbrString,_))

  def runQuery(query: Query): Future[FileActionPerformed] =
    fileRegistry.ask(StartQuery(query,_))

  def createFileFromQuery(sourceQuery: SourceQuery): Future[String] =
    fileRegistry.ask(CreateFileFromQuery(sourceQuery, _))

  //This function handles situations when the tile could not be generated on the fly
  private val tileOnTheFlyHandler = RejectionHandler.newBuilder
    .handleNotFound { complete((StatusCodes.NotFound, "Error: Could not get tile on the fly!"))
       }
    .handle { case ValidationRejection(msg, _) => complete((StatusCodes.InternalServerError, msg)) }
    .result()

  //#all-routes
  val fileRoutes: Route =  {
    import CorsDirectives._
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)

    // Exception handler
    val exceptionHandler = ExceptionHandler {
      case e: NoSuchElementException => complete(StatusCodes.NotFound -> e.getMessage)
    }

    // Combining the two handlers only for convenience
    val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)

    handleErrors{
      cors(){
        handleErrors {
          pathPrefix("ui"){
            path(Segment) { filename =>
              get{
              getFromResource("static/"+filename)
              }
            }
          } ~
          pathPrefix("meta"){
            pathEnd {
              get{
                parameters("dataset","mbrString"){(dataset,mbrString) =>
                  onSuccess(getTileMeta(dataset,mbrString)) { metadata =>
                    complete(StatusCodes.OK,metadata)
                  }
                }
              }}
          } ~
          pathPrefix("tiles"){
            handleRejections(tileOnTheFlyHandler){
              parameters("dataset","z","x","y") { (dataset,z,x,y) =>
                val resourcePath = "data/viz/"+dataset+"/"+"tile-"+z+"-"+x+"-"+y+".png"
                if (File(resourcePath).exists) {
                  getFromDirectory(directoryName=resourcePath)
                } else {
                  onSuccess(getTile(dataset, (z,x,y))) { byteStream =>
                    complete(StatusCodes.OK, HttpEntity(MediaTypes.`image/png`,byteStream))
                  }
                }
              }
            }
          } ~
            pathPrefix("query") {
              concat(
                pathEnd {
                  post {
                    entity(as[Query]) {query =>
                      onSuccess(runQuery(query)) { performed =>
                        complete((StatusCodes.Accepted))
                      }

                    }
                  }
                }
              )
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
                      entity(as[DataFile]) { file =>
                        onSuccess(createFile(file)) { performed =>
                          complete((StatusCodes.Accepted, performed))
                        }
                      }
                    },
                    put {
                      entity(as[DataFile]) { file =>
                        onSuccess(createIndex(file)) { performed =>
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
                    },
                    delete {
                      rejectEmptyResponse {
                        onSuccess(deleteFile(filename)) { response =>
                          complete(StatusCodes.Accepted, response)
                        }
                      }
                    }
                  )
                }
              )
            } ~
          pathPrefix("index"){
            pathEnd {
              post {
                entity(as[DataFile]) { file =>
                  onSuccess(createRTreeIndex(file)) { performed =>
                    complete((StatusCodes.Accepted, performed))
                  }
                }
              }
            }
          } ~
          pathPrefix("summary"){
            pathEnd {
              concat(
                put {
                  parameters("filename") { filename =>
                    onSuccess(generateSummary(filename)) { performed =>
                      complete((StatusCodes.Accepted, performed)) // TODO check for errors?
                    }
                  }
                }
                ,
                get {
                  parameters("filename") { (filename) =>
                    onSuccess( getSummary(filename) ) { summary =>
                      complete(StatusCodes.OK, summary) // TODO handle errors
                    }
                  }
                }
              )
            }
          } ~
          pathPrefix("summary_status") {
            pathEnd {
              get {
                parameters("filename") { (filename) =>
                  onSuccess( getSummaryStatus(filename) ) { status =>
                    complete(StatusCodes.OK, status)
                  }
                }
              }
            }
          } ~
          pathPrefix("source_query") {
            concat(
              pathEnd {
                post {
                  entity(as[SourceQuery]) { sourceQuery =>
                    onSuccess(createFileFromQuery(sourceQuery)) { returnPath =>
                      complete(StatusCodes.OK, returnPath)
                    }

                  }
                }
              }
            )
          }
        }
      }
        }
      }
    }
  /* ROUTES TO RETRIEVE TILES */
//    pathPrefix("tiles"){
//      handleRejections(tileOnTheFlyHandler){
//        parameters("dataset","z","x","y") { (dataset,z,x,y) =>
//          val resourcePath = "data/viz/"+dataset+"/"+"tile-"+z+"-"+x+"-"+y+".png"
//          if (File(resourcePath).exists) {
//            getFromDirectory(directoryName=resourcePath)
//          } else {
//            onSuccess(getTile(dataset, (z,x,y))) { byteStream =>
//                complete(StatusCodes.OK, HttpEntity(MediaTypes.`image/png`,byteStream))
//            }
//          }
//        }
//      }
//    } ~
//    pathPrefix("files") {
//      concat(
//        /* ROUTES TO CREATE AND GET BIG DATA FILES */
//        pathEnd {
//          concat(
//            get {
//              complete(getFiles)
//            },
//
//            post {
//              entity(as[DataFile]) { file =>
//                onSuccess(createFile(file)) { performed =>
//                  complete((StatusCodes.Created, performed))
//                }
//              }
//
//            }
//          )
//        },
//        /* RETURN STATUS OF REQUESTED FILE */
//        path(Segment) { filename =>
//          concat(
//            get {
//              rejectEmptyResponse {
//                onSuccess(getFile(filename)) { response =>
//                  complete(StatusCodes.OK, response)
//                }
//              }
//            }
//          )
//        }
//      )
//    }
//}
//}
//
//trait CORSHandler{
//
//  private val corsResponseHeaders = List(
//    `Access-Control-Allow-Origin`.*,
//    `Access-Control-Allow-Credentials`(true),
//    `Access-Control-Allow-Headers`("Authorization",
//      "Content-Type", "X-Requested-With")
//  )
//
//  //this directive adds access control headers to normal responses
//  private def addAccessControlHeaders: Directive0 = {
//    respondWithHeaders(corsResponseHeaders)
//  }
//
//  //this handles preflight OPTIONS requests.
//  private def preflightRequestHandler: Route = options {
//    complete(HttpResponse(StatusCodes.OK).
//      withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE)))
//  }
//
//  // Wrap the Route with this method to enable adding of CORS headers
//  def corsHandler(r: Route): Route = addAccessControlHeaders {
//    preflightRequestHandler ~ r
//  }
//
//  // Helper method to add CORS headers to HttpResponse
//  // preventing duplication of CORS headers across code
//  def addCORSHeaders(response: HttpResponse):HttpResponse =
//    response.withHeaders(corsResponseHeaders)
//
//}