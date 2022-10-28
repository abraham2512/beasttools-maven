package actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.DataFileDAL
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.io.{SpatialCSVSource, SpatialReader}
import edu.ucr.cs.bdlab.beast.io.SpatialReader.{dataFrameToSpatialRDD, readInput}
import edu.ucr.cs.bdlab.davinci.{GeometricPlotter, MultilevelPlot}
import org.apache.spark.sql.SaveMode
import utils.SparkFactory.{sparkContext, sparkSession}

import scala.reflect.io.{Directory, File}


object HdfsActor {

  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description: String) extends HdfsCommand

  val HdfsKey: ServiceKey[HdfsCommand] = ServiceKey("HDFS_ACTOR")

  final case class CreateVizIndexFromDF(file: DataFile) extends HdfsCommand
  final case class CreateDFSource(file: DataFile) extends  HdfsCommand
  final case class SpeakText(msg: String) extends HdfsCommand

  def apply(): Behavior[HdfsCommand] = Behaviors.setup {
    context: ActorContext[HdfsCommand] =>
      context.system.receptionist ! Receptionist.Register(HdfsKey, context.self)
      println("actors.HdfsRegistry: Hdfs Actor awake")

      Behaviors.receiveMessage {

        case SpeakText(msg) =>
          println(s"actors.HdfsActor: got a msg: $msg")
          Behaviors.same

        case CreateDFSource(file) =>
          println("actors.HdfsActor: starting datasource API")
          try {
            val input = file.filesource
            val input_df  = sparkSession.read.format("geojson").load(input)
            val schema = input_df.schema
            println(schema.size)
            println(schema.fields.mkString(","))
            println(input_df.count())
            val df_outPath = "data/datasource/" + file.filename
            if (Directory(df_outPath).exists){
              File(df_outPath).deleteRecursively()
              println("actors.HdfsActor: Existing folder with same name deleted")
            }
            input_df.write.format("geojson").mode(SaveMode.Overwrite).save(df_outPath)
            println("actors.HdfsActor: Dataframes created with Datasource API")
            DataFileDAL.update_status(file.filename, filestatus = "partitioned")
          }
          catch {
            case e:Exception =>
              println("actors.HdfsActor: "+e.toString)
              DataFileDAL.update_status(file.filename, filestatus = "error")
          }
          Behaviors.same

        case CreateVizIndexFromDF(file) =>
          try {
            println("actors.HdfsActor: Converting file " + file.filename + " to RDD")
            val input_path = "data/datasource/" + file.filename
            val input_df  = sparkSession.read.format("geojson").load(input_path)
            val features = SpatialReader.dataFrameToSpatialRDD(input_df)
            println("actors.HdfsActor: RDD loaded from "+ file.filename)
            val opts = new BeastOptions(false)
            opts.set(MultilevelPlot.ImageTileThreshold, 0)
            opts.set("mercator", true)
            opts.set("stroke", "blue")
            opts.set("data-tiles", true)
            opts.set("iformat", "rtree")
            opts.set("data", "../../datasource/" + file.filename)
            //opts.set("threshold","1m")
            val outPath = "data/viz/" + file.filename
            val inputPath = " "
            MultilevelPlot.plotFeatures(features, levels = 0 until 16, classOf[GeometricPlotter], inputPath, outPath, opts)
            println("actors.HdfsActor: Dataset plotted successfully")
            DataFileDAL.update_status(file.filename, filestatus = "indexed")
            HDFSActionPerformed("Success")
            Behaviors.same
          } catch {
            case e: NoClassDefFoundError =>
              println("actors.HdfsActor: Could not get spark session" + e.toString)
              HDFSActionPerformed("Failure")
              Behaviors.same
            case e: Exception =>
              println("actors.HdfsActor: Error :" + e.toString)
              DataFileDAL.update_status(file.filename, filestatus = "error")
              HDFSActionPerformed("Failure")
              Behaviors.same
          } finally {
            HDFSActionPerformed("Exit")
            Behaviors.same
          }
        case _ => println("actors.HdfsActor: default case")
          Behaviors.same
      }
  }


}
