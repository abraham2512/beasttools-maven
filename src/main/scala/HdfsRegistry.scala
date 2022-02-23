
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.util.BeastServer

object HdfsRegistry {
  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description:String) extends HdfsCommand
  final case class PartitionToHDFS(file:File) extends  HdfsCommand
  //final case class RunQuery(file:File,query: String, replyTo:ActorRef[HDFSActionPerformed])
  case class SpeakText(msg: String) extends HdfsCommand
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
        val sparkContext = spark.sparkContext
        try {
          println("HdfsRegistry: Started partition job for file "+ file.filename)
          DataFileDAL.update_status(file.filename,filestatus="downloading")
          //Partitioning the data and storing as r-tree
          val partitioned_data:SpatialRDD = sparkContext.shapefile(file.filesource).spatialPartition(classOf[RSGrovePartitioner])
          partitioned_data.writeSpatialFile(filename= "partitioned_data/" + file.filename,oformat = "rtree")

          println("HdfsRegistry: Partitioning complete")
          DataFileDAL.update_status(file.filename,filestatus="partitioned")


          //Building multilevel visualization
          sparkContext.spatialFile(filename= "partitioned_data/" + file.filename)
            .plotPyramid(outPath="viz/"+file.filename, 20,
              opts = Seq("data-tiles" -> false, "mercator" -> true, "stroke" -> "blue", "threshold" -> 0))

          //new BeastServer().run(new BeastOptions(), null, null, sparkContext)

          println("HdfsRegistry: Multilevel indexing complete")
          DataFileDAL.update_status(file.filename,filestatus="indexed")




          HDFSActionPerformed("Success")
          Behaviors.same
        } catch {
          case e: NoClassDefFoundError =>
            println("HdfsRegistry: Could not get spark session" + e.toString)
            HDFSActionPerformed("Failure")
            Behaviors.same
//          case _: Throwable =>
//            println("HdfsRegistry: Error" + _)
//            HDFSActionPerformed("Failure")
//            Behaviors.same
          case _: Exception  =>
            println("Error"+ _)
            HDFSActionPerformed("Failure")
            Behaviors.same
        } finally {
          println("Good Bye!")
          spark.stop()
          HDFSActionPerformed("Exit")
          Behaviors.same
        }

//      case RunQuery(file,replyTo) =>
//
//        Behaviors.same
      case _ => println("default case")
        Behaviors.same
    }
  }


}


