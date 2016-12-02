package persistence

import com.amazonaws.services.dynamodbv2.{AmazonDynamoDB, AmazonDynamoDBClient}
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item, Table}
import com.amazonaws.services.dynamodbv2.model._
import com.amazonaws.services.dynamodbv2.util.TableUtils
import models.AgentDetail
import play.api.Logger
import util.ConfigUtils

import scala.collection.JavaConversions.seqAsJavaList
/**
  * Created by tomas.mccandless on 12/1/16.
  */
object DynamoUtils {

  val logger: Logger = Logger(this.getClass)

  // provisioned throughput: read capacity and write capacity
  val throughput: ProvisionedThroughput = new ProvisionedThroughput(10L, 10L)


  val client: AmazonDynamoDB = new AmazonDynamoDBClient(ConfigUtils.awsCredentials)
  val dynamo: DynamoDB = new DynamoDB(this.client)

  /**
    * Deletes the specified table from dynamo if it exists.
    *
    * @param tableName
    */
  def deleteTableIfExists(tableName: String): Unit = {
    val table: Table = this.dynamo.getTable(tableName)
    TableUtils.deleteTableIfExists(this.client, new DeleteTableRequest(tableName))
    table.waitForDelete()
  }


  /**
    * Creates a table with `tableName` with the agent schema only if it does not already exist.
    *
    * @param tableName
    */
  def createAgentTableIfNotExists(tableName: String): Unit = {
    val createTable: CreateTableRequest = new CreateTableRequest()
      .withProvisionedThroughput(this.throughput)
      .withTableName(tableName)
      .withKeySchema(AgentDetail.keys)
      .withAttributeDefinitions(AgentDetail.attributes)

    val table: Table = this.dynamo.getTable(tableName)
    TableUtils.createTableIfNotExists(this.client, createTable)
    table.waitForActive()
  }


  def persistAgentDetail(detail: AgentDetail): Unit = {
    val item: Item = detail.toItem
    // TODO persist the item
  }


  def getAgentDetails(): List[AgentDetail] = {
    // TODO read back from the table
    List.empty
  }
}
