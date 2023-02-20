package models

import org.json4s.DefaultFormats
import org.json4s.jackson.Json
import slick.jdbc.JdbcProfile

import java.util.UUID

class DataFileDAO(val profile: JdbcProfile) {
  // Import the Scala API from the profile
  import profile.api._
  //#dao

  implicit val doubleArrayColumnType = MappedColumnType.base[Array[Double], String](
    { arrD => arrD.mkString(",") }, // map Array[Double] to String
    { str => if (str == "") Array[Double]() else str.split(",").map(_.toDouble) } // map String to Array[Double]
  )
  implicit val MapSSArrayColumnType = MappedColumnType.base[Array[Map[String, String]], String](
    { arrD => Json(DefaultFormats).write(arrD) },
    { str => Json(DefaultFormats).read[Array[Map[String, String]]](str) }
  )

  class Props(tag: Tag) extends Table[(String, String, String, String, String, Long, Long, Long, String, Array[Double], Array[Double], Array[Map[String, String]])](tag, "PROPS") {
    def id = column[Option[Int]]("id", O.AutoInc)
    def uuid = column[UUID]("uuid", O.PrimaryKey)
    def filename = column[String]("filename")
    def filetype = column[String]("filetype")
    def filesource = column[String]("filesource")
    def filestatus = column[String]("filestatus")
    def summary_status = column[String]("summary_status", O.Default("not_summarized"))
    def size = column[Long]("size", O.Default(-1))
    def num_features = column[Long]("num_features", O.Default(-1))
    def num_points = column[Long]("num_points", O.Default(-1))
    def geometry_type = column[String]("geometry_type", O.Default(""))
    def extent = column[Array[Double]]("extent", O.Default(Array[Double]()))
    def avg_sidelength = column[Array[Double]]("avg_sidelength", O.Default(Array[Double]()))
    def attributes = column[Array[Map[String, String]]]("attributes", O.Default(Array[Map[String, String]]()))
    def * = (filename,filetype,filesource,filestatus, summary_status, size, num_features, num_points, geometry_type, extent, avg_sidelength, attributes)
  }
  val props = TableQuery[Props]

  /** Create the database schema */
  def create: DBIO[Unit] =
    props.schema.createIfNotExists

  /** Insert a key/value pair */
  def insert(filename: String, filetype:String, filesource: String, filestatus: String): DBIO[Int] =
    props.map( c => (c.filename, c.filetype, c.filesource, c.filestatus) ) += (filename, filetype, filesource, filestatus)

  /** Get the value for the given key */
  def get(filename: String): DBIO[Option[(String,String,String,String)]] =
    (for(p <- props if p.filename === filename) yield p ).map( c => (c.filename, c.filetype, c.filesource, c.filestatus) ).result.headOption

  /** Get all values */
  def get_all(): DBIO[Seq[(String,String,String,String)]] =
    (for(p <- props ) yield p).map( c => (c.filename, c.filetype, c.filesource, c.filestatus) ).result.withPinnedSession

  def update_status(filename:String, filestatus:String): DBIO[Int] = {
    val q = for { p <- props if p.filename === filename } yield p.filestatus
    q.update(filestatus)
  }

  def delete(filename: String): DBIO[Int] = {
    props.filter(_.filename === filename).delete
  }

  def get_uuid(filename: String): DBIO[Option[UUID]] =
    (for(p <- props if p.filename === filename) yield p.uuid ).result.headOption

  def update_summary(filename:String, size: Long, num_features: Long, num_points: Long, geometry_type: String, extent: Array[Double], avg_sidelength: Array[Double], attributes: Array[Map[String, String]]): DBIO[Int] = {
    props.filter(_.filename === filename)
      .map( c => (c.size, c.num_features, c.num_points, c.geometry_type, c.extent, c.avg_sidelength, c.attributes) )
      .update(size, num_features, num_points, geometry_type, extent, avg_sidelength, attributes)
  }

  def update_summary_status(filename: String, summary_status: String): DBIO[Int] = {
    val q = for { p <- props if p.filename === filename } yield p.summary_status
    q.update(summary_status)
  }

  /** Get the first element for a Query from this DAO */
//  def getFirst[M, U, C[_]](q: Query[M, U, C]): DBIO[U] =
//    q.result.head

}