package actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.geolite.EnvelopeNDLite
import edu.ucr.cs.bdlab.beast.io.SpatialFileRDD
import edu.ucr.cs.bdlab.davinci.{MultilevelPyramidPlotHelper, TileIndex}
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{IntegerType, StringType}

object TileActor {
  sealed trait TileCommand
  final case class TileActionPerformed(description: String)
  final case class GetTile(dataset: String, tile: (String, String, String), replyTo: ActorRef[Array[Byte]]) extends TileCommand
  final case class GetMetaData(dataset: String, mbrString: String, replyTo: ActorRef[String]) extends TileCommand


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

        case GetMetaData(dataset: String, mbrString:String,replyTo) =>
          val spark = SparkSession
            .builder
            .appName("HdfsTest")
            .master("local[*]").getOrCreate()
          val sc = spark.sparkContext
          val opts = new BeastOptions(loadDefaults = false)
          val datapath = "data/indexed/"+dataset
          val featureReaderClass = SpatialFileRDD.getFeatureReaderClass(datapath,opts)
          println(featureReaderClass)
          val partitions = SpatialFileRDD.createPartitions(datapath, opts,sc.hadoopConfiguration)



          println(dataset+"->"+mbrString)

          var mbrs:Array[EnvelopeNDLite] = null
          var mbr:EnvelopeNDLite = null

          if (mbrString!=null) {
            mbr = EnvelopeNDLite.decodeString(mbrString,new EnvelopeNDLite())
            if (mbr.getSideLength(0) > 360.0) {
              mbr.setMinCoord(0, -180.0)
              mbr.setMaxCoord(0, +180.0)
            }
            else { // Adjust the minimum and maximum longitude to be in the range [-180.0, +180.0]
              mbr.setMinCoord(0, mbr.getMinCoord(0) - 360.0 * Math.floor((mbr.getMinCoord(0) + 180.0) / 360.0))
              mbr.setMaxCoord(0, mbr.getMaxCoord(0) - 360.0 * Math.floor((mbr.getMaxCoord(0) + 180.0) / 360.0))
            }
            if (mbr.getMinCoord(0) > mbr.getMaxCoord(0)) { // The MBR crosses the international day line, split it into two MBRs
              mbrs = new Array[EnvelopeNDLite](2)
              mbrs(0) = new EnvelopeNDLite(2, mbr.getMinCoord(0), mbr.getMinCoord(1), +180.0, mbr.getMaxCoord(1))
              mbrs(1) = new EnvelopeNDLite(2, -180.0, mbr.getMinCoord(1), mbr.getMaxCoord(0), mbr.getMaxCoord(1))
            }
            else { // A simple MBR that does not cross the line.
              mbrs = Array[EnvelopeNDLite](mbr)
            }
          }

          opts.set(SpatialFileRDD.FilterMBR,mbr.encodeAsString)

          var feature_found = false
          var return_map:Map[String,Any] = Map()
          for (partition <- partitions) {
            val features = SpatialFileRDD.readPartitionJ(partition, featureReaderClass, opts)
            while (features.hasNext & !feature_found) {
              val feature = features.next()
              val fields = feature.schema.fields
              import scala.collection.JavaConversions._ //Deprecated, find alternate to convert features java collection
              for (i <- feature.iNonGeomJ) {
                if (!feature.isNullAt(i)) {
                  feature_found = true
                  val dataType = fields(i).dataType
                  val attName = fields(i).name
                  val value = feature.get(i)
//                  println(attName, dataType, value)
                  dataType match {
                    case StringType => return_map += (attName->value)
                    case IntegerType => return_map += (attName->value.toString)
                    case default => println("default case for "+dataType)
                  }
                }
              }
            }
          }
          print(return_map.mkString("{",",","}"))
          import org.json4s.native.Json
          import org.json4s.DefaultFormats

          //GET METADATA
          replyTo ! Json(DefaultFormats).write(return_map)
          Behaviors.same


        case _ => println("default case")
          Behaviors.same
      }
  }

}
