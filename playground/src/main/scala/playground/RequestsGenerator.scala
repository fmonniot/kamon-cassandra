package playground

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class RequestsGenerator(implicit system: ActorSystem, mat: ActorMaterializer) {

  implicit val ec: ExecutionContext = system.dispatcher

  val logger = Logging(system, getClass)

  def activate(tickInterval: java.time.Duration, endpoints: List[String],
               interface: String = "localhost", port: Int = 8080): Cancellable = {

    val timeout = 2 seconds
    val interval = FiniteDuration(tickInterval.getSeconds, "s")

    val decider: Supervision.Decider = exc ⇒ {
      logger.error(s"Request Generator Stream failed. It will restart in seconds.", exc)
      Supervision.Restart
    }

    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] = Http().outgoingConnection(interface, port)
    val tickSource = Source.tick(interval, interval, NotUsed)

    tickSource
      .map(_ ⇒ {
        val httpRequest = HttpRequest(uri = endpoints(Random.nextInt(endpoints.size)))
        logger.info(s"Request: ${httpRequest.getUri()}")
        httpRequest
      })
      .via(connectionFlow)
      .to(Sink.foreach { httpResponse ⇒ httpResponse.toStrict(timeout) })
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      .run()

  }

}
