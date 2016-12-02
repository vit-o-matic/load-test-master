package models

import java.util.UUID

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.model.{AttributeDefinition, KeySchemaElement, KeyType}
import play.api.libs.json.Json
import org.json4s._
import org.json4s.jackson.JsonMethods._

/**
  * @author Hussachai Puripunpinyo
  */
case class SingleHitResult (
  clientId: String,
  targetUrl: String,
  statusCode: Int,
  success: Boolean,
  totalTime: Long, // round trip time
  message: String
) {

  // uniqueness in dynamo
  val id: String = UUID.randomUUID.toString

  /**
    * Converts this [[SingleHitResult]] into an [[Item]] that can be persisted in dynamo.
    *
    * @return an [[Item]] with the same fields as in this [[SingleHitResult]].
    */
  def toItem: Item = {
    val resultItem: Item = new Item()
      .`with`("id", this.id)
      .`with`("clientId", this.clientId)
      .`with`("targetUrl", this.targetUrl)
      .`with`("statusCode", this.statusCode)
      .`with`("success", this.success)
      .`with`("totalTime", this.totalTime)

    // dynamo will complain if we send an attribute that is an empty string
    if (this.message.nonEmpty) resultItem.`with`("message", this.message) else resultItem
  }
}

object SingleHitResult {
  // for production, we'll use this table name to store registered agents.
  // for testing, we use a random uuid as the table name -- see PersistenceSpec
  val tableName: String = "SingleHitResult"

  // keys we are using, must be the same number of keys as attributes below, but we can add other fields later that are
  // not indexed
  // TODO this can be consolidated with similar code in AgentDetail
  val idKey: KeySchemaElement = new KeySchemaElement("id", KeyType.HASH)

  val id: AttributeDefinition = new AttributeDefinition("id", "S")

  val keys: List[KeySchemaElement] = List(this.idKey)
  val attributes: List[AttributeDefinition] = List(this.id)

  /**
    * Reconstructs a [[SingleHitResult]] from an [[Item]] retrieved from DynamoDB.
    * @param item
    * @return
    */
  def fromItem(item: Item): SingleHitResult = {
    val clientId: String = item.getString("clientId")
    val targetUrl: String = item.getString("targetUrl")
    val statusCode: Int = item.getInt("statusCode")
    val success: Boolean = item.getBoolean("success")
    val totalTime: Long = item.getLong("totalTime")
    // dynamo complains if an attribute has empty string as its value, so we omit that field
    // entirely during serialization, now we have to add it back
    val message: String = Option(item.getString("message")) match {
      case Some(msg) => msg
      case None => ""
    }

   SingleHitResult(clientId, targetUrl, statusCode, success, totalTime, message)
  }


  /**
    * Deserializes `serialized` into a [[SingleHitResult]].
    *
    * @param serialized json form of a [[SingleHitResult]].
    * @return a [[SingleHitResult]] obtained by deserializing `serialized`.
    */
  // TODO could be nicer, kind of cluttered, maybe use play json libs?
  def fromJson(serialized: String): SingleHitResult = {
    val json: JValue = parse(serialized)

    val clientId: String = ((json \ "clientId").toOption map { _.values.toString }).get
    val targetUrl: String = ((json \ "targetUrl").toOption map { _.values.toString }).get
    val statusCode: Int = ((json \ "statusCode").toOption map { _.values.toString.toInt }).get
    val success: Boolean = ((json \ "success").toOption map { _.values.toString.toBoolean }).get
    val totalTime: Long = ((json \ "totalTime").toOption map { _.values.toString.toLong }).get
    val message: String = ((json \ "message").toOption map { _.values.toString }).get

    SingleHitResult(clientId, targetUrl, statusCode, success, totalTime, message)
  }
}


/**
  * We should use this one instead of SingleHitResult if we decide to use Lambda.
  *
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