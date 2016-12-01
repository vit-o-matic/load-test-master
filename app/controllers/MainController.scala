package controllers

import java.net.URLDecoder
import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.sqs.AmazonSQS
import models._
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

  val logger = Logger(getClass)

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


  //GET Only
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
            * We can publish to subset of agents but it needs two steps.
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

    def receive = {
      case cmd: CommandMessage =>
        logger.debug(s"Received: ${cmd} from CommandEventBus of channel: $channel")
        onNext(cmd.json)
      case Cancel =>
        commandBus.unsubscribe(self)
        logger.debug(s"Un-subscribed actor: $self from CommandEventBus of channel: $channel")
    }
  }

  class ResultActorPublisher(id: String, resultBus: ResultEventBus) extends ActorPublisher[String] {

    resultBus.subscribe(self, id)

    def receive = {
      case result: SingleHitResult =>
        logger.debug(s"Received: ${result} from ResultEventBus of id: $id")
        val json = result.toString //TODO: toJson here
        onNext(json)
      case Cancel =>
        commandBus.unsubscribe(self)
        logger.debug(s"Un-subscribed actor: $self from ResultEventBus of id: $id")
    }
  }
}

