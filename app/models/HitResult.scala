package models

/**
  * @author Hussachai Puripunpinyo
  */
case class SingleHitResult (
  id: String, // this id must match with LoadTest.id
  success: Boolean,
  totalTime: Long, // round trip time
  processingTime: Long //processing time
)

/**
  * We should use this one instead of SingleHitResult if we decide to use Lambda.
  * @param id
  * @param results
  */
case class MultipleHitResults (
  id: String,
  results: Seq[SingleHitResult]
)
