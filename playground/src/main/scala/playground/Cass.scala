package playground

import com.datastax.driver.core.{Cluster, ResultSet, Session}
import com.google.common.util.concurrent.{FutureCallback, Futures}
import com.typesafe.config.Config
import playground.Cass.MyTableRow

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Promise}
import scala.util.control.NonFatal

class Cass(config: Config) {

  private val cluster = Cluster.builder()
    .addContactPoint(config.getString("cassandra.contact-point"))
    .withPort(config.getInt("cassandra.port"))
    .build()
  val session: Session = cluster.connect()


  def prepare(): Unit = {
    session.execute(
      """CREATE KEYSPACE IF NOT EXISTS ks
        | WITH replication = { 'class': 'SimpleStrategy', 'replication_factor': '1' }
        | """.stripMargin
    )

    session.execute(
      """CREATE TABLE IF NOT EXISTS ks.mytable (
        |            thekey text,
        |            col1 text,
        |            col2 text,
        |            PRIMARY KEY (thekey)
        |        )
      """.stripMargin
    )

    session.execute("""INSERT INTO ks.mytable (thekey, col1, col2) VALUES ('a', 'b', 'c');""")
  }

  def query(key: String): Future[Option[MyTableRow]] = {
    val select = "SELECT * FROM ks.mytable where thekey = ?"

    val p = Promise[Option[MyTableRow]]

    Futures.addCallback(session.executeAsync(select, key), new FutureCallback[ResultSet] {
      override def onFailure(t: Throwable): Unit = p.failure(t)

      override def onSuccess(result: ResultSet): Unit = try {
        val row = Option(result.one())

        p.success(row.map(r => MyTableRow(
          r.getString("thekey"),
          r.getString("col1"),
          r.getString("col2")
        )))

      } catch {
        case NonFatal(t) => p.failure(t)
      }
    })

    p.future
  }

}

object Cass {

  case class MyTableRow(key: String, col1: String, col2: String)

}
