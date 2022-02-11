
import FileRegistry.FileActionPerformed
//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val fileJsonFormat = jsonFormat4(File)
  implicit val filesJsonFormat = jsonFormat1(Files)
  implicit val fileActionPerformedJsonFormat = jsonFormat1(FileActionPerformed)
}
//#json-formats
