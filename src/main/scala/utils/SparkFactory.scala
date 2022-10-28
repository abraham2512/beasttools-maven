package utils

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.locationtech.geomesa.spark.jts._
import org.apache.spark.beast.{CRSServer, SparkSQLRegistration}

object SparkFactory {
  val sparkSession: SparkSession = SparkSession
    .builder
    .appName("HdfsTest") //.config(conf)
    .master("local[*]").getOrCreate
  val sparkContext:SparkContext = sparkSession.sparkContext
  SparkSQLRegistration.registerUDT
  SparkSQLRegistration.registerUDF(sparkSession)
}
