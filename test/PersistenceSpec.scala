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

    DynamoUtils.createAgentTableIfNotExists("someTable")
    DynamoUtils.deleteTableIfExists("someTable")
  }


  /** Checks that an attempt to create an existing table does not fail. */
  @Test
  def createIfNotExists(): Unit = {
    DynamoUtils.createAgentTableIfNotExists("someTable")
    DynamoUtils.createAgentTableIfNotExists("someTable")
  }


  /** Checks that an attempt to delete a non-existing table does not fail. */
  @Test
  def deleteIfNotExists(): Unit = {
    DynamoUtils.deleteTableIfExists("someTable")
    DynamoUtils.deleteTableIfExists("someTable")
  }
}
