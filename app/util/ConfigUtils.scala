package util

import com.amazonaws.auth.BasicAWSCredentials
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Provides access to the current configuration.
  *
  * Created by tomas.mccandless on 12/1/16.
  */
object ConfigUtils {

  private val _config: Config = ConfigFactory.load
  def config: Config = this._config

  /**
    * @return [[BasicAWSCredentials]] based on what is in the current config.
    */
  def awsCredentials: BasicAWSCredentials = {
    new BasicAWSCredentials(
      this._config.getString("aws.accessKeyId"),
      this._config.getString("aws.secretAccessKey")
    )
  }
}
