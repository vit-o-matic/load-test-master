import models.{AgentDetail, AgentTimeZone}
import org.junit.Test
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import persistence.DynamoUtils

/**
  * Created by tomas.mccandless on 12/1/16.
  */
class PersistenceSpec extends JUnitSuite with Matchers {

  @Test
  def persistAgentDetails(): Unit = {
    val detail: AgentDetail = AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1))

    DynamoUtils.createAgentDetailTable("someTable")
    DynamoUtils.deleteTable("someTable")
  }
}
