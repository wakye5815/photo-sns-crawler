name := "instagramCrawler"

version := "0.1"

scalaVersion := "2.11.8"

lazy val slickVersion = "3.1.1"
libraryDependencies ++= Seq(
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.0",
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "mysql" % "mysql-connector-java" % "8.0.11",
  "com.typesafe" % "config" % "1.3.1"
)