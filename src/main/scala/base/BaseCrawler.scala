package base

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.support.ui.{ExpectedConditions, WebDriverWait}

trait BaseCollection

trait BaseCrawler {
  val driver: ChromeDriver
  val targetUrl: String
  protected val expectedUrlPattern: String

  protected def moveTargetPage(): Unit = {
    require(targetUrl.matches(expectedUrlPattern))
    this.driver.get(this.targetUrl)
    val wait = new WebDriverWait(this.driver, 30)
    wait.until(ExpectedConditions.urlToBe(this.targetUrl))
  }

  protected def executeScript(script: String) = this.driver.asInstanceOf[JavascriptExecutor]
    .executeScript(script)

  def createCollection(): BaseCollection
}
