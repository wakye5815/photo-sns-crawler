package crawler

import base.{BaseCollection, BaseCrawler}
import org.openqa.selenium.chrome.ChromeDriver

import scala.collection.JavaConverters._

case class TagsCollection(tagName: String, postsCount: Long) extends BaseCollection

case class TagsCrawler(driver: ChromeDriver, targetUrl: String) extends BaseCrawler {
  val expectedUrlPattern = "https://www.instagram.com/explore/tags/.*/"
  super.moveTargetPage()

  private def findPostsCount() = this.driver.findElementByClassName("g47SY")
    .getText()
    .toLong

  private def findTagName() = this.driver.findElementByTagName("h1")
    .getText()
    .replaceAll("#", "")

  def findPopularPostsUrlList() = this.driver.findElementsByXPath("//*[@class='v1Nh3 kIKUG  _bz0w']/a")
    .asScala.toList.slice(0, 9)
    .map(ele => ele.getAttribute("href"))

  def createCollection() = new TagsCollection(
    this.findTagName(),
    this.findPostsCount()
  )
}
