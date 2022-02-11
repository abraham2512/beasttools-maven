

import slick.dbio.DBIO
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend.Database
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration


object DataFileDAL {
    //sealed trait QueryResult
    private val dao = new DataFileDAO(H2Profile)
    private val db = Database.forConfig("h2")
    def apply(): Unit = {
      val f = create_db()
      Await.result(f, Duration.Inf)
      println("DAO Object created!")
    }

    def create_db() : Future[Unit] = {
      db.run(DBIO.seq(
        dao.create
      ).withPinnedSession)
    }

    def insert(file: File): Future[Unit] = {
      db.run(DBIO.seq(
        dao.insert(file.filename,file.filetype,file.filesource,file.filestatus)
      ).withPinnedSession)
    }

    def get_all(): Seq[(String,String,String,String)]= {
      val files: Future[Seq[(String, String, String, String)]] = db.run(dao.get_all())
      Await.result(files, Duration.Inf)

    }

  def get(k:String): Option[(String,String,String,String)] = {
  try{
    val file : Future[Option[(String,String,String,String)]] = db.run(dao.get(k).withPinnedSession)
    Await.result(file,Duration.Inf)
    } finally {
    println("Get complete")
  }
  }

}
