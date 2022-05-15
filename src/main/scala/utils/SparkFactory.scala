package utils

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

object SparkFactory {
  val spark: SparkSession = SparkSession
    .builder
    .appName("HdfsTest") //.config(conf)
    .master("local[*]").getOrCreate()
  val sc:SparkContext = spark.sparkContext
}
