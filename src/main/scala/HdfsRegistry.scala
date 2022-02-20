
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior,ActorContext,Behaviors}
import akka.actor.typed.receptionist.{Receptionist,ServiceKey}

object HdfsRegistry {
  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description:String) extends HdfsCommand
  final case class WriteToHdfs(file:File) extends  HdfsCommand
  case class SpeakText(msg: String) extends HdfsCommand
  val HdfsKey: ServiceKey[HdfsCommand] = ServiceKey("HDFS_ACTOR")

  def apply(): Behavior[HdfsCommand] = Behaviors.setup{
  context: ActorContext[HdfsCommand] =>
    context.system.receptionist ! Receptionist.Register(HdfsKey,context.self)
    println("Hdfs Actor Born!")
    //hdfs_registry
    Behaviors.receiveMessage {
      case SpeakText(msg) =>
        println(s"HdfsRegistry: got a msg: $msg")
        Behaviors.same

      case WriteToHdfs(file:File) =>
        //case WriteToHdfs(replyTo) => {
        val spark = SparkSession
          .builder
          .appName("HdfsTest")
          .master("local[*]").getOrCreate()
        val sparkContext = spark.sparkContext
        try {
          println("STARTED SPARK JOB")
          //val fileURI = "/Users/abraham/Downloads/Riverside_WaterDistrict2.csv"
          DataFileDAL.update_status(file.filename,"downloading")
          val partitioned_data:SpatialRDD = sparkContext.geojsonFile(file.filesource).spatialPartition(classOf[RSGrovePartitioner])
          partitioned_data.saveAsShapefile(filename="partitioned_data")
          println("partitioning finished")
          DataFileDAL.update_status(file.filename,"partitioned")

          //sparkContext.
          //              data.plotImage(2000,2000,"~/Desktop/output.png")
          //              val df = data.toDataFrame(spark)

          //df.show()

          println("Success")
          HDFSActionPerformed("Success")
          Behaviors.same
        } catch {
          case e: NoClassDefFoundError =>
            println("Could not get spark session" + e.toString)
            HDFSActionPerformed("Failure")
          case _: Throwable =>
            println("Error" + _)
            HDFSActionPerformed("Failure")
        } finally {
          //println("Good Bye!")
          spark.stop()
          HDFSActionPerformed("Exit")
          Behaviors.same
        }
        //replyTo ! HDFSActionPerformed(s"FILE UPLOAD STARTED")
        Behaviors.same
    }
}


}


