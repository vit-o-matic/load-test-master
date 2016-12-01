package services

import javax.inject.{Inject, Singleton}

import com.amazonaws.services.sqs.AmazonSQS
import play.api.inject.ApplicationLifecycle

/**
  * @author Hussachai Puripunpinyo
  */
@Singleton
class SQSResultPoller @Inject() (sqs: AmazonSQS, resultBus: ResultEventBus, appLifecycle: ApplicationLifecycle) {

  //create akka scheduler to pull data from AmazonSQS and publish the result to ResultEventBus

}
