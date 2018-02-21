package playground

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import kamon.Kamon
import kamon.prometheus.PrometheusReporter
import kamon.trace.Span
import kamon.zipkin.ZipkinReporter

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutor

object Main extends App {

  Kamon.addReporter(new PrometheusReporter())
  Kamon.addReporter(new ZipkinReporter() {
    override def reportSpans(spans: Seq[Span.FinishedSpan]): Unit = {
      println(s"reporting ${spans.length} span to ZipKin")
      super.reportSpans(spans)
    }
  })

  val config = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem()
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val logger = Logging(system, getClass)

  val cass = new Cass(config)
  cass.prepare()

  val server = new WebServer(cass, config)
  val requests = new RequestsGenerator

  server.bind().map { binding ⇒

    logger.info(s"Server online at http://${binding.localAddress}/\nPress RETURN to stop...")

    val makingRequests = requests.activate(
      config.getDuration("requests.interval"),
      config.getStringList("requests.endpoints").asScala.toList
    )
  }

}
