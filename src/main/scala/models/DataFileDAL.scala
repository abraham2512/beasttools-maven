package models

import actors.DataFile
import slick.dbio.DBIO
import slick.jdbc.H2Profile
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object DataFileDAL {
  private val dao = new DataFileDAO(H2Profile)
  private val db = Database.forConfig("h2")

  def apply(): Unit = {
    Await.result(create_db(), Duration.Inf)
  }

  def create_db(): Future[Unit] = {
    db.run(DBIO.seq(
      dao.create
    ).withPinnedSession)
  }

  def insert(file: DataFile): Unit = {
    val f = db.run(DBIO.seq(
      dao.insert(file.filename, file.filetype, file.filesource, file.filestatus)
    ).withPinnedSession)
    Await.result(f, Duration.Inf)
  }

  def get_all(): Seq[(String, String, String, String)] = {
    val files: Future[Seq[(String, String, String, String)]] = db.run(dao.get_all())
    Await.result(files, Duration.Inf)

  }

  def get(k: String): Option[(String, String, String, String)] = {
    val file: Future[Option[(String, String, String, String)]] = db.run(dao.get(k).withPinnedSession)
    Await.result(file, Duration.Inf)
  }

  def update_status(filename: String, filestatus: String): Int = {
    val update = db.run(dao.update_status(filename, filestatus).withPinnedSession)
    Await.result(update, Duration.Inf)
  }
  def delete_file(filename:String) :Int = {
    val delete = db.run(dao.delete(filename).withPinnedSession)
    Await.result(delete,Duration.Inf)
  }
}
