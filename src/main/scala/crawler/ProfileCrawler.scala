package crawler

import base.{BaseCollection, BaseCrawler}
import org.openqa.selenium.{By, WebElement}
import org.openqa.selenium.chrome.ChromeDriver

import scala.collection.JavaConverters._

case class ProfileCollection(
                              userId: String,
                              postsCount: Int,
                              followingUserCount: Int,
                              followedUserCount: Int,
                              followedUserIdList: List[String],
                              followingUserIdList: List[String],
                              postsIdList: List[String]
                            ) extends BaseCollection

case class ProfileCrawler(driver: ChromeDriver, targetUrl: String) extends BaseCrawler {
  private val displayedUserRowInDialogElements = """document.getElementsByClassName("PZuss")[0].getElementsByTagName("li")"""
  private val userPostElements = """document.getElementsByClassName("v1Nh3 kIKUG  _bz0w")"""

  val expectedUrlPattern = "https://www.instagram.com/.*/"
  super.moveTargetPage()

  private def findUserId() = this.driver.findElementByXPath("//*[@class='nZSzR']/h1")
    .getText()

  private def findSomeCountByRegexStr(regex: String) = this.driver.findElementsByClassName("-nal3")
    .asScala.toList
    .find(ele => ele.getText.matches(regex)) match {
    case Some(target) => {
      val numberExtractor = regex.r
      target.getText() match {
        case numberExtractor(count) => count.replace(",", "").toInt
        case _ => 0
      }
    }
    case None => 0
  }

  private def findDisplayedUserInDialogCount() = driver.executeScript(s"return $displayedUserRowInDialogElements.length").toString.toInt

  private def findDisplayedUserIdList(displayedUserCount: Int) = (0 to (displayedUserCount - 1)).toList
    .map(i => driver.executeScript(s"""return document.getElementsByClassName("FPmhX notranslate _0imsa ")[$i].getAttribute("title")""").toString)

  //  ダイアログ内のユーザーのIDを取得
  //  フォロワーとフォローしているユーザーのID取得に使用
  private def findUserIdListInDialog(userMaxCount: Int, triggerElement: WebElement): List[String] = {
    def recursiveFunc(list: List[String] = List()): List[String] = list.length match {
      case 0 => {
        triggerElement.click()
        Thread.sleep(3000)
        val displayedUserIdList = this.findDisplayedUserIdList(this.findDisplayedUserInDialogCount())
        recursiveFunc(displayedUserIdList)
      }
      case length: Int if (length >= userMaxCount) => list.distinct
      case _: Int => {
        driver.executeScript(s"${this.displayedUserRowInDialogElements}[${this.findDisplayedUserInDialogCount() - 1}].scrollIntoView()")
        Thread.sleep(3000)
        val displayedUserIdList = this.findDisplayedUserIdList(this.findDisplayedUserInDialogCount())
        recursiveFunc(List.concat(list, displayedUserIdList))
      }
    }

    recursiveFunc()
  }

  private def findDisplayableUserPostCount() = driver.executeScript(s"return $userPostElements.length")
    .toString
    .toInt

  //  スクロールが終端か判定
  private def canScrollInUserPostSpace() =
    this.executeScript("""return document.body.scrollHeight > document.documentElement.scrollTop + document.body.clientHeight""")
      .toString match {
      case "true" => true
      case _ => false
    }

  //  fixme:4~6割ほどしか取得しきれないので要修正
  private def findUserPostsIdList(): List[String] = {
    //    スクロールできる限り再帰的にIdを取得
    def recursiveFunc(list: List[String] = List(), cnt: Int = 0): List[String] = {
      if (cnt != 0) {
        driver.executeScript(s"$userPostElements[${this.findDisplayableUserPostCount() - 1}].scrollIntoView()")
        Thread.sleep(3000)
      }
      val displayedUserPostIdList = (0 to (findDisplayableUserPostCount() - 1)).toList
        .map(i => driver.executeScript(s"""return $userPostElements[$i].getElementsByTagName("a")[0].getAttribute("href")""").toString)
      if (canScrollInUserPostSpace()) recursiveFunc(List.concat(list, displayedUserPostIdList), cnt + 1)
      else List.concat(list, displayedUserPostIdList)
    }

    val pattern = "/p/(.*)/".r
    recursiveFunc().distinct.map({
      case pattern(id) => id
      case _ => ""
    })
      .filter(id => !id.isEmpty)
  }

  def createCollection() = {
    val followedUserCount = this.findSomeCountByRegexStr("""フォロワー([\d,]+)人""")
    val followedTriggerEle = driver.findElementByXPath("//section/main/div")
      .findElement(By.xpath("//ul/li[2]/a"))
    val followedUseIdList = this.findUserIdListInDialog(followedUserCount, followedTriggerEle)

    //ダイアログを消すためにグレーマスク部分をクリック
    driver.executeScript("""document.getElementsByClassName("RnEpo Yx5HN")[0].click()""")

    val followingUserCount = this.findSomeCountByRegexStr("""([\d,]+)人をフォロー中""")
    val followingTriggerEle = driver.findElementByXPath("//section/main/div")
      .findElement(By.xpath("//ul/li[3]/a"))
    val followingUseIdList = this.findUserIdListInDialog(followingUserCount, followingTriggerEle)

    new ProfileCollection(
      this.findUserId(),
      this.findSomeCountByRegexStr("""投稿([\d,]+)件"""),
      followedUserCount,
      followingUserCount,
      followedUseIdList,
      followingUseIdList,
      findUserPostsIdList()
    )
  }
}
