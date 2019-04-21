package model

import java.sql.Timestamp

import scala.concurrent.{ExecutionContext, Future}
import slick.lifted.Tag
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult

object LoginCookie {
  def apply(db: Database)(implicit ec: ExecutionContext): LoginCookie = new LoginCookie(db)

  final case class LoginCookieEntity(id: Int, crawlAccountId: Int, sessionId: String, mid: String, csrftoken: String, dsUserId: String, rur: String, urlgen: String, isEnabled: Boolean, createdAt: Timestamp)

  class LoginCookieTable(tag: Tag) extends Table[LoginCookieEntity](tag, "login_cookies") {
    val id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val crawlAccountId = column[Int]("crawl_account_id")
    val sessionId = column[String]("session_id")
    val mid = column[String]("mid")
    val csrftoken = column[String]("csrftoken")
    val dsUserId = column[String]("ds_user_id")
    val rur = column[String]("rur")
    val urlgen = column[String]("urlgen")
    val isEnabled = column[Boolean]("is_enabled")
    val createdAt = column[Timestamp]("created_at")

    def * = (id, crawlAccountId, sessionId, mid, csrftoken, dsUserId, rur, urlgen, isEnabled, createdAt) <> (LoginCookieEntity.tupled, LoginCookieEntity.unapply)

  }

  val loginCookieTable = TableQuery[LoginCookieTable]
}

class LoginCookie(db: Database)(implicit ec: ExecutionContext) {

  import LoginCookie._

  implicit val getLoginCookieResult = GetResult(r => LoginCookieEntity(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  def findAll(): Future[Seq[LoginCookieEntity]] = db.run(loginCookieTable.result)

  def findById(id: Int): Future[Option[LoginCookieEntity]] = db.run(loginCookieTable.filter(_.id === id).result.headOption)

  def findAvailableOne() =
    db.run(
      sql"""
           SELECT *
           FROM login_cookies
           WHERE created_at BETWEEN DATE_SUB(NOW(), INTERVAL 1 DAY) AND NOW()
           LIMIT 1
         """
        .as[LoginCookieEntity]
        .headOption
    )

  def create(crawlAccountId: Int, cookie: Map[String, String]) =
    db.run(
      loginCookieTable.map(t => (t.crawlAccountId, t.sessionId, t.mid, t.csrftoken, t.dsUserId, t.rur, t.urlgen)) +=
        (crawlAccountId, cookie("sessionid"), cookie("mid"), cookie("csrftoken"), cookie("dsUserId"), cookie("rur"), cookie("urlgen"))
    )

  def delete(target: LoginCookieEntity): Future[Int] = db.run(loginCookieTable.filter(_.id === target.id).delete)

}