
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner


object HdfsRegistry {
  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description:String) extends HdfsCommand
  final case class WriteToHdfs(replyTo: ActorRef[HDFSActionPerformed]) extends  HdfsCommand

  def apply(): Behavior[HDFSActionPerformed] = {
    println("Hdfs Actor Born!")
    HDFSActionPerformed("START SUCCESS")
    Behaviors.empty
  }
  def startHDFS(fileURI:String,file: File):HDFSActionPerformed = {
    hdfs_registry(fileURI,file)
    HDFSActionPerformed("Starting Spark Job")
  }

  private def hdfs_registry(fileURI:String,file:File):HDFSActionPerformed = {

//    Behaviors.receiveMessage {

        //case WriteToHdfs(replyTo) => {
        val spark = SparkSession
          .builder
          .appName("HdfsTest")
          .master("local[*]").getOrCreate()
        val sparkContext = spark.sparkContext

        try {
          println("STARTED SPARK JOB")
          //val fileURI = "/Users/abraham/Downloads/Riverside_WaterDistrict2.csv"
//          filetype match {
//            case "geojson" =>
              DataFileDAL.update_status(file.filename,"started_download")
              val partitioned_data:SpatialRDD = sparkContext.geojsonFile(fileURI).spatialPartition(classOf[RSGrovePartitioner])
              partitioned_data.saveAsShapefile(filename="partitioned_data")
              println("partitioning finished")
              DataFileDAL.update_status(file.filename,"partitioned")



          //sparkContext.
//              data.plotImage(2000,2000,"~/Desktop/output.png")
//              val df = data.toDataFrame(spark)

              //df.show()

//            case "shapefile" =>
//              println("TODO")
//
//            case "csv" =>
//              println("TODO")
//
//            case _ =>
//              println("Unknown")
          //}
          //val df = spark.read.csv(fileURI)
          //df.show(20,truncate = false)

          //val polygons = sparkContext.shapefile(fileURI)
          //println(polygons.name)
//          val beastOpts = new mutable.HashMap[String,String]()
//          beastOpts.put("separator",",")
//          beastOpts.put("skipheader","true")
//          beastOpts.put("iformat","geojson")

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
          //println("Good Bye!")
          spark.stop()
          HDFSActionPerformed("Exit")
        }
        //replyTo ! HDFSActionPerformed(s"FILE UPLOAD STARTED")
        //Behaviors.same
      }
    //}

  //}


}


