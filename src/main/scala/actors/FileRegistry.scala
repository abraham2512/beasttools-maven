package actors

//#file-registry-actor
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import models.DataFileDAL
import org.apache.commons.io.FileUtils

import java.io.{File, FileNotFoundException}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object FileRegistry {
  //#actor protocol
  sealed trait Command
  final case class FileActionPerformed(description:String) extends Command

  val FileKey: ServiceKey[Command] = ServiceKey("FILE_ACTOR")

  //final case class Error(error: String) extends Command

  final case class CreateFile(file: DataFile, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class GetFiles(replyTo: ActorRef[DataFiles]) extends Command
  final case class GetFile(filename: String,replyTo: ActorRef[DataFile]) extends Command
  final case class DeleteFile(filename: String, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class CreateIndex(file: DataFile, replyTo: ActorRef[FileActionPerformed]) extends Command

  final case class CreateDataFrameFromSource(listing: Listing, file: DataFile) extends Command
  final case class CreateVisualizationIndex(listing: Listing, file: DataFile) extends Command

  def apply(): Behavior[Command]  = Behaviors.setup {
    println("actors.FileRegistry: actors.Routes awake")
    DataFileDAL()
    startup_clean() //removes corrupt datasets on startup
    var hdfsActor:Option[ActorRef[HdfsActor.HdfsCommand]] = None

    context: ActorContext[Command] =>

      Behaviors.receiveMessage {
        // CREATE FILE implemented here
        case CreateFile(file, replyTo) =>
          //CHECK IF FILE EXISTS IN DB
          val f: Seq[(String, String, String, String)] = DataFileDAL.get_all()
          val files = f.map(f => f._1)
          if(files contains file.filename){
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
                FileRegistry.CreateDataFrameFromSource(listing,file)
                //FileRegistry.CreateVisualizationIndex(listing,file)
              case Failure(_)=>
                FileRegistry.FileActionPerformed("No HDFS Actor, could not download dataset")
            }
            replyTo ! FileActionPerformed(s"created")
            println("actors.FileRegistry: Database insert complete!")
            //val f = DataFileDAL.get_uuid(file.filename)
            //println(f)
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
            //println("actors.FileRegistry: Database get all complete!")
          Behaviors.same

        case DeleteFile(filename,replyTo) =>
          delete_dataset(filename)
          replyTo ! FileActionPerformed("deleted")
          Behaviors.same

        case CreateIndex(file, replyTo) =>
          implicit val timeout: Timeout = 1.second
          context.ask(context.system.receptionist,Find(HdfsActor.HdfsKey))
          {
            case Success(listing:Listing) =>
              FileRegistry.CreateVisualizationIndex(listing,file)
            //FileRegistry.CreateVisualizationIndex(listing,file)
            case Failure(_)=>
              FileRegistry.FileActionPerformed("No HDFS Actor, could not download dataset")
          }
          replyTo ! FileActionPerformed(s"indexed")
          Behaviors.same





        //MESSAGES TO HDFS ACTOR
        case CreateDataFrameFromSource(listing,file) =>
          println("actors.FileRegistry: Sending a CreateDataFrameFromSource message")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          //println(hdfsActor)
          hdfsActor.foreach { m =>
          //  m ! HdfsActor.PartitionToHDFS(file) #RDD API
            m ! HdfsActor.CreateDFSource(file)
          }
          Behaviors.same

        case CreateVisualizationIndex(listing,file) =>
          println("actors.FileRegistry: Sending a create r-tree index message")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          //println(hdfsActor)
          hdfsActor.foreach { m =>
            //  m ! HdfsActor.PartitionToHDFS(file) #RDD API
            m ! HdfsActor.CreateVizIndexFromDF(file)
          }
          Behaviors.same

        //DEFAULT
        case _ =>
          print("actors.FileRegistry: default case")
          Behaviors.same

      }
  }
  def startup_clean(): Unit = {
    val files = DataFileDAL.get_all()
    for ( file <- files){
      if(file._4 != "indexed"){
        delete_dataset(file._1)
      }
    }
  }

  def delete_dataset(filename: String): Unit = {
    DataFileDAL.delete_file(filename)
    //println("File Deleted")
    val indexpath = "data/indexed/"+filename
    val vizpath = "data/viz/"+filename

    try {
      FileUtils.deleteDirectory(new File(indexpath))
      FileUtils.deleteDirectory(new File(vizpath))
    } catch {
      case e: FileNotFoundException =>
        println("actors.FileRegistry: File does not exist" + e.toString)
    } finally {
      println(s"actors.FileRegistry: $filename deleted")
    }
  }
}






