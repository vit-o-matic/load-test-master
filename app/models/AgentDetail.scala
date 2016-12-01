package models

/**
  * @author Hussachai Puripunpinyo
  */
case class AgentDetail (
  address: String, //ip address of the node
  userAgent: String, //navigator.userAgent
  //To get Chrome version: userAgent.substring(userAgent.indexOf("Chrome/") + 7, userAgent.lastIndexOf(" "))
  timezone: AgentTimeZone
)

case class AgentTimeZone (
  name: String, //Intl.DateTimeFormat().resolvedOptions().timeZone
  offset: Int //new Date().getTimezoneOffset() / -60
)
