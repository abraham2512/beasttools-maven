import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import edu.ucr.cs.bdlab.davinci.{MultilevelPyramidPlotHelper, TileIndex}
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession
import org.apache.commons.io.output.ByteArrayOutputStream

import java.awt.image.{DataBufferByte, WritableRaster}

object TileActor {
  sealed trait TileCommand
  final case class TileActionPerformed(description:String)
  final case class GetTile(dataset:String,tile: (String,String,String), replyTo:ActorRef[Array[Byte]]) extends TileCommand


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
            val tileIndexPath = new Path("data/indexed",dataset)
            val tile = "tile-"+z+"-"+x+"-"+y+".png"
            val tileImagePath = new Path("data/viz",dataset)
            val tileHeight = 256
            val tileWidth = 256
            val fileSystem = tileIndexPath.getFileSystem(sc.hadoopConfiguration)
//            println("Filesystem" + fileSystem)
//            println("tileID",tileID)
//            println("tileIndexPath",tileIndexPath)
//            println("tileImagePath",tileImagePath)
//            println(fileSystem)
            //val pathToAIDIndex = new Path(tileIndexPath, dataset)
            val interimOutput = new ByteArrayOutputStream()
            println("Starting tile plot")
            println(MultilevelPyramidPlotHelper
             .plotTile(fileSystem,tileImagePath,tileID,interimOutput))
            interimOutput.close()
            val tiledata = interimOutput.toByteArray
            println("Finished plotting")
            //println("Tile status -> "+tileGenerated)
            // println("TileActor: Generated on the fly", tileGenerated)
            //spatialFile.plotImage()
            replyTo ! tiledata
          } catch {
            case _:Throwable => println("ERROR" + _)
          } finally {

            print("EXIT")
            // replyTo ! resourcePath.toString
          }


          //replyTo ! "error"
          Behaviors.same

        case _ => println("default case")
          Behaviors.same
      }
  }

}
