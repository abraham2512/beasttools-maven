
import actors.{FileRegistry, HdfsActor, Routes, TileActor}
import akka.actor.typed.{ActorSystem, MailboxSelector}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

import java.io
import scala.reflect.io.{Directory, File}
import scala.util.{Failure, Success}

//#main-class
object StartApp {
  //#start-http-servers
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    // Akka HTTP still needs a classic ActorSystem to start
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
        println("Server online at http://"+ address.getHostString+":"+ address.getPort+"/ui/index.html")
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
      val rootBehavior = Behaviors.setup[Nothing] { context =>
        //val props = MailboxSelector.defaultMailbox()
        //println("Pre Actors Routine")
        //if (File("application.conf").exists) println("FOUND APP CONF") else println("NOPE")
        if(!Directory("data").exists){
          //new java.io.File("data/indexed").mkdirs
          new java.io.File("data/viz").mkdirs
          new java.io.File("data/datasource").mkdirs
          println("root: Creating rtree/tiles directories")
        }
        println("root: Starting actor system")
        val fileRegistryActor = context.spawn(FileRegistry(),name="FileRegistryActor",MailboxSelector.bounded(capacity = 100))
        context.watch(fileRegistryActor)
        val tileActor = context.spawn(TileActor(), name="TileActor",MailboxSelector.bounded(capacity = 100))
        context.watch(tileActor)
        val hdfsActor = context.spawn(HdfsActor(),name="HdfsActor",MailboxSelector.bounded(capacity = 100))
        context.watch(hdfsActor)

        val routes = new Routes(fileRegistryActor,tileActor)(context.system)
        startHttpServer(routes.fileRoutes)(context.system)
        Behaviors.empty
      }
      val system = ActorSystem[Nothing](rootBehavior, "BeastAkkaServer")
      system.printTree
      //#server-bootstrapping
  }
}
//#main-class
