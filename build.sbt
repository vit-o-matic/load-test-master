name := """load-test-master"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "com.amazonaws" % "aws-java-sdk-sns" % "1.11.62",
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.11.62",
  "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.11.62",
  "xml-apis" % "xml-apis" % "1.4.01",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

