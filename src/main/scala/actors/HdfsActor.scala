package actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import models.DataFileDAL
import edu.ucr.cs.bdlab.beast.common.BeastOptions
import edu.ucr.cs.bdlab.beast.indexing.{IndexHelper, RSGrovePartitioner}
import edu.ucr.cs.bdlab.beast.io.{SpatialFileRDD, SpatialReader, SpatialWriter}
import edu.ucr.cs.bdlab.beast.io.ReadWriteMixin._
import edu.ucr.cs.bdlab.davinci.{GeometricPlotter, MultilevelPlot}
import org.apache.spark.sql.SaveMode
import utils.SparkFactory.{sparkContext, sparkSession}

import scala.reflect.io.{Directory, File}


object HdfsActor {

  sealed trait HdfsCommand
  final case class HDFSActionPerformed(description: String) extends HdfsCommand

  val HdfsKey: ServiceKey[HdfsCommand] = ServiceKey("HDFS_ACTOR")

  final case class CreateRTreeIndexFromDF(file: DataFile) extends HdfsCommand
  final case class CreateVizIndexFromDF(file: DataFile) extends HdfsCommand
  final case class CreateDFSource(file: DataFile) extends  HdfsCommand
  final case class SpeakText(msg: String) extends HdfsCommand
  final case class StartQueryAndSave(query: Query) extends HdfsCommand

  def apply(): Behavior[HdfsCommand] = Behaviors.setup {
    context: ActorContext[HdfsCommand] =>
      context.system.receptionist ! Receptionist.Register(HdfsKey, context.self)
      println("actors.HdfsRegistry: Hdfs Actor awake")

      Behaviors.receiveMessage {

        case SpeakText(msg) =>
          println(s"actors.HdfsActor: got a msg: $msg")
          Behaviors.same

        case CreateDFSource(file) =>
          println("actors.HdfsActor: starting datasource API")
          try {
            val input = file.filesource
            // GeoJSON
//            val input_df = sparkSession.read.format("geojson").load(input)
//            val schema = input_df.schema
//            println(schema.size)
//            println(schema.fields.mkString(","))
//            println(input_df.count())

            //CSV with header
//            val input_df = SpatialCSVSource.read(sparkSession, input, Seq(SpatialCSVSource.GeometryType -> "point",
//              "header" -> "true", SpatialCSVSource.DimensionColumns -> "x,y", "delimiter" -> " ").toMap.asJava)

            //CSV with no header
//            val input_df = SpatialCSVSource.read(sparkSession, input, Seq(SpatialCSVSource.GeometryType -> "point",
//              SpatialCSVSource.DimensionColumns -> "1,2", "delimiter" -> " ").toMap.asJava)


            // Shapefile


            val input_df = sparkSession.read.format("shapefile").load(input)



            val schema = input_df.schema


            val df_outPath = "data/datasource/" + file.filename
            if (Directory(df_outPath).exists){
              File(df_outPath).deleteRecursively()
              println("actors.HdfsActor: Existing folder with same name deleted")
            }
            input_df.write.format("shapefile").mode(SaveMode.Overwrite).save(df_outPath)
            println("actors.HdfsActor: Dataframes created with Datasource API")
            DataFileDAL.update_status(file.filename, filestatus = "partitioned")
          }
          catch {
            case e:Exception =>
              println("actors.HdfsActor: "+e.toString)
              DataFileDAL.update_status(file.filename, filestatus = "error")
          }
          Behaviors.same

        case CreateRTreeIndexFromDF(file) =>
          try {
            println("actors.HdfsActor: Creating RTree index for file: " + file.filename)
            val input_path = "data/datasource/" + file.filename
            //geojson
//            val input_df  = sparkSession.read.format("geojson").load(input_path)

            //csv

//            val input_df = SpatialCSVSource.read(sparkSession, input_path, Seq(SpatialCSVSource.GeometryType -> "point",
//              "header" -> "true", SpatialCSVSource.DimensionColumns -> "x,y", "delimiter" -> " ").toMap.asJava)


            //shapefile
            val input_df = sparkSession.read.format("shapefile").load(input_path)

            //Partition
            val input_rdd = SpatialReader.dataFrameToSpatialRDD(input_df)
            val features = input_rdd.spatialPartition(classOf[RSGrovePartitioner])
            val opts2 = Seq( // TODO opts2 -> opts
              "iformat" -> "shapefile",
              IndexHelper.BalancedPartitioning -> true,
              SpatialWriter.OutputFormat -> "rtree",
              SpatialFileRDD.Recursive -> true
            )
            val outPath = "data/indexed/" + file.filename

            IndexHelper.saveIndex2(features, outPath, opts2)

            println("actors.HdfsActor: RTree index created successfully")
            DataFileDAL.update_status(file.filename, filestatus = "rtree-indexed")
            HDFSActionPerformed("Success")
            Behaviors.same
          } catch {
            case e: NoClassDefFoundError =>
              println("actors.HdfsActor: Could not get spark session" + e.toString)
              HDFSActionPerformed("Failure")
              Behaviors.same
            case e: Exception =>
              println("actors.HdfsActor: Error :" + e.toString)
              e.printStackTrace()
              DataFileDAL.update_status(file.filename, filestatus = "error")
              HDFSActionPerformed("Failure")
              Behaviors.same
          } finally {
            HDFSActionPerformed("Exit")
            Behaviors.same
          }

        case CreateVizIndexFromDF(file) =>
          try {
            println("actors.HdfsActor: Converting file " + file.filename + " to RDD")
            val input_path = "data/indexed/" + file.filename
            //geojson
//            val input_df  = sparkSession.read.format("geojson").load(input_path)

            //csv

//            val input_df = SpatialCSVSource.read(sparkSession, input_path, Seq(SpatialCSVSource.GeometryType -> "point",
//              "header" -> "true", SpatialCSVSource.DimensionColumns -> "x,y", "delimiter" -> " ").toMap.asJava)


            //shapefile
//            val input_dff = sparkSession.read.format("rtree").load(input_path)


            val input_rdd = sparkContext.spatialFile(input_path)

            val opts3 = new BeastOptions(false)
            opts3.setInt(MultilevelPlot.ImageTileThreshold, 500000)
            opts3.setBoolean("data-tiles", false)
            opts3.setBoolean("mercator", true)
            opts3.set("stroke", "blue")

            val outPath = "data/viz/" + file.filename

            MultilevelPlot.plotFeatures(input_rdd, levels = 0 until 20, classOf[GeometricPlotter], input_path, outPath, opts3)
            println("actors.HdfsActor: Dataset plotted successfully")
            DataFileDAL.update_status(file.filename, filestatus = "indexed")
            HDFSActionPerformed("Success")
            Behaviors.same
          } catch {
            case e: NoClassDefFoundError =>
              println("actors.HdfsActor: Could not get spark session" + e.toString)
              HDFSActionPerformed("Failure")
              Behaviors.same
            case e: Exception =>
              println("actors.HdfsActor: Error :" + e.toString)
              e.printStackTrace()
              DataFileDAL.update_status(file.filename, filestatus = "error")
              HDFSActionPerformed("Failure")
              Behaviors.same
          } finally {
            HDFSActionPerformed("Exit")
            Behaviors.same
          }
        case StartQueryAndSave(query) => {
          println(query)
          try {
            val input_path = "data/datasource/" + query.dataset

            //geojson
//            val input_df  = sparkSession.read.format("geojson").load(input_path)

            //csv
//            val input_df = SpatialCSVSource.read(sparkSession, input_path, Seq(SpatialCSVSource.GeometryType -> "point",
//              "header" -> "true", SpatialCSVSource.DimensionColumns -> "x,y", "delimiter" -> " ").toMap.asJava)

            //shapefile
            val input_df = sparkSession.read.format("shapefile").load(input_path)


            var df_outPath = "data/datasource/" + query.dataset
            var dataset = query.dataset
            if (query.saveMode=="true") {
              df_outPath = df_outPath + "_new"
              dataset += "_new"
            }
            if (Directory(df_outPath).exists){
              File(df_outPath).deleteRecursively()
              println("actors.HdfsActor: Existing folder with same name deleted")
            }
            val output_file = DataFile(filename = dataset, filetype = "geojson", filestatus = "started", filesource = df_outPath )
            DataFileDAL.insert(output_file)

            println(input_df.schema)

            input_df.createOrReplaceTempView(query.dataset)
            val new_DF = sparkSession.sql(query.query)
            println(new_DF.schema)
            val output_path =  new java.io.File(df_outPath)

            new_DF.write.format("shapefile").mode(SaveMode.Overwrite).save(output_path.getPath)

            DataFileDAL.update_status(dataset,"partitioned")
            println("actors.HdfsActor: Dataset saved using Datasource API")

          } catch {
            case e: NullPointerException =>
              println("actors.HdfsActor: Dataset does not exist :" + e.toString)
            case e: Exception =>
              println("actors.HdfsActor: " + e.toString)
              e.printStackTrace()
          }
          Behaviors.same
        }
        case _ => println("actors.HdfsActor: default case")
          Behaviors.same
      }
  }


}
