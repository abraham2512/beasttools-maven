

import slick.jdbc.JdbcProfile

class DataFileDAO(val profile: JdbcProfile) {
  // Import the Scala API from the profile
  import profile.api._
  //#dao

  class Props(tag: Tag) extends Table[(String, String, String, String)](tag, "PROPS") {
    def filename = column[String]("filename", O.PrimaryKey)
    def filetype = column[String]("filetype")
    def filesource = column[String]("filesource")
    def filestatus = column[String]("filestatus")
    def * = (filename,filetype,filesource,filestatus)
  }
  val props = TableQuery[Props]

  /** Create the database schema */
  def create: DBIO[Unit] =
    props.schema.create

  /** Insert a key/value pair */
  def insert(filename: String, filetype:String, filesource: String, filestatus: String): DBIO[Int] =
    props += (filename,filetype,filesource,filestatus)

  /** Get the value for the given key */
  def get(k: String): DBIO[Option[(String,String,String,String)]] =
    (for(p <- props if p.filename === k) yield p ).result.headOption

  /** Get all values */
  def get_all(): DBIO[Seq[(String,String,String,String)]] =
    (for(p <- props ) yield p).result.withPinnedSession

  /** Get the first element for a Query from this DAO */
  def getFirst[M, U, C[_]](q: Query[M, U, C]): DBIO[U] =
    q.result.head
}