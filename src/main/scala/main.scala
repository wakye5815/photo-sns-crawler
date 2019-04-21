import model.{LoginCookie}
import org.openqa.selenium.{Cookie}
import org.openqa.selenium.chrome.{ChromeDriver, ChromeOptions}
import DataBaseConnection.getConnection
import crawler.{PostsCrawler, ProfileCrawler, TagsCrawler}

import scala.concurrent.ExecutionContext.Implicits.global
import model.LoginCookie.LoginCookieEntity

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) throw new Exception("not enough parametor")

    val crawlerClass = args(0) match {
      case "posts" => PostsCrawler
      case "prof" => ProfileCrawler
      case "tags" => TagsCrawler
      case _ => throw new Exception(s"illegal parameter: ${args(0)}")
    }

    val db = getConnection()

    //    有効なクッキーがない場合、取得のためログイン
    val loginCookie = Await.result(LoginCookie(db).findAvailableOne, Duration.Inf) match {
      case Some(cookie) => cookie
      case None => {
        Await.result(new LoginCookieCollector(db).storeAfterCollect(), Duration.Inf)
        Await.result(LoginCookie(db).findAvailableOne, Duration.Inf).get
      }
    }

    val driver = createWebDriver()
    addLoginCookie(driver, loginCookie)

    val crawler = crawlerClass.apply(driver, args(1))
    println(crawler.createCollection().toString)
  }

  def createWebDriver() = {
    System.setProperty("webdriver.chrome.driver", "./selenium/chromeDriver/chromedriver.exe")
    val options = new ChromeOptions();
    options.addArguments("--headless")
    new ChromeDriver(options)
  }

  def addLoginCookie(driver: ChromeDriver, loginCookie: LoginCookieEntity) = {
    driver.get("https://www.instagram.com")
    driver.manage().addCookie(new Cookie("ds_user_id", loginCookie.dsUserId))
    driver.manage().addCookie(new Cookie("csrftoken", loginCookie.csrftoken))
    driver.manage().addCookie(new Cookie("mid", loginCookie.mid))
    driver.manage().addCookie(new Cookie("rur", loginCookie.rur))
    driver.manage().addCookie(new Cookie("urlgen", loginCookie.urlgen))
    driver.manage().addCookie(new Cookie("sessionid", loginCookie.sessionId))
  }

}