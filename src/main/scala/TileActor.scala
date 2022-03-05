import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.util.BeastServer
import edu.ucr.cs.bdlab.davinci.{MultilevelPyramidPlotHelper, TileIndex}
import org.apache.hadoop.fs.{FSDataInputStream, FileSystem, Path}
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

object TileActor {
  sealed trait TileCommand
  final case class TileActionPerformed(description:String)
  final case class GetTile(string: String, replyTo:ActorRef[String]) extends TileCommand

//  val sc = SparkContext.getOrCreate()
  val dataDir = "viz"
//  val dataFileSystem = FileSystem.get(sc.hadoopConfiguration)


  def apply(): Behavior[TileCommand] = Behaviors.setup {
    context: ActorContext[TileCommand] =>
      Behaviors.receiveMessage {
        case GetTile(tilename, replyTo) => {
          val spark = SparkSession
            .builder
            .appName("HdfsTest")
            .master("local[*]").getOrCreate()
          val sc = spark.sparkContext
          print("Received message!")
          //val tileGenerated = MultilevelPyramidPlotHelper.plotTile()
          val datasetID:String = "viz/SafetyDept"

          val z, x, y = 0
          //val tileID = "tile-10-177-408.png"
          val resourcePath = new Path(datasetID,tilename)

          println(resourcePath)
//          if(dataFileSystem.exists(resourcePath)) println("FileExists")

          println("GETTING TILE ", tilename)
          replyTo ! resourcePath.toString
          Behaviors.same
        }
      }
  }

}
