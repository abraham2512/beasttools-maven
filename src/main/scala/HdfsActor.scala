
import akka.actor.typed.Behavior
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.davinci.{GeometricPlotter, MultilevelPlot}


object HdfsActor {
  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description:String) extends HdfsCommand
  final case class PartitionToHDFS(file:DataFile) extends  HdfsCommand
  //final case class RunQuery(file:File,query: String, replyTo:ActorRef[HDFSActionPerformed])
  final case class PlotTile() extends HdfsCommand
  final case class SpeakText(msg: String) extends HdfsCommand
  val HdfsKey: ServiceKey[HdfsCommand] = ServiceKey("HDFS_ACTOR")

  def apply(): Behavior[HdfsCommand] = Behaviors.setup {
  context: ActorContext[HdfsCommand] =>
    context.system.receptionist ! Receptionist.Register(HdfsKey,context.self)
    println("HdfsRegistry: Hdfs Actor Born!")

    Behaviors.receiveMessage {

      case SpeakText(msg) =>
        println(s"HdfsRegistry: got a msg: $msg")
        Behaviors.same

      case PartitionToHDFS(file) =>
        val spark = SparkSession
          .builder
          .appName("HdfsTest")
          .master("local[*]").getOrCreate()
        val sc = spark.sparkContext
        try {
          println("HdfsRegistry: Started partition job for file "+ file.filename)
          DataFileDAL.update_status(file.filename,filestatus="downloading")

          //Partitioning the data and storing as r-tree
          val partitioned_data:SpatialRDD = sc.spatialFile(file.filesource).spatialPartition(classOf[RSGrovePartitioner])
          partitioned_data.writeSpatialFile(filename= "data/indexed/" + file.filename,oformat = "rtree")
          println("HdfsRegistry: Partitioning complete")
          DataFileDAL.update_status(file.filename,filestatus="partitioned")

          //Building multilevel visualization
          val features = sc.spatialFile(filename= "data/indexed/" + file.filename)
//            .plotPyramid(outPath="viz/"+file.filename, 20,
//              opts = Seq("data-tiles" -> true, "mercator" -> true, "stroke" -> "blue","threshold"->"1m"))
          val inputMBR = features.summary
          //val pyramid = new SubPyramid(inputMBR,0,2)
          val opts = new BeastOptions(false)
          opts.set(MultilevelPlot.ImageTileThreshold, 0)
          opts.set("mercator",true)
          opts.set("stroke", "blue")
          opts.set("data-tiles",true)
          opts.set("iformat","rtree")
          opts.set("data","../../indexed/" + file.filename)
          //opts.set("threshold","20m")
          //features.plotPyramid(outPath="viz/"+file.filename,numLevels = 4,opts = opts)
          val outPath="data/viz/"+file.filename
          val inputPath=" "

          MultilevelPlot.plotFeatures(features, levels=0 until 4, classOf[GeometricPlotter], inputPath, outPath, opts)


          println("HdfsRegistry: Multilevel indexing complete")
          DataFileDAL.update_status(file.filename,filestatus="indexed")
          HDFSActionPerformed("Success")
          Behaviors.same
        } catch {
          case e: NoClassDefFoundError =>
            println("HdfsRegistry: Could not get spark session" + e.toString)
            HDFSActionPerformed("Failure")
            Behaviors.same
          case _: Exception  =>
            println("Error"+ _)
            DataFileDAL.update_status(file.filename,filestatus="error")
            HDFSActionPerformed("Failure")
            Behaviors.same
        } finally {
          //println("Good Bye!")
          spark.stop()
          HDFSActionPerformed("Exit")
          Behaviors.same
        }
      case _ => println("default case")
        Behaviors.same
    }
  }


}

