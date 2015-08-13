package pirc.kpi

class Counter(val hvc: HistogramValueCalculator) {
  val hourly = new Histogram(24, 60*60, hvc)
  val daily = new Histogram(30, 60*60*24, hvc)
  val weekly = new Histogram(52, 60*60*24*7, hvc)
}

/**
 * The base Histogram class is here to accumulate counts of something over
 * certain periods of time.  Concrete subclasses will be responsible for
 * defining what the period of time is.  So, for example, let's say we
 * want to track the hourly totals for some operation over the course of
 * a day.  What this class will contain is a list of 24 Int's that contain
 * those hourly totals.  Whenever the bump() method is called, the current
 * hour's count will be bumped.  The class also tracks what it thinks the
 * "current" hour is, and if a bump or a retrieve (to pull the histogram)
 * comes in, and we find that we are no longer in the current hour, the
 * sample set is padded to the current hour and we begin counting with the
 * new current sample.
 */
class Histogram(val sampleCount: Int,
                 val secondsPerSample: Int,
                 val hvc: HistogramValueCalculator)
{
  var currentOffset: Long = _

  var samples = List[Int]()

 /**
  * Finds the current offset and uses the populate(offset) method to 
  * initialize the histogram to the correct values.*
  */
  def initialize = { 
    currentOffset = calculateOffset
    Range.Long(currentOffset+1 - sampleCount, currentOffset+1, 1)
      .foreach { offset => samples = hvc.calculateValue(offset)::samples }
  }

 /**
  * Figures out if the current front of the histogram is still correct,
  * pads the sample list with empty samples if not (thus creating a new
  * current sample) and returns the value of the current sample (which
  * will presumably be pre-pended on to the samples list again).
  */
  private def pad: Int = {
    val shouldBe = calculateOffset
    if(shouldBe != currentOffset) {
      println(s"shouldBe: ${shouldBe}, but is: ${currentOffset}")
      Range.Long( Math.max(currentOffset+1, shouldBe+1-sampleCount)
                , shouldBe+1, 1)
        .foreach { offset => samples = 0::samples }
      samples = samples.take(sampleCount)
      currentOffset = shouldBe
    }
    val result = samples.head
    samples = samples.tail
    result
  }

  def bump(amt: Int) = {
    samples = pad+amt :: samples
    samples
  }

  def calculateOffset: Long = {
    new java.util.Date().getTime / (1000*secondsPerSample)
  }
}

/**
 * This trait allows applications to define how to initialize the histogram
 * when this object is first instantiated, so that restarted the Histogram
 * does not result in just an empty graph.
 */
trait HistogramValueCalculator {
  def calculateValue(offset: Long): Int
}
