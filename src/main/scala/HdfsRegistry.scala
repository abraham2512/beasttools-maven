
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.common.BeastOptions

import scala.collection.mutable

object HdfsRegistry {
  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description:String) extends HdfsCommand
  final case class WriteToHdfs(replyTo: ActorRef[HDFSActionPerformed]) extends  HdfsCommand

  def apply(): Behavior[HDFSActionPerformed] = {
    println("Hdfs Actor Born!")
    HDFSActionPerformed("START SUCCESS")
    Behaviors.empty
  }
  def startHDFS(fileURI:String):HDFSActionPerformed = {
    hdfs_registry(fileURI)
    HDFSActionPerformed("Starting Spark Job")
  }

  private def hdfs_registry(fileURI:String):HDFSActionPerformed = {

//    Behaviors.receiveMessage {

        //case WriteToHdfs(replyTo) => {
        val spark = SparkSession
          .builder
          .appName("HdfsTest")
          .master("local[*]").getOrCreate()
        try {
          println("STARTED SPARK JOB")
          //val fileURI = "/Users/abraham/Downloads/Riverside_WaterDistrict2.csv"
          val df = spark.read.csv(fileURI)
          df.show(20,truncate = false)
          val sparkContext = spark.sparkContext
          //import edu.ucr.cs.bdlab.beast._
          val polygons = sparkContext.shapefile(fileURI)
          //println(polygons.name)
          val beastOpts = new mutable.HashMap[String,String]()
          beastOpts.put("separator",",")
          beastOpts.put("skipheader","true")
          beastOpts.put("iformat","geojson")

          println("Success")
          HDFSActionPerformed("Success")

        } catch {
          case e: NoClassDefFoundError =>
            println("Could not get spark session" + e.toString)
            HDFSActionPerformed("Failure")
          case _: Throwable =>
            println("Error" + _)
            HDFSActionPerformed("Failure")
        } finally {
          println("Good Bye!")
          spark.stop()
          HDFSActionPerformed("Exit")
        }
        //replyTo ! HDFSActionPerformed(s"FILE UPLOAD STARTED")
        //Behaviors.same
      }
    //}

  //}

}


