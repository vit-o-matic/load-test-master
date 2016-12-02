import java.util.UUID

import models.{AgentDetail, AgentTimeZone}
import org.junit.{After, Before, Test}
import org.scalatest.Matchers
import org.scalatest.junit.JUnitSuite
import persistence.DynamoUtils

/**
  * Created by tomas.mccandless on 12/1/16.
  */
class PersistenceSpec extends JUnitSuite with Matchers {

  // use some random identifier so we're less likely to step on each other if someone else is running tests
  // using the same aws credentials
  val tableName: String = UUID.randomUUID.toString

  @Before
  def createTables(): Unit = DynamoUtils.createAgentTableIfNotExists(this.tableName)


  @After
  def deleteTables(): Unit = DynamoUtils.deleteTableIfExists(this.tableName)


  /** Checks that we can read and write agent details. */
  @Test
  def persistAgentDetails(): Unit = {
    val detail: AgentDetail = AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1))
    // just write some agent detail into a fresh table, then assert that the table is not empty
    DynamoUtils.persistAgentDetail(this.tableName, detail)
    DynamoUtils.getAgentDetails(this.tableName) should not be empty
  }


  /** Checks that we can deregister an agent. */
  @Test
  def deleteAgentDetails(): Unit = {
    val detail: AgentDetail = AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1))
    // just write some agent detail into a fresh table, then assert that the table is not empty
    DynamoUtils.persistAgentDetail(this.tableName, detail)
    DynamoUtils.getAgentDetails(this.tableName) should not be empty

    // now remove the agent with that id, and we should expect to have an empty table again
    DynamoUtils.removeAgent(this.tableName, detail.id)
    DynamoUtils.getAgentDetails(this.tableName) should be (empty)
  }


  /** Checks that an attempt to create an existing table does not fail. */
  @Test
  def createIfNotExists(): Unit = {
    DynamoUtils.createAgentTableIfNotExists(this.tableName)
    DynamoUtils.createAgentTableIfNotExists(this.tableName)
  }


  /** Checks that an attempt to delete a non-existing table does not fail. */
  @Test
  def deleteIfExists(): Unit = {
    DynamoUtils.deleteTableIfExists(this.tableName)
    DynamoUtils.deleteTableIfExists(this.tableName)
  }
}
