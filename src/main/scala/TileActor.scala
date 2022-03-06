import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.util.BeastServer
import edu.ucr.cs.bdlab.davinci.{MultilevelPyramidPlotHelper, Plotter, TileIndex}
import org.apache.hadoop.fs.{FSDataInputStream, FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._

import java.io.FileOutputStream
import org.apache.spark.sql.SparkSession
import edu.ucr.cs.bdlab.beast._
import edu.ucr.cs.bdlab.beast.indexing.RSGrovePartitioner
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import org.apache.commons.io.output.ByteArrayOutputStream

object TileActor {
  sealed trait TileCommand
  final case class TileActionPerformed(description:String)
  final case class GetTile(dataset:String,tile: (String,String,String), replyTo:ActorRef[String]) extends TileCommand


  def apply(): Behavior[TileCommand] = Behaviors.setup {
    context: ActorContext[TileCommand] =>
      Behaviors.receiveMessage {
        case GetTile(dataset,(z,x,y), replyTo) =>
          println("Received message!")
          val spark = SparkSession
            .builder
            .appName("HdfsTest")
            .master("local[*]").getOrCreate()
          val sc = spark.sparkContext
          //val tileGenerated = MultilevelPyramidPlotHelper.plotTile()
          //val tileID = "tile-10-177-408.png"
          println("Spark session started!")
          try{
            val tileID = TileIndex.encode(z.toInt,x.toInt,y.toInt)
            val tileIndexPath = new Path("partitioned_data")
            val tile = "tile-"+z+"-"+x+"-"+y+".png"
            val tileImagePath = new Path("viz/"+dataset, tile)

            val tileHeight = 256
            val tileWidth = 256
            val fileSystem = tileIndexPath.getFileSystem(sc.hadoopConfiguration)
            println("Filesystem" + fileSystem)
            println("tileID",tileID)
            println("tileIndexPath",tileIndexPath)
            println("tileImagePath",tileImagePath)
            println(fileSystem)
            val pathToAIDIndex = new Path(tileIndexPath, dataset)


            val interimOutput = new ByteArrayOutputStream()
            println("Starting tile plot")
            val tileGenerated = MultilevelPyramidPlotHelper
              .plotTile(fileSystem,pathToAIDIndex,tileID,interimOutput)


            interimOutput.close()
            println("Finished plotting")
            println("TILE"+tileGenerated)

            if(!tileGenerated){
              println("ERROR!")
              replyTo ! "FAILED TO GENERATE ON THE FLY"
            }else{
              println("GENERATED ON THE FLY")
              println(interimOutput)
            }
            // println("TileActor: Generated on the fly", tileGenerated)
            //spatialFile.plotImage()
          } catch {
            case _:Throwable => println("ERROR" + _)
          } finally {

            print("EXIT")
            // replyTo ! resourcePath.toString
          }


          replyTo ! "SUCCESS"
          Behaviors.same

        case _ => println("default case")
          Behaviors.same
      }
  }

}
