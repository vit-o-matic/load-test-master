import com.google.inject.AbstractModule
import java.time.Clock
import javax.inject.{Inject, Provider}

import com.amazonaws.auth.{AWSCredentials, BasicAWSCredentials}
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.sns.{AmazonSNS, AmazonSNSClient}
import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClient}
import play.api.{Configuration, Environment, Logger}
import services._

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module(env: Environment, config: Configuration) extends AbstractModule {

  lazy val awsCredentials = new BasicAWSCredentials(
    config.getString("aws.accessKeyId").get,
    config.getString("aws.secretAccessKey").get)

  lazy val awsRegion = Region.getRegion(Regions.valueOf(config.getString("aws.region").get))

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])

    bind(classOf[AWSCredentials]).toInstance{
      new BasicAWSCredentials(
        config.getString("aws.accessKeyId").get,
        config.getString("aws.secretAccessKey").get)
    }
    bind(classOf[Region]).toInstance{
      Region.getRegion(Regions.valueOf(config.getString("aws.region").get))
    }
    bind(classOf[AmazonSNS]).toProvider(classOf[AmazonSNSProvider]).asEagerSingleton()
    bind(classOf[AmazonSQS]).toProvider(classOf[AmazonSQSProvider]).asEagerSingleton()
    bind(classOf[DynamoDB]).toProvider(classOf[DynamoDBProvider]).asEagerSingleton()
    bind(classOf[CommandEventBus]).toInstance(new CommandEventBus())
    bind(classOf[ResultEventBus]).toInstance(new ResultEventBus())
  }

}

class AmazonSNSProvider @Inject()(credentials: AWSCredentials, region: Region) extends Provider[AmazonSNSClient] {
  override def get() = {
    val client = new AmazonSNSClient(credentials)
    client.setRegion(region)
    client
  }
}

class AmazonSQSProvider @Inject()(credentials: AWSCredentials, region: Region) extends Provider[AmazonSQS] {
  override def get() = {
    val client = new AmazonSQSClient(credentials)
    client.setRegion(region)
    client
  }
}

class DynamoDBProvider @Inject()(credentials: AWSCredentials, region: Region) extends Provider[DynamoDB] {
  override def get() = {
    val client = new AmazonDynamoDBClient(credentials)
    client.setRegion(region)
    new DynamoDB(client)
  }
}
