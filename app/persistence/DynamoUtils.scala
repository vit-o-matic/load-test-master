package persistence

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item, Table}
import com.amazonaws.services.dynamodbv2.model._
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

  val dynamo: DynamoDB = new DynamoDB(new AmazonDynamoDBClient(ConfigUtils.awsCredentials))

  /**
    * Deletes the specified table from dynamo.
    *
    * @param tableName
    */
  def deleteTable(tableName: String): Unit = {
    val table: Table = this.dynamo.getTable(tableName)
    table.delete()
    table.waitForDelete()
  }


  // TODO seems like its a good idea to have a default table name that just uses tests
  def createAgentDetailTable(tableName: String): Unit = {
    val createTable: CreateTableRequest = new CreateTableRequest()
      .withProvisionedThroughput(this.throughput)
      .withTableName(tableName)
      .withKeySchema(AgentDetail.keys)
      .withAttributeDefinitions(AgentDetail.attributes)

    val table: Table = this.dynamo.createTable(createTable)
    table.waitForActive()
  }


  def persistAgentDetail(detail: AgentDetail): Unit = {
    val item: Item = detail.toItem
    // TODO persist the item
  }


  def getAgentDetails(): List[AgentDetail] = {
    List.empty
  }
}
