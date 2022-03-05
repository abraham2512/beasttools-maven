//#file-registry-actor
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

//#case classes
case class File(filename:String, filetype:String, filesource:String, filestatus:String) {
  def apply(filename:String,filetype:String,filesource:String,filestatus:String): File = { File(filename,filetype,filesource,filestatus)}
}

final case class Files(files: Seq[File])

object FileRegistry {
  //#actor protocol
  sealed trait Command
  final case class FileActionPerformed(description:String)

  final case class GetFiles(replyTo: ActorRef[Files]) extends Command
  final case class GetFile(filename: String,replyTo: ActorRef[File]) extends Command

  final case class CreateFile(file: File, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class RunQuery(file:File,query:String,replyTo:ActorRef[FileActionPerformed]) extends Command

  final case class SpeakToHDFS(listing: Listing,file: File) extends Command
  final case class SpeakToQuery(listing: Listing,query:String,file: File) extends Command

  final case class Error(error: String) extends Command


  def apply(): Behavior[Command]  = Behaviors.setup {
    println("FileRegistry: File Actor Born!")
    DataFileDAL()
    var hdfsActor:Option[ActorRef[HdfsActor.HdfsCommand]] = None
    //var queryActor:Option[ActorRef[QueryActor.QueryCommand]] = None
    context: ActorContext[Command] =>

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
              println("FileRegistry: No Such Element" + e.toString)
              replyTo ! File("", "", "", "")
              Behaviors.same

          }
          finally {
            println("FileRegistry: Get File complete")
          }
        //GET ALL FILES implemented here
        case GetFiles(replyTo) =>
            val f: Seq[(String, String, String, String)] = DataFileDAL.get_all()
            val files_data = f.map(f => File(f._1, f._2, f._3, f._4))
            replyTo ! Files(files_data)
            println("FileRegistry: Database get all complete!")
            Behaviors.same

        // CREATE FILE implemented here
        case CreateFile(file, replyTo) =>
            println("FileRegistry: Inserted file, queuing spark download")
            DataFileDAL.insert(file)
            implicit val timeout: Timeout = 1.second
            context.ask(context.system.receptionist,Find(HdfsActor.HdfsKey))
            {
              case Success(listing:Listing) =>
                FileRegistry.SpeakToHDFS(listing,file)
              case Failure(_)=>
                FileRegistry.Error("No HDFS Actor")
            }
            replyTo ! FileActionPerformed(s"File ${file.filename} created!")
            println("FileRegistry: Database insert complete!")
            Behaviors.same

        //MESSAGE TO HDFS ACTOR
        case SpeakToHDFS(listing,file) =>
          println("FileRegistry: Sending a SpeakToHDFS message")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          println(hdfsActor)
          hdfsActor.foreach { m =>
            m ! HdfsActor.PartitionToHDFS(file)
          }

          Behaviors.same

//        case RunQuery(file,query,replyTo) =>
//          println("FileRegistry: Running Query spark download")
//          implicit val timeout: Timeout = 1.second
//          context.ask(context.system.receptionist,Find(QueryActor.QueryKey))
//          {
//            case Success(listing:Listing) =>
//              FileRegistry.SpeakToQuery(listing,query,file)
//            case Failure(_)=>
//              FileRegistry.Error("No Query Actor")
//          }
//          replyTo ! FileActionPerformed(s"Running query!")
//          Behaviors.same
//        //MESSAGE TO QUERY ACTOR
//
//        case SpeakToQuery(listing, file,query) =>
//          println("FileRegistry: Sending a SpeakToQuery message")
//          val instances: Set[ActorRef[QueryActor.QueryCommand]] =
//            listing.serviceInstances(QueryActor.QueryKey)
//          queryActor = instances.headOption
//          println(query)
//          queryActor.foreach { m =>
//            m ! QueryActor.SpeakText("HELLO QUERY")
//          }
//          Behaviors.same



        //DEFAULT
        case _ =>
          print("FileRegistry: DEFAULT CASE")
          Behaviors.same

      }
  }

}






