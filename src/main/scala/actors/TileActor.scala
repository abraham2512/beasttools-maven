package actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import edu.ucr.cs.bdlab.davinci.{MultilevelPyramidPlotHelper, TileIndex}
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession

object TileActor {
  sealed trait TileCommand
  final case class TileActionPerformed(description: String)
  final case class GetTile(dataset: String, tile: (String, String, String), replyTo: ActorRef[Array[Byte]]) extends TileCommand

  def apply(): Behavior[TileCommand] = Behaviors.setup {
    context: ActorContext[TileCommand] =>
      println("actors.TileActor: Listening for tiles to generate")
      Behaviors.receiveMessage {
        case GetTile(dataset, (z, x, y), replyTo) =>
          //          println("Received message!")
          val spark = SparkSession
            .builder
            .appName("HdfsTest")
            .master("local[*]").getOrCreate()
          val sc = spark.sparkContext
          println("actors.TileActor: Spark session started!")
          try {
            println("actors.TileActor: Starting tile plot for tile-" + z + "-" + x + "-" + y)

            val tileID = TileIndex.encode(z.toInt, x.toInt, y.toInt)
            val tileIndexPath = new Path("data/indexed", dataset)
            val tileVizPath = new Path("data/viz", dataset)
            val fileSystem = tileIndexPath.getFileSystem(sc.hadoopConfiguration)
            val interimOutput = new ByteArrayOutputStream()

            MultilevelPyramidPlotHelper
              .plotTile(fileSystem, tileVizPath, tileID, interimOutput)
            interimOutput.close()
            println("actors.TileActor: Finished plot")
            replyTo ! interimOutput.toByteArray

          } catch {
            case _: Throwable => println("ERROR" + _)
          } finally {
            spark.stop()
          }

          Behaviors.same

        case _ => println("default case")
          Behaviors.same
      }
  }

}
