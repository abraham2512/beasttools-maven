

//#file-registry-actor
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.immutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}



//#case classes
case class File(filename:String, filetype:String, filesource:String, filestatus:String) {
  def apply(filename:String,filetype:String,filesource:String,filestatus:String): File = { File(filename,filetype,filesource,filestatus)}
}

final case class Files(files: Seq[File])

object FileRegistry {
  //#actor protocol
  sealed trait Command
  final case class GetFiles(replyTo: ActorRef[Files]) extends Command
  final case class GetFile(filename: String,replyTo: ActorRef[File]) extends Command

  final case class CreateFile(file: File, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class FileActionPerformed(description:String)


  def apply(): Behavior[Command] = {
    println("File Actor Born!")
    DataFileDAL()
    registry
  }

  private def registry: Behavior[Command] = {
    Behaviors.receiveMessage {
      //GET FILE with filename implemented here
      case GetFile(filename,replyTo)  =>
        try {
          val f = DataFileDAL.get(filename)
          val file_data = f.get
          val returnFile = File(file_data._1,file_data._2,file_data._3,file_data._4)
          replyTo ! returnFile

          HdfsRegistry.startHDFS(file_data._3)

          Behaviors.same
        } catch {
          case e : NoSuchElementException =>
            println("No Such Element" + e.toString)
            replyTo ! File("", "", "", "")
            Behaviors.same

        }
        finally {
          println("Get File complete")
        }

      //GET ALL FILES implemented here
      case GetFiles(replyTo) =>
        try {
          val f: Seq[(String,String,String,String)]= DataFileDAL.get_all()
          val files_data = f.map(f=> File(f._1,f._2,f._3,f._4))
          replyTo ! Files(files_data)
          Behaviors.same
        } finally {
          println("Database get all complete!")
        }

      // CREATE FILE implemented here
      case CreateFile(file,replyTo) =>
        try {
          println("Started File download")
          val f = DataFileDAL.insert(file)
          Await.result(f, Duration.Inf) //MOVE THIS TO DAL file
          replyTo ! FileActionPerformed(s"File ${file.filename} created!")
          HdfsRegistry.startHDFS(file.filesource)
          Behaviors.same
        } finally {
          //ASK HDFS Actor from here
          //val fileName = "/Users/abraham/Downloads/Riverside_WaterDistrict2.csv"
          println("Database insert complete!")
          Behaviors.same
        }
    }
  }

}






