import java.util.UUID

import models.{AgentDetail, AgentTimeZone, SingleHitResult}
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
  val agentTableName: String = UUID.randomUUID.toString
  // name of the table we'll store hit results in
  val resultTableName: String = UUID.randomUUID.toString

  @Before
  def createTables(): Unit = {
    DynamoUtils.createAgentTableIfNotExists(this.agentTableName)
    DynamoUtils.createResultTableIfNotExists(this.resultTableName)
  }


  @After
  def deleteTables(): Unit = {
    DynamoUtils.deleteTableIfExists(this.agentTableName)
    DynamoUtils.deleteTableIfExists(this.resultTableName)
  }


  /** Checks that we can read and write agent details. */
  @Test
  def persistAgentDetails(): Unit = {
    val detail: AgentDetail = AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1))
    // just write some agent detail into a fresh table, then assert that the table is not empty
    DynamoUtils.persistAgentDetail(this.agentTableName, detail)
    DynamoUtils.getAgentDetails(this.agentTableName) should not be empty
  }


  /** Checks that we can deregister an agent. */
  @Test
  def deleteAgentDetails(): Unit = {
    val detail: AgentDetail = AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1))
    // just write some agent detail into a fresh table, then assert that the table is not empty
    DynamoUtils.persistAgentDetail(this.agentTableName, detail)
    DynamoUtils.getAgentDetails(this.agentTableName) should not be empty

    // now remove the agent with that id, and we should expect to have an empty table again
    DynamoUtils.removeAgent(this.agentTableName, detail.id)
    DynamoUtils.getAgentDetails(this.agentTableName) should be (empty)
  }


  /** Checks that we can write [[SingleHitResult]] into dynamo. */
  @Test
  def persistSingleHitResult(): Unit = {
    val result: SingleHitResult = SingleHitResult(
      clientId = "some client id",
      targetUrl = "google.com",
      statusCode = 200,
      success = true,
      totalTime = 9000, // ms?
      // dynamo will complain if an attribute value is empty string
      message = ""
    )

    // just write some result into a fresh table, then assert the table is not empty
    DynamoUtils.persistHitResult(this.resultTableName, result)
    DynamoUtils.getHitResults(this.resultTableName) should not be empty
  }


  /** Checks that an attempt to create an existing table does not fail. */
  @Test
  def createIfNotExists(): Unit = {
    DynamoUtils.createAgentTableIfNotExists(this.agentTableName)
    DynamoUtils.createAgentTableIfNotExists(this.agentTableName)
  }


  /** Checks that an attempt to delete a non-existing table does not fail. */
  @Test
  def deleteIfExists(): Unit = {
    DynamoUtils.deleteTableIfExists(this.agentTableName)
    DynamoUtils.deleteTableIfExists(this.agentTableName)
  }
}
