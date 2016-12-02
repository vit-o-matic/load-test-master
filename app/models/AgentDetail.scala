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
  val idKey: KeySchemaElement = new KeySchemaElement("id", KeyType.HASH)
  val addressKey: KeySchemaElement = new KeySchemaElement("address", KeyType.RANGE)

  val id: AttributeDefinition = new AttributeDefinition("id", "S")
  val address: AttributeDefinition = new AttributeDefinition("address", "S")

  val keys: List[KeySchemaElement] = List(this.idKey, this.addressKey)
  val attributes: List[AttributeDefinition] = List(this.id, this.address)

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
