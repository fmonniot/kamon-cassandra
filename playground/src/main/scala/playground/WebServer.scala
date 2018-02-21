package playground

import akka.actor.ActorSystem
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.Future


class WebServer(cass: Cass, config: Config)(implicit mat: ActorMaterializer, sys: ActorSystem) extends FailFastCirceSupport {

  private implicit val ec = sys.dispatcher

  private val port: Int = config.getInt("http.port")
  private val interface: String = config.getString("http.interface")

  private def remoteHost = s"${config.getString("services.ip-api.host")}:${config.getString("services.ip-api.port")}"

  val routes: Route = {
    get {
      path("ok") {
        complete {
          "ok"
        }
      } ~
        path("go-to-outside") {
          complete {
            Http().singleRequest(HttpRequest(uri = s"http://$remoteHost/"))
          }
        } ~
        path("go-to-cass") {
          complete {
            cass.query("a").map(row => OK -> row)
          }
        } ~
        path("internal-error") {
          complete(HttpResponse(InternalServerError))
        } ~
        path("fail-with-exception") {
          throw new RuntimeException("Failed!")
        }
    }
  }

  def bind(): Future[Http.ServerBinding] = Http().bindAndHandle(routes, interface, port)

}

object WebServer {

}