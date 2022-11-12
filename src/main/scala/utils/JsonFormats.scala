package utils

import actors.FileRegistry.FileActionPerformed
import actors.DataFile
import actors.DataFiles
import spray.json.DefaultJsonProtocol
import actors.Query
object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._

  implicit val fileJsonFormat = jsonFormat4(DataFile)
  implicit val filesJsonFormat = jsonFormat1(DataFiles)
  implicit val fileActionPerformedJsonFormat = jsonFormat1(FileActionPerformed)
  implicit val queryJsonFormat = jsonFormat3(Query)
}
