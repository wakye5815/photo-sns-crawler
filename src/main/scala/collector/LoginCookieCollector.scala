import Main.createWebDriver
import model.{CrawlAccount, LoginCookie}
import model.CrawlAccount.CrawlAccountEntity
import org.openqa.selenium.{By, Cookie, JavascriptExecutor, WebDriver, WebElement}
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.concurrent.{Await}
import scala.concurrent.duration.Duration

case class LoginCookieCollector(db: Database) {

  def storeAfterCollect() = {
    val futureCookieTuple = CrawlAccount(db).findLoginableOne()
      .map(_.map(account => (account.id, createLoginCookieMap(account))))
      .map({
        case Some(cookieTupleOpt) => cookieTupleOpt._2 match {
          case Some(cookieMap) => (cookieTupleOpt._1, cookieMap)
          case None => throw new Exception("fail generate login cookie")
        }
        case None => throw new Exception("not found loginable account")
      })

    val cookieTuple = Await.result(futureCookieTuple, Duration.Inf)
    new LoginCookie(db).create(cookieTuple._1, cookieTuple._2)
  }

  //  Future内でwebDriverを生成しないとwebDriverException→Futureは別スレッドだから？？
  private def createLoginCookieMap(account: CrawlAccountEntity) = {
    val driver = createWebDriver()
    this.login(driver, account)
    val loginCookiesMap = this.findLoginCookieFromWebDriver(driver)
    driver.close()
    loginCookiesMap
  }

  private def findLoginCookieFromWebDriver(webDriver: WebDriver): Option[Map[String, String]] = {
    val cookie = webDriver.manage().getCookies.asScala.toList

    def recursiveFunc(retryCnt: Int = 5): Option[Map[String, String]] = {
      if (isLoginCookie(cookie)) Some(Map(
        "mid" -> cookie.find(_.getName == "mid").get.getValue,
        "sessionid" -> cookie.find(_.getName == "sessionid").get.getValue,
        "csrftoken" -> cookie.find(_.getName == "csrftoken").get.getValue,
        "dsUserId" -> cookie.find(_.getName == "ds_user_id").get.getValue,
        "rur" -> cookie.find(_.getName == "rur").get.getValue,
        "urlgen" -> cookie.find(_.getName == "urlgen").get.getValue
      ))
      else if (retryCnt == 0) None
      else {
        Thread.sleep(3000)
        recursiveFunc(retryCnt - 1)
      }
    }

    recursiveFunc()
  }


  private def isLoginCookie(cookies: List[Cookie]) = cookies.filter(c => c.getDomain == ".instagram.com")
    .count(c => c.getName match {
      case "mid" => true
      case "sessionid" => true
      case "csrftoken" => true
      case "ds_user_id" => true
      case "rur" => true
      case "urlgen" => true
      case _ => false
    }) == 6

  private def login(webDriver: WebDriver, account: CrawlAccountEntity) = {
    webDriver.get("https://www.instagram.com/accounts/login/")
    webDriver.findElements(By.xpath("//*[@class='_2hvTZ pexuQ zyHYP']")).asScala.toList
      .foreach((form: WebElement) => form.getAttribute("name") match {
        case "username" => form.sendKeys(account.instagramId)
        case "password" => form.sendKeys(account.password)
        case _ => throw new Exception("login error")
      })
    webDriver.asInstanceOf[JavascriptExecutor]
      .executeScript("document.getElementsByClassName(\"_0mzm- sqdOP L3NKy\")[0].click()")
  }
}