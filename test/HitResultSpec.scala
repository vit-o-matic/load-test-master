import models.SingleHitResult
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite

/**
  * Created by tomas.mccandless on 12/2/16.
  */
class HitResultSpec extends JUnitSuite with Matchers {

  /** Checks that we can deserialize json into a [[SingleHitResult]]. */
  @Test
  def fromJson(): Unit = {
    val json: String =
      """
      {"clientId":"a5a80330-daeb-465a-8c24-cceab9c9831d","targetUrl":"https://www.google.com","statusCode":200,"success":true,"totalTime":"200","message":""}
      """

    val expected: SingleHitResult = SingleHitResult(
      clientId = "a5a80330-daeb-465a-8c24-cceab9c9831d",
      targetUrl = "https://www.google.com",
      statusCode = 200,
      success = true,
      totalTime = 200,
      message = ""
    )
    
    SingleHitResult fromJson json should be (expected)
  }
}
