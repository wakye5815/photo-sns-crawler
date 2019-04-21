package model

import java.sql.Timestamp

import scala.concurrent.{ExecutionContext, Future}
import slick.driver.MySQLDriver.api._
import slick.lifted.Tag

object CrawlAccount {
  def apply(db: Database)(implicit ec: ExecutionContext): CrawlAccount = new CrawlAccount(db)

  final case class CrawlAccountEntity(id: Int, instagramId: String, mailAddress: String, password: String, status: Int, updatedAt: Timestamp, createdAt: Timestamp)

  class CrawlAccountTable(tag: Tag) extends Table[CrawlAccountEntity](tag, "crawl_accounts") {
    val id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    val instagramId = column[String]("instagram_id")
    val mailAddress = column[String]("mail_address")
    val password = column[String]("password")
    val status = column[Int]("status")
    val updatedAt = column[Timestamp]("updated_at")
    val createdAt = column[Timestamp]("created_at")

    def * = (id, instagramId, mailAddress, password, status, updatedAt, createdAt) <> (CrawlAccountEntity.tupled, CrawlAccountEntity.unapply)

  }

  val crawlAccountTable = TableQuery[CrawlAccountTable]

}

class CrawlAccount(db: Database)(implicit ec: ExecutionContext) {

  import CrawlAccount._

  def findAll(): Future[Seq[CrawlAccountEntity]] = db.run(crawlAccountTable.result)

  def findById(id: Int): Future[Option[CrawlAccountEntity]] = db.run(crawlAccountTable.filter(_.id === id).result.headOption)

  def findLoginableOne(): Future[Option[CrawlAccountEntity]] = db.run(crawlAccountTable.filter(_.status === 1).result.headOption)

  def create(crawlUserId: String, mailAddress: String, password: String) = db.run(
    crawlAccountTable.map(u => (u.instagramId, u.mailAddress, u.password)) += (crawlUserId, mailAddress, password)
  )

  def update(latest: CrawlAccountEntity): Future[Int] = db.run(
    crawlAccountTable.filter(_.id === latest.id)
      .map(old => (old.instagramId, old.mailAddress, old.password, old.status, ))
      .update((latest.instagramId, latest.mailAddress, latest.password, latest.status))
  )

  def delete(target: CrawlAccountEntity): Future[Int] = db.run(crawlAccountTable.filter(_.id === target.id).delete)

}