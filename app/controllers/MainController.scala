package controllers

import java.net.URLDecoder
import java.util.UUID
import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.sqs.AmazonSQS
import models._
import persistence.DynamoUtils
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import services.{CommandEventBus, ResultEventBus}

import scala.concurrent.Future
import play.api.libs.json._
import services.CommandEventBus.CommandMessage

/**
  * @author Hussachai Puripunpinyo
  */
@Singleton
class MainController @Inject() (
    system: ActorSystem,
    commandBus: CommandEventBus,
    resultBus: ResultEventBus,
    dynamo: DynamoDB,
    sqs: AmazonSQS) extends Controller {

  import LoadTest.Implicits._
  import system.dispatcher

  val logger = Logger.logger
  logger.debug("WHAT IS GOING ON")

  // we need to make sure the table exists, i think this could possibly be done elsewhere
  DynamoUtils.createAgentTableIfNotExists(AgentDetail.tableName)

  val configForm = Form(
    mapping(
      "numNodes" -> number,
      "loopCount" -> number,
      "config" -> mapping(
        "address" -> text,
        "port" -> number,
        "method" -> text,
        "headers" -> optional(text), //expecting format: key1=value1,key2=value2
        "body" -> optional(text)
      )(LoadTestConfig.applyForm)(LoadTestConfig.unapplyForm)
    )(LoadTest.apply)(LoadTest.unapply)
  )

  val agentForm = Form(
    mapping(
      "address" -> text,
      "userAgent" -> text,
      "timezone" -> mapping(
        "name" -> text,
        "offset" -> number
      )(AgentTimeZone.apply)(AgentTimeZone.unapply)
    )(AgentDetail.apply)(AgentDetail.unapply)
  )

  // POST this one is for testing result data stream on WebSocket
  def test = Action { implicit request =>
    val clientId = request.session.get("clientId").getOrElse("ID")
    logger.info("TEST.." + clientId)
    resultBus.publish(SingleHitResult(clientId, "tailrec.io", 200, true, 123, 12, "OK"))
    Ok
  }

  //GET
  def index = Action{ implicit request =>
    // uncomment this for a quick and dirty test
//    val registeredAgents: List[AgentDetail] = List(
//      AgentDetail("127.0.0.1", "some chrome agent", AgentTimeZone("PST", 1)),
//      AgentDetail("127.2.3.4", "some other chrome agent", AgentTimeZone("CST", 1))
//    )

    val registeredAgents: List[AgentDetail] = DynamoUtils.getAgentDetails(AgentDetail.tableName)

    logger.debug(s"found ${registeredAgents.size} registered agent(s)")
    request.session.get("clientId").map { clientId =>
      logger.debug(s"Found client ID: $clientId")
      Ok(views.html.main(clientId, registeredAgents))
    }.getOrElse {
      val clientId = UUID.randomUUID().toString
      logger.debug(s"Generated new client ID: $clientId")
      Ok(views.html.main(clientId, registeredAgents)).withSession("clientId" -> clientId)
    }
  }

  //GET
  def resultPushJs(clientId: String) = Action { implicit request =>
    val endpoint = routes.MainController.subscribeChart(clientId).webSocketURL()
    Ok(views.js.result_push(clientId, endpoint))
  }

  //GET
  def subscribeChart(clientId: String) = WebSocket.accept[String,String] { implicit header  =>
    val workerActor = system.actorOf(Props(new ResultActorPublisher(clientId, resultBus)))
    val publisher = ActorPublisher[String](workerActor)
    val source = Source.fromPublisher(publisher)
    Flow.fromSinkAndSource(Sink.foreach(println), source)
  }

  //GET
  def subscribeAgent(encodedData: String) = WebSocket.accept[String,String] { implicit header  =>

    val json = Json.parse(URLDecoder.decode(encodedData, "UTF-8"))
    val agentDetail: Option[AgentDetail] = agentForm.bind(json).value
    //TODO: persist this detail in DB. Dynamo?

    val workerActor = system.actorOf(Props(
      new CommandActorPublisher(commandBus)))
    val publisher = ActorPublisher[String](workerActor)
    val source = Source.fromPublisher(publisher)
    Flow.fromSinkAndSource(Sink.foreach(println), source)
  }

  //POST
  def startLoadTest = Action.async { implicit request =>
    configForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest)
      },
      loadTest => {
        Future {
          /**
            * We can publish to subset of agents but it needs many steps.
            *  1. Broadcast to all available agents
            *  2. Put agents that reply back into the same channel (keep track of the number of subscribers as well)
            *  3. Notify agents that they're registered in the channel.
            *  4. Refuse all responses coming from agents after the channel is full.
            * Oh man it's complicate. We will skip it for now. Just put everyone in the same channel for now.
            */
          val json = Json.toJson(loadTest.config) // TODO: use uPickle
          commandBus.publish(CommandMessage(json.toString()))
          //TODO: pass HTML template with LoadTest.config.id
          //We will use this id to subscribe WebSocket in order to receive stats data in real-time
          Ok//(views.html.show_stats(loadTest.config.id))
        }
      }
    )
  }


  class CommandActorPublisher(commandBus: CommandEventBus) extends ActorPublisher[String] {

    val channel = CommandEventBus.DefaultChannel
    commandBus.subscribe(self, channel)
    logger.debug("Chrome extension has subscribed to command pub/sub")

    def receive = {
      case cmd: CommandMessage =>
        logger.debug(s"Received: ${cmd} from CommandEventBus of channel: $channel")
        onNext(cmd.json)
      case Cancel =>
        commandBus.unsubscribe(self)
        logger.debug(s"Un-subscribed actor: $self from CommandEventBus of channel: $channel")
    }
  }

  class ResultActorPublisher(clientId: String, resultBus: ResultEventBus) extends ActorPublisher[String] {

    import HitResult.Implicits._
    
    resultBus.subscribe(self, clientId)
    logger.info(s"ClientId: ${clientId} has subscribed to result stream pub/sub")

    def receive = {
      case result: SingleHitResult =>
        logger.debug(s"Received: ${result} from ResultEventBus of id: $clientId")
        val json = Json.toJson(result).toString()
        onNext(json)
      case Cancel =>
        commandBus.unsubscribe(self)
        logger.debug(s"Un-subscribed actor: $self from ResultEventBus of id: $clientId")
    }
  }
}

