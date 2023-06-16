package actors

//#file-registry-actor
import akka.actor.typed.receptionist.Receptionist.{Find, Listing}
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout
import edu.ucr.cs.bdlab.beast.ReadWriteMixinFunctions
import edu.ucr.cs.bdlab.beast.geolite.IFeature
import models.DataFileDAL
import org.apache.commons.io.FileUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode

import java.io.{File, FileNotFoundException}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import utils.SparkFactory.{sparkContext, sparkSession}

import scala.collection.mutable.ArrayBuffer

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
  final case class CreateRTreeIndex(file: DataFile, replyTo: ActorRef[FileActionPerformed]) extends Command // TODO check if only filename is required?
  final case class StartSummaryGen(filename: String, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class GetSummaryStatus(filename: String, replyTo: ActorRef[String]) extends Command
  final case class GetSummary(filename: String, replyTo: ActorRef[GeneratedSummary]) extends Command
  final case class StartQuery(query:Query, replyTo: ActorRef[FileActionPerformed]) extends Command
  final case class CreateFileFromQuery(sourceQuery:SourceQuery, replyTo: ActorRef[String]) extends Command // TODO async? hdfs actor?


  final case class CreateDataFrameFromSource(listing: Listing, file: DataFile) extends Command
  final case class CreateVisualizationIndex(listing: Listing, file: DataFile) extends Command
  final case class CreateRTreeIndexHDFS(listing: Listing, file: DataFile) extends Command
  final case class CreateSummary(filename: String) extends Command
  final case class StartQueryInHDFS(listing: Listing, query:Query) extends Command


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

        case CreateRTreeIndex(file, replyTo) =>
          implicit val timeout: Timeout = 1.second
          context.ask(context.system.receptionist,Find(HdfsActor.HdfsKey))
          {
            case Success(listing:Listing) =>
              FileRegistry.CreateRTreeIndexHDFS(listing,file)
            case Failure(_)=>
              FileRegistry.FileActionPerformed("No HDFS Actor, could not download dataset")
          }
          replyTo ! FileActionPerformed(s"rtree_index_started")
          Behaviors.same

        case StartSummaryGen(filename, replyTo) =>
          println("actors.FileRegistry: Starting file summary generation") // TODO remove?
          implicit val timeout: Timeout = 1.second
          context.self ! CreateSummary(filename)
          replyTo ! FileActionPerformed("summarization_started")
          Behaviors.same

        case CreateSummary(filename) =>
          println("actors.FileRegistry: Creating file summary")
          try {
            val input = "data/datasource/" + filename
            val inputFeatures = sparkContext.shapefile(input)
            val summary = inputFeatures.summary

            var return_map: Map[String, Any] = Map()
            val size = if (summary.size <= Integer.MAX_VALUE) summary.size.toInt else summary.size
            return_map += ("size" -> size)
            val num_features = if (summary.numFeatures <= Integer.MAX_VALUE) summary.numFeatures.toInt else summary.numFeatures
            return_map += ("num_features" -> num_features)
            val num_points = if (summary.numPoints <= Integer.MAX_VALUE) summary.numPoints.toInt else summary.numPoints
            return_map += ("num_points" -> num_points)
            return_map += ("geometry_type" -> summary.geometryType.toString)

            val mbr = new Array[Double](4)
            mbr(0) = summary.getMinCoord(0)
            mbr(1) = summary.getMinCoord(1)
            mbr(2) = summary.getMaxCoord(0)
            mbr(3) = summary.getMaxCoord(1)
            return_map += ("extent" -> mbr)

            val avgSideLength = new Array[Double](2)
            avgSideLength(0) = summary.averageSideLength(0)
            avgSideLength(1) = summary.averageSideLength(1)
            return_map += ("avg_sidelength" -> avgSideLength)
            println(summary.averageSideLength(0)) // TODO remove

            // 2- Add attribute information

            val inputFeatures2: RDD[IFeature] = sparkContext.shapefile(input)
            val sampleFeature: IFeature = inputFeatures2.first // TODO change var name
            if (sampleFeature.length > 1) {
              val attributes = ArrayBuffer[Map[String, String]]()
              import scala.collection.JavaConversions._
              for (iAttr <- sampleFeature.iNonGeomJ) {
                var attribute: Map[String, String] = Map[String, String]()
                attribute += ("name" -> sampleFeature.getName(iAttr))
                val value = sampleFeature.get(iAttr)
                val valueClass = if (value == null) null
                else value.getClass
                var `type`: String = null
                if (value == null) `type` = "unknown"
                else if (valueClass eq classOf[String]) `type` = "string"
                else if ((valueClass eq classOf[Integer]) || (valueClass eq classOf[Long])) `type` = "integer"
                else if ((valueClass eq classOf[Float]) || (valueClass eq classOf[Double])) `type` = "number"
                else if (valueClass eq classOf[Boolean]) `type` = "boolean"
                else `type` = "unknown"
                attribute += ("type" -> `type`)
                attributes += attribute
              }
              return_map += ("attributes" -> attributes.toArray)
            }
            else
              return_map += ("attributes" -> null)

            println(return_map.mkString("{", ",", "}"))

            // Store to H2DB
            DataFileDAL.update_summary(filename, return_map)
            DataFileDAL.update_summary_status(filename, summary_status = "summarized")
            println("actors.FileRegistry: File summary created!")
            Behaviors.same
          } catch {
            case e: Exception =>
              println("actors.FileRegistry: Error :" + e.toString)
              DataFileDAL.update_summary_status(filename, summary_status = "error")
              Behaviors.same
          }

        case GetSummaryStatus(filename, replyTo) =>
          try {
            val ss = DataFileDAL.get_summary_status(filename)
            val summary_status = ss.get
            var returnVal = "false"
            if (summary_status == "summarized") returnVal = "true"
            else if (summary_status == "error") returnVal = "error_in_summary"
            replyTo ! returnVal
          } catch {
            case e: Exception =>
              println("actors.FileRegistry: Error :" + e.toString)
              replyTo ! "error_in_request"
          }
          Behaviors.same

        case GetSummary(filename, replyTo) =>
          try {
            val s = DataFileDAL.get_summary(filename)
            val summary = s.get
            val returnSummary = GeneratedSummary(summary._1, summary._2, summary._3, summary._4, summary._5, summary._6, summary._7)
            replyTo ! returnSummary
          } catch {
            case e: Exception =>
              println("actors.FileRegistry: Error :" + e.toString)
              e.printStackTrace()
              replyTo ! GeneratedSummary(-1, -1, -1, "", Array[Double](), Array[Double](), Array[Map[String, String]]())
          }
          Behaviors.same

        case StartQuery(query,replyTo) =>
          implicit val timeout: Timeout = 1.second
          context.ask(context.system.receptionist,Find(HdfsActor.HdfsKey))
          {
            case Success(listing:Listing) =>
            FileRegistry.StartQueryInHDFS(listing,query)
//              FileRegistry.CreateVisualizationIndex(listing,file)
            //FileRegistry.CreateVisualizationIndex(listing,file)
            case Failure(_)=>
              FileRegistry.FileActionPerformed("No HDFS Actor, could not query dataset")
          }
          replyTo ! FileActionPerformed(s"indexed")
          Behaviors.same

        case CreateFileFromQuery(sourceQuery, replyTo) =>
          try {
            val table1_path = sourceQuery.path + "/table1.txt"
            val table2_path = sourceQuery.path + "/table2.txt"

            val table1_df = sparkSession.read.options(Map("delimiter"->" ")).csv(table1_path)
            table1_df.createOrReplaceTempView("table1")
            val table2_df = sparkSession.read.options(Map("delimiter"->" ")).csv(table2_path)
            table2_df.createOrReplaceTempView("table2")

            val new_DF = sparkSession.sql(sourceQuery.query)

            // Save output as shapefile at data source
            val df_outPath = (sourceQuery.path.split('/').dropRight(1) :+ "download").mkString("/")
            val output_path =  new File(df_outPath)

            new_DF.write.format("shapefile").mode(SaveMode.Overwrite).save(output_path.getPath)

            // Add as dataset in beast-tools
            val df_outPath2 = "data/datasource/" + sourceQuery.filename
            val output_path2 =  new File(df_outPath2)
            new_DF.write.format("shapefile").mode(SaveMode.Overwrite).save(output_path2.getPath)

            val output_file = DataFile(filename = sourceQuery.filename, filetype = "shapefile", filestatus = "partitioned", filesource = df_outPath2 )
            DataFileDAL.insert(output_file)

            println( "actors.FileRegistry: Query output saved as shapefile at location: " + df_outPath )
            replyTo ! df_outPath
          } catch {
            case e: Exception =>
              println("actors.FileRegistry: Error :" + e.toString)
              e.printStackTrace()
              replyTo ! "error"
          }
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

        case CreateRTreeIndexHDFS(listing,file) =>
          println("actors.FileRegistry: Sending a create r-tree index message")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          hdfsActor.foreach { m =>
            m ! HdfsActor.CreateRTreeIndexFromDF(file)
          }
          Behaviors.same

        case StartQueryInHDFS(listing,query) =>
          println("actors.FileRegistry: Started running query")
          val instances: Set[ActorRef[HdfsActor.HdfsCommand]] =
            listing.serviceInstances(HdfsActor.HdfsKey)
          hdfsActor = instances.headOption
          //println(hdfsActor)
          hdfsActor.foreach { m =>
            //  m ! HdfsActor.PartitionToHDFS(file) #RDD API
            m ! HdfsActor.StartQueryAndSave(query)
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






