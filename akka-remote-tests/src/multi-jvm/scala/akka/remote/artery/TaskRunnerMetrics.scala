package akka.remote.artery

import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.remote.RemoteActorRefProvider
import org.HdrHistogram.Histogram
import java.util.concurrent.TimeUnit.SECONDS

class TaskRunnerMetrics(system: ActorSystem) {

  private var entryOffset = 0

  def printHistograms(): Unit = {
    val aeronSourceHistogram = new Histogram(SECONDS.toNanos(10), 3)
    val aeronSinkHistogram = new Histogram(SECONDS.toNanos(10), 3)
    system.asInstanceOf[ExtendedActorSystem].provider.asInstanceOf[RemoteActorRefProvider].transport match {
      case a: ArteryTransport ⇒
        var c = 0
        val reader = new FlightRecorderReader(a.afrFileChannel)
        reader.structure.hiFreqLog.logs.foreach(_.compactEntries.foreach { entry ⇒
          c += 1
          if (c > entryOffset) {
            if (entry.code == FlightRecorderEvents.AeronSource_ReturnFromTaskRunner)
              aeronSourceHistogram.recordValue(entry.param)
            else if (entry.code == FlightRecorderEvents.AeronSink_ReturnFromTaskRunner)
              aeronSinkHistogram.recordValue(entry.param)
          }
        })
        entryOffset = c

        if (aeronSourceHistogram.getTotalCount > 0) {
          println("Histogram of AeronSource tasks in microseconds.")
          aeronSourceHistogram.outputPercentileDistribution(System.out, 1000.0)
        }

        if (aeronSinkHistogram.getTotalCount > 0) {
          println("Histogram of AeronSink tasks in microseconds.")
          aeronSinkHistogram.outputPercentileDistribution(System.out, 1000.0)
        }
      case _ ⇒
    }
  }

}
