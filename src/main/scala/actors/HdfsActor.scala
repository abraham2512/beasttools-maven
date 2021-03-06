package actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.DataFileDAL
import edu.ucr.cs.bdlab.beast.{ReadWriteMixinFunctions, SpatialRDD}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner
import edu.ucr.cs.bdlab.davinci.{GeometricPlotter, MultilevelPlot}
import utils.SparkFactory.sc

object HdfsActor {

  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description: String) extends HdfsCommand

  val HdfsKey: ServiceKey[HdfsCommand] = ServiceKey("HDFS_ACTOR")

  final case class PartitionToHDFS(file: DataFile) extends HdfsCommand
  final case class SpeakText(msg: String) extends HdfsCommand

  def apply(): Behavior[HdfsCommand] = Behaviors.setup {
    context: ActorContext[HdfsCommand] =>
      context.system.receptionist ! Receptionist.Register(HdfsKey, context.self)
      println("actors.HdfsRegistry: Hdfs Actor awake")

      Behaviors.receiveMessage {

        case SpeakText(msg) =>
          println(s"actors.HdfsActor: got a msg: $msg")
          Behaviors.same

        case PartitionToHDFS(file) =>

          try {
            println("actors.HdfsActor: Started file " + file.filename)
            DataFileDAL.update_status(file.filename, filestatus = "downloading")

            //Partitioning the data and storing as r-tree
            val partitioned_data: SpatialRDD = sc.spatialFile(file.filesource).spatialPartition(classOf[RSGrovePartitioner])
            partitioned_data.writeSpatialFile(filename = "data/indexed/" + file.filename, oformat = "rtree")
            println("actors.HdfsActor: Partitioning complete")
            DataFileDAL.update_status(file.filename, filestatus = "partitioned")

            //Building multilevel visualization
            val features = sc.spatialFile(filename = "data/indexed/" + file.filename)
            val opts = new BeastOptions(false)
            opts.set(MultilevelPlot.ImageTileThreshold, 0)
            opts.set("mercator", true)
            opts.set("stroke", "blue")
            opts.set("data-tiles", true)
            opts.set("iformat", "rtree")
            opts.set("data", "../../indexed/" + file.filename)
            //opts.set("threshold","1m")
            val outPath = "data/viz/" + file.filename
            val inputPath = " "
            MultilevelPlot.plotFeatures(features, levels = 0 until 16, classOf[GeometricPlotter], inputPath, outPath, opts)

            println("actors.HdfsActor: Multilevel indexing complete")
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
              DataFileDAL.update_status(file.filename, filestatus = "Partitioning error, check log")
              HDFSActionPerformed("Failure")
              Behaviors.same
          } finally {
            //println("Good Bye!")
            //spark.stop()
            HDFSActionPerformed("Exit")
            Behaviors.same
          }
        case _ => println("actors.HdfsActor: default case")
          Behaviors.same
      }
  }


}
