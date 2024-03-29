package utils

import actors.FileRegistry.FileActionPerformed
import actors.{DataFile, DataFiles, GeneratedSummary, Query, SourceQuery}
import spray.json.DefaultJsonProtocol
object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._

  implicit val fileJsonFormat = jsonFormat4(DataFile)
  implicit val filesJsonFormat = jsonFormat1(DataFiles)
  implicit val fileActionPerformedJsonFormat = jsonFormat1(FileActionPerformed)
  implicit val queryJsonFormat = jsonFormat3(Query)
  implicit val generatedSummaryJsonFormat = jsonFormat7(GeneratedSummary)
  implicit val sourceQueryJsonFormat = jsonFormat3(SourceQuery)
}
