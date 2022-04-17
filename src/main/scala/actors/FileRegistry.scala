package actors

//#file-registry-actor
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import models.DataFileDAL
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object FileRegistry {
  //#actor protocol
  sealed trait Command
  final case class FileActionPerformed(description:String) extends Command
  final case class Error(error: String) extends Command

  final case class CreateFile(file: DataFile, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class GetFiles(replyTo: ActorRef[DataFiles]) extends Command
  final case class GetFile(filename: String,replyTo: ActorRef[DataFile]) extends Command
  final case class DeleteFile(filename: String, replyTo: ActorRef[FileActionPerformed]) extends Command

  final case class SendHadoopTask(listing: Listing, file: DataFile) extends Command

  def apply(): Behavior[Command]  = Behaviors.setup {
    println("actors.FileRegistry: actors.Routes awake")
    DataFileDAL()
    var hdfsActor:Option[ActorRef[HdfsActor.HdfsCommand]] = None

    context: ActorContext[Command] =>

      Behaviors.receiveMessage {
        // CREATE FILE implemented here
        case CreateFile(file, replyTo) =>
          //CHECK IF FILE EXISTS IN DB
          val f: Seq[(String, String, String, String)] = DataFileDAL.get_all()
          val files = f.map(f => f._1)
          if(files contains(file.filename)){
            println("actors.FileRegistry: File exists, doing nothing")
            replyTo ! FileActionPerformed(s"exists")
          }
          else {
            println("actors.FileRegistry: Inserted file, queuing spark download")
            DataFileDAL.insert(file)
            implicit val timeout: Timeout = 1.second
            context.ask(context.system.receptionist,Find(HdfsActor.HdfsKey))
            {
              case Success(listing:Listing) =>
                FileRegistry.SendHadoopTask(listing,file)
              case Failure(_)=>
                FileRegistry.Error("No HDFS Actor")
            }
            replyTo ! FileActionPerformed(s"created")
            println("actors.FileRegistry: Database insert complete!")
          }

          Behaviors.same

        //GET FILE Implemented here
        case GetFile(filename, replyTo) =>
          try {
            val f = DataFileDAL.get(filename)
            val file_data = f.get
            val returnFile = DataFile(file_data._1, file_data._2, file_data._3, file_data._4)
            replyTo ! returnFile
          } catch {
            case e: NoSuchElementException =>
              println("actors.FileRegistry: No Such Element" + e.toString)
              replyTo ! DataFile("", "", "", "")
          }
          Behaviors.same

        //GET ALL FILES implemented here
        case GetFiles(replyTo) =>
            val f: Seq[(String, String, String, String)] = DataFileDAL.get_all()
            val files_data = f.map(f => DataFile(f._1, f._2, f._3, f._4))
            replyTo ! DataFiles(files_data)
            println("actors.FileRegistry: Database get all complete!")
          Behaviors.same

        case DeleteFile(filename,replyTo) =>
          DataFileDAL.delete_file(filename)
          println("File Deleted")
          replyTo ! FileActionPerformed("deleted")
          Behaviors.same
        //MESSAGE TO HDFS ACTOR
        case SendHadoopTask(listing,file) =>
          println("actors.FileRegistry: Sending a SendHadoopTask message")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          println(hdfsActor)
          hdfsActor.foreach { m =>
            m ! HdfsActor.PartitionToHDFS(file)
          }
          Behaviors.same

        //DEFAULT
        case _ =>
          print("actors.FileRegistry: DEFAULT CASE")
          Behaviors.same

      }
  }

}






