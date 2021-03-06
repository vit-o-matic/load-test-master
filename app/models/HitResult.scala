package models

import play.api.libs.json.Json

/**
  * @author Hussachai Puripunpinyo
  */
case class SingleHitResult (
  clientId: String,
  agentAddress: String,
  targetUrl: String,
  statusCode: Int,
  success: Boolean,
  totalTime: Long, // round trip time
  message: String
)

/**
  * We should use this one instead of SingleHitResult if we decide to use Lambda.
  * @param id
  * @param results
  */
case class MultipleHitResults (
  id: String,
  results: Seq[SingleHitResult]
)

object HitResult {
  object Implicits {
    implicit val SingleHitResultFormat = Json.format[SingleHitResult]
    implicit val MultipleHitResultsFormat = Json.format[MultipleHitResults]
  }
}