import com.typesafe.config.ConfigFactory
import slick.driver.MySQLDriver.api._

object DataBaseConnection {
  def getConnection() = {
    val mySqlConf = ConfigFactory.load().getConfig("mysql")
    val dbName = mySqlConf.getString("dbName")
    val user = mySqlConf.getString("user")
    val password = mySqlConf.getString("password")
    val host = mySqlConf.getString("host")
    val port = mySqlConf.getString("port")
    Database.forURL(s"jdbc:mysql://$host:$port/$dbName?&useSSL=false", driver = "com.mysql.cj.jdbc.Driver", user = user, password = password)
  }
}