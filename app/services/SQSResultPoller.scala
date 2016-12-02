package services

import javax.inject.{Inject, Singleton}

import akka.actor.{Actor, ActorSystem, PoisonPill, Props}
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.{GetQueueUrlRequest, ReceiveMessageRequest}
import play.api.inject.ApplicationLifecycle

import scala.collection.JavaConversions._
import models.HitResult.Implicits._
import models.SingleHitResult
import play.api.libs.json.Json
import play.api.Logger.logger
import scala.concurrent.Future

/**
  * @author Hussachai Puripunpinyo
  */
@Singleton
class SQSResultPoller @Inject() (system: ActorSystem,
  sqs: AmazonSQS, resultBus: ResultEventBus, appLifecycle: ApplicationLifecycle) {

  val queueName = "SingleHitResultQueue" //TODO: get from config

  case object Poll

  val pollerActor = system.actorOf(Props(new PollerActor))

  class PollerActor extends Actor {
    def receive = {
      case Poll =>
        readQueue
        self ! Poll
    }
  }

  pollerActor ! Poll

  def readQueue(): Seq[SingleHitResult] = {
    val queueUrl = sqs.getQueueUrl(new GetQueueUrlRequest(queueName)).getQueueUrl
    logger.debug(s"Reading queue: ${queueUrl}")
    val msgRequest = new ReceiveMessageRequest(queueUrl)
    msgRequest.setWaitTimeSeconds(15) //long polling
    sqs.receiveMessage(msgRequest).getMessages.map{ msg =>
      val result = Json.parse(msg.getBody).as[SingleHitResult]
      resultBus.publish(result)
      logger.debug(s"Received from SQS: ${msg.getBody}")
      val deleteResult = sqs.deleteMessage(queueUrl, msg.getReceiptHandle)
      logger.debug(s"SQS delete result: ${deleteResult}")
      result
    }
  }

  appLifecycle.addStopHook { () =>
    pollerActor ! PoisonPill
    Future.successful(())
  }
}
