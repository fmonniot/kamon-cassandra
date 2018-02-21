/*
 * Copyright © 2018 François Monniot <https://github.com/fmonniot/kamon-cassandra>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the
 *  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific language governing permissions
 *  and limitations under the License.
 */

package eu.monniot.kamon.cassandra

import java.lang.Thread.sleep

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.{Cluster, Session}
import kamon.Kamon
import kamon.context.Context
import kamon.testkit.{MetricInspection, Reconfigure, TestSpanReporter}
import kamon.trace.Span.TagValue
import kamon.trace.SpanCustomizer
import kamon.util.Registration
import org.scalatest._
import org.scalatest.concurrent.{JavaFutures, ScalaFutures}

import scala.collection.JavaConverters._
import scala.util.Try


// This class will start an embedded Cassandra server.
// As such, it is preferable to use it only for general test (which requires a query to C*)
// and use dedicated suite for unit tests.
class IntegrationSpec extends FlatSpec with Matchers with BeforeAndAfterAll with BeforeAndAfterEach
  with MetricInspection with Reconfigure with OptionValues with ScalaFutures with JavaFutures {

  it should "pickup SpanCustomizer from the current context and apply it to the new spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = '1'"

    Kamon.withContext(Context(SpanCustomizer.ContextKey, SpanCustomizer.forOperationName("select.the.key"))) {
      session.execute(select)
    }

    waitSomeTime()

    val span = reporter.nextSpan().value
    span.operationName shouldBe "select.the.key"
    span.tags("component") shouldBe TagValue.String("java-cassandra")
    span.tags("db.statement").toString should include(select)
  }

  it should "add errors to Spans when errors happen" in {
    val insert = "INSERT INTO ks.notatable VALUES ('1')"
    val select = "SELECT * FROM ks.mytable WHERE notakey = '1'"

    Try(session.execute(insert))
    waitSomeTime()

    {
      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement") shouldBe TagValue.String("INSERT INTO ks.notatable VALUES ('1')")

      span.tags("error") shouldBe TagValue.True
      span.tags("error.kind") shouldBe TagValue.String("com.datastax.driver.core.exceptions.SyntaxError")
      span.tags("error.object") shouldBe a[TagValue.String]
    }

    Try(session.execute(select))
    waitSomeTime()

    {
      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement") shouldBe TagValue.String("SELECT * FROM ks.mytable WHERE notakey = '1'")

      span.tags("error") shouldBe TagValue.True
      span.tags("error.kind") shouldBe TagValue.String("com.datastax.driver.core.exceptions.InvalidQueryException")
      span.tags("error.object") shouldBe a[TagValue.String]
    }
  }

  "#execute(String)" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = '1'"

    session.execute(select)

    waitSomeTime()

    val span = reporter.nextSpan().value
    span.operationName shouldBe "execute"
    span.tags("component") shouldBe TagValue.String("java-cassandra")
    span.tags("span.kind") shouldBe TagValue.String("client")
    span.tags("db.statement").toString should include(select)
  }

  "#execute(String, Object*)" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = ?"

    session.execute(select, "1")

    waitSomeTime()

    val span = reporter.nextSpan().value
    span.operationName shouldBe "execute"
    span.tags("component") shouldBe TagValue.String("java-cassandra")
    span.tags("span.kind") shouldBe TagValue.String("client")
    span.tags("db.statement").toString should include(select)
  }

  "#execute(String, Map[String, Object])" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = :i"

    session.execute(select, Map("i" -> "1".asInstanceOf[Object]).asJava)

    waitSomeTime()

    val span = reporter.nextSpan().value
    span.operationName shouldBe "execute"
    span.tags("component") shouldBe TagValue.String("java-cassandra")
    span.tags("span.kind") shouldBe TagValue.String("client")
    span.tags("db.statement").toString should include(select)
  }

  "#execute(Statement)" should "generate Spans" in {
    val stmt = QueryBuilder.select().from("ks", "mytable").where(QueryBuilder.eq("thekey", "1"))
    session.execute(stmt)

    waitSomeTime()

    val span = reporter.nextSpan().value
    span.operationName shouldBe "execute"
    span.tags("component") shouldBe TagValue.String("java-cassandra")
    span.tags("span.kind") shouldBe TagValue.String("client")
    span.tags("db.statement").toString should include("SELECT * FROM ks.mytable WHERE thekey=?;")
  }

  "#executeAsync(String)" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = '1'"

    val f = session.executeAsync(select)

    whenReady(f) { _ =>
      waitSomeTime()

      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement").toString should include(select)
    }
  }

  "#executeAsync(String, Object*)" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = ?"

    val f = session.executeAsync(select, "1")

    whenReady(f) { _ =>
      waitSomeTime()

      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement").toString should include(select)
    }
  }

  "#executeAsync(String, Map[String, Object])" should "generate Spans" in {
    val select = "SELECT * FROM ks.mytable where thekey = :i"

    val f = session.executeAsync(select, Map("i" -> "1".asInstanceOf[Object]).asJava)

    whenReady(f) { _ =>
      waitSomeTime()

      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement").toString should include(select)
    }
  }

  "#executeAsync(Statement)" should "generate Spans" in {
    val stmt = QueryBuilder.select().from("ks", "mytable").where(QueryBuilder.eq("thekey", "1"))

    val f = session.executeAsync(stmt)

    whenReady(f) { _ =>
      waitSomeTime()

      val span = reporter.nextSpan().value
      span.operationName shouldBe "execute"
      span.tags("component") shouldBe TagValue.String("java-cassandra")
      span.tags("span.kind") shouldBe TagValue.String("client")
      span.tags("db.statement").toString should include("SELECT * FROM ks.mytable WHERE thekey=?;")
    }
  }


  val reporter: TestSpanReporter = new TestSpanReporter
  var registration: Registration = Kamon.addReporter(reporter)

  val cluster: Cluster = Cluster.builder()
    .addContactPoint("127.0.0.1")
    .build()
  var session: Session = _

  // This account for two things:
  // - one is the Kamon tick to flush the trace (happens every milliseconds)
  // - second is because the underlying instrumentation is async and the callback
  //   may not be fired immediately, so we wait a bit for it to be.
  private def waitSomeTime(): Unit = sleep(10)

  override protected def beforeAll(): Unit = {
    session = cluster.connect()

    // Don't sample the query belows (setup)
    sampleNever()

    session.execute(
      """CREATE KEYSPACE IF NOT EXISTS ks
        | WITH replication = { 'class': 'SimpleStrategy', 'replication_factor': '2' }
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

    // But sample all query in the tests
    sampleAlways()
  }

  override protected def afterAll(): Unit = {
    session.close()
    cluster.close()
    registration.cancel()
  }

  override protected def afterEach(): Unit = reporter.clear()

}