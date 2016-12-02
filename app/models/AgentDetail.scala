package models

import java.util.UUID

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, KeySchemaElement, KeyType}

/**
  * Tracks details of registered chrome extension users.
  *
  * @author Hussachai Puripunpinyo
  */
case class AgentDetail (
  address: String, //ip address of the node
  userAgent: String, //navigator.userAgent
  //To get Chrome version: userAgent.substring(userAgent.indexOf("Chrome/") + 7, userAgent.lastIndexOf(" "))
  timezone: AgentTimeZone
) {

  // uniqueness in dynamo
  val id: String = UUID.randomUUID().toString

  /**
    * Converts this [[AgentDetail]] into an [[Item]] that can be persisted in dynamo.
    *
    * @return an [[Item]] with the same fields as in this [[AgentDetail]].
    */
  def toItem: Item = {
    new Item()
      .`with`("id", this.id)
      .`with`("address", this.address)
      .`with`("userAgent", this.userAgent)
      .`with`("timezoneName", this.timezone.name)
      .`with`("timezoneOffset", this.timezone.offset)
  }
}


// holds the schema keys and attributes for agent detail
object AgentDetail {
  // for production, we'll use this table name to store registered agents.
  // for testing, we use a random uuid as the table name -- see PersistenceSpec
  val tableName: String = "AgentDetail"

  // keys we are using, must be the same number of keys as attributes below, but we can add other fields later that are
  // not indexed
  val idKey: KeySchemaElement = new KeySchemaElement("id", KeyType.HASH)

  val id: AttributeDefinition = new AttributeDefinition("id", "S")

  val keys: List[KeySchemaElement] = List(this.idKey)
  val attributes: List[AttributeDefinition] = List(this.id)

  /**
    * Reconstructs an [[AgentDetail]] from an [[Item]] retrieved from DynamoDB.
    *
    * @param item
    * @return
    */
  def fromItem(item: Item): AgentDetail = {
    // TODO we might want to include id in the agentdetail constructor -- not sure
    // val id: String = item.getString("id")
    val address: String = item.getString("address")
    val userAgent: String = item.getString("userAgent")
    val timezoneName: String = item.getString("timezoneName")
    val timezoneOffset: Int = item.getInt("timezoneOffset")
    new AgentDetail(address, userAgent, new AgentTimeZone(timezoneName, timezoneOffset))
  }
}


case class AgentTimeZone (
  name: String, //Intl.DateTimeFormat().resolvedOptions().timeZone
  offset: Int //new Date().getTimezoneOffset() / -60
)
