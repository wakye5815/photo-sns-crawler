package crawler

import base.{BaseCollection, BaseCrawler}
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeDriver
import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

case class PostsCollection(postsId: String, postsOwnerId: String, usedTagList: List[String], likeCount: Int, LikedUserIdList: List[String], postTime: Date) extends BaseCollection

case class PostsCrawler(driver: ChromeDriver, targetUrl: String) extends BaseCrawler {
  private val likedUserRowElements = "document.getElementsByClassName(\"Igw0E IwRSH YBx95 vwCYk\")"
  val expectedUrlPattern = "https://www.instagram.com/p/.*/"
  super.moveTargetPage()

  private def findDisplayedLikedUserCount() = this.driver.asInstanceOf[JavascriptExecutor]
    .executeScript(s"return $likedUserRowElements.length")
    .toString.toInt

  private def extractPostsId() = {
    val pattern = "https://www.instagram.com/p/(.*)/".r
    this.targetUrl match {
      case pattern(postsId) => postsId
      case _ => ""
    }
  }

  private def findPostsOwnerId() = this.driver.findElementByXPath("//*[@class='FPmhX notranslate nJAzx']")
    .getText()

  private def findDisplaydLikedUserIdList(displayedLikedUserCount: Int) = (0 to (displayedLikedUserCount - 1)).toList
    .map(i => this.executeScript(s"return ${this.likedUserRowElements}[$i].children[0].innerText")
      .toString()
    )

  private def findUsedTagList() = {
    val pattern = "https://www.instagram.com/explore/tags/(.*)/".r
    this.driver.findElementsByXPath("//*[@class='C4VMK']/span/a")
      .asScala.toList
      .map(ele => ele.getAttribute("href") match {
        case pattern(tagName) => tagName
        case _ => ""
      })
      .filter(tagName => !tagName.isEmpty)
  }

  private def findLikeCount() = this.driver.findElementByXPath("//*[@class='zV_Nj']/span")
    .getText()
    .toInt

//  fixme:4~6割ほどしか取得しきれないので要修正
  private def findLikedUserIdList(): List[String] = {
    def recursiveFunc(list: List[String] = List()): List[String] = list.length match {
      case 0 => {
        this.executeScript("document.getElementsByClassName(\"zV_Nj\")[0].click()")
        Thread.sleep(3000)
        val likedUserIdList = this.findDisplaydLikedUserIdList(this.findDisplayedLikedUserCount())
        recursiveFunc(List.concat(list, likedUserIdList))
      }
      case length: Int if (length >= this.findLikeCount()) => list.distinct
      case _: Int => {
        this.executeScript(s"${this.likedUserRowElements}[${this.findDisplayedLikedUserCount() - 1}].scrollIntoView()")
        Thread.sleep(3000)
        val likedUserIdList = this.findDisplaydLikedUserIdList(this.findDisplayedLikedUserCount())
        recursiveFunc(List.concat(list, likedUserIdList))
      }
    }
    recursiveFunc()
  }

  private def findPostDate() = this.driver.findElementByXPath("//*[@class='_1o9PC Nzb55']")
    .getAttribute("datetime")

  private def parsePostDate(dateStr: String) = Try {
    val pattern = """(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})\..*$""".r
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(
      dateStr match {
        case pattern(year, month, day, hour, minute, second) => s"$year-$month-$day $hour:$minute:$second"
        case _ => throw new Exception(s"""Could not parse string:"$dateStr" to date""")
      }
    )
  } match {
    case Success(date: Date) => date
    case Failure(_) => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("1900-01-01 00:00:00")
  }

  def createCollection() = new PostsCollection(
    this.extractPostsId(),
    this.findPostsOwnerId(),
    this.findUsedTagList(),
    this.findLikeCount(),
    this.findLikedUserIdList(),
    this.parsePostDate(this.findPostDate())
  )

}
