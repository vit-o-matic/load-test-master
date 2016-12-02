package models


import play.api.libs.json.Json


/**
  * @author Hussachai Puripunpinyo
  */
case class LoadTest(
  numNodes: Int, // this number cannot exceed the total number of online slaves
  // we can add thread numbers per node, but let's keep it simple
  config: LoadTestConfig
)

object LoadTest {

  object Implicits {
    implicit val HttpHeaderFormat = Json.format[HttpHeader]
    implicit val HttpMethodFormat = Json.format[HttpMethod]
    implicit val LoadTestConfigFormat = Json.format[LoadTestConfig]
    implicit val LoadTestFormat = Json.format[LoadTest]
  }
}

case class LoadTestConfig (
  clientId: String,
  loopCount: Int,
  targetUrl: String,
  method: HttpMethod,
  headers: Seq[HttpHeader] = Nil,
  body: Option[String] = None
)

object LoadTestConfig {

  object HeaderParser {
    def unapply(header: String): Option[HttpHeader] = {
      val kv = header.split("=")
      if(kv.length == 2) Some(HttpHeader(kv(0).trim, kv(1).trim))
      else None
    }
  }

  def applyForm(clienId: String, loopCount: Int, targetUrl: String, method: String,
    headers: Option[String], body: Option[String]): LoadTestConfig = {
    LoadTestConfig(
      clienId,
      loopCount,
      targetUrl,
      HttpMethod(method),
      headers.map(_.split(",").collect { case HeaderParser(h) => h }.toSeq).getOrElse(Nil),
      body
    )
  }

  def unapplyForm(config: LoadTestConfig) = {
    Option(config).map { m =>
      val headers = if(m.headers.isEmpty) None else Some(m.headers.mkString(","))
      (m.clientId, m.loopCount, m.targetUrl, m.method.name, headers, m.body)
    }
  }

}
case class HttpMethod(name: String)

object HttpMethods {
  val Get = HttpMethod("GET")
  val Post = HttpMethod("POST")
  val Put = HttpMethod("PUT")
  val Delete = HttpMethod("DELETE")
}

case class HttpHeader (
  name: String,
  value: String
) {
  override def toString(): String = s"$name=$value"
}