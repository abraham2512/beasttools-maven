

//#file-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, DurationInt}
import scala.util.{Failure, Success}



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

  final case class SpeakToHDFS(listing: Listing,file: File) extends Command
  final case class Error(error: String) extends Command


  def apply(): Behavior[Command]  = Behaviors.setup {
    println("File Actor Born!")
    DataFileDAL()
    context: ActorContext[Command] =>
      var hdfs:Option[ActorRef[HdfsRegistry.HdfsCommand]] = None
      Behaviors.receiveMessage {
        case GetFile(filename, replyTo) =>
          try {
            val f = DataFileDAL.get(filename)
            val file_data = f.get
            val returnFile = File(file_data._1, file_data._2, file_data._3, file_data._4)
            replyTo ! returnFile
            Behaviors.same
          } catch {
            case e: NoSuchElementException =>
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
            val f: Seq[(String, String, String, String)] = DataFileDAL.get_all()
            val files_data = f.map(f => File(f._1, f._2, f._3, f._4))
            replyTo ! Files(files_data)
            Behaviors.same
          } finally {
            println("Database get all complete!")
          }
        // CREATE FILE implemented here
        case CreateFile(file, replyTo) =>
          try {
            println("Started File download")
            val f = DataFileDAL.insert(file)
            Await.result(f, Duration.Inf)

            implicit val timeout: Timeout = 1.second
            context.ask(context.system.receptionist,Find(HdfsRegistry.HdfsKey))
            {
              case Success(listing:Listing) =>
                FileRegistry.SpeakToHDFS(listing,file)
              case Failure(_)=>
                FileRegistry.Error("No HDFS Actor")
            }
            replyTo ! FileActionPerformed(s"File ${file.filename} created!")

            Behaviors.same
          } finally {
            println("Database insert complete!")
            Behaviors.same
          }
        case SpeakToHDFS(listing,file) =>
          println("Brain: Got a FoundTheMouth message")
          val instances: Set[ActorRef[HdfsRegistry.HdfsCommand]] =
            listing.serviceInstances(HdfsRegistry.HdfsKey)
          hdfs = instances.headOption
          println(hdfs)
          hdfs.foreach { m =>
            m ! HdfsRegistry.WriteToHdfs(file)
          }
          Behaviors.same

        case _ =>
          print("DEFAULT CASE")
          Behaviors.same

      }
  }

}






