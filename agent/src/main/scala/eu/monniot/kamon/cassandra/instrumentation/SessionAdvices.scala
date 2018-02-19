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

package eu.monniot.kamon.cassandra.instrumentation

import com.datastax.driver.core._
import com.google.common.util.concurrent.{FutureCallback, Futures}
import kamon.Kamon
import kamon.trace.{Span, SpanCustomizer}
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.{Around, Aspect}


@Aspect
class SessionAdvices {

  import SessionAdvices._

  @Around("execution(* executeAsync(com.datastax.driver.core.Statement)) && args(query)")
  def executeAsync(pjp: ProceedingJoinPoint, query: Statement): Any =
    withSpan(buildSpan(getQuery(query))) {
      pjp.proceed().asInstanceOf[ResultSetFuture]
    }

}

object SessionAdvices {

  private[instrumentation] def withSpan(span: Span)(f: => ResultSetFuture): ResultSetFuture = {
    try {
      val rsf = Kamon.withContextKey(Span.ContextKey, span)(f)

      Futures.addCallback(rsf, new FutureCallback[ResultSet] {
        override def onFailure(t: Throwable): Unit = {

          span.addError("", t)
          span.tag("error.kind", t.getClass.getName)
          span.finish()
        }

        override def onSuccess(result: ResultSet): Unit = {
          import java.net.Inet4Address
          import java.nio.ByteBuffer
          val host = result.getExecutionInfo.getQueriedHost

          span.tag("peer.port", host.getSocketAddress.getPort)
          span.tag("peer.hostname", host.getAddress.getCanonicalHostName)

          // This follow what is being done in the open tracing cassandra driver
          // TODO See if why that is
          host.getSocketAddress.getAddress match {
            case ipv4: Inet4Address =>
              span.tag("peer.ipv4", ByteBuffer.wrap(ipv4.getAddress).getInt)
            case ip =>
              span.tag("peer.ipv6", ip.getHostAddress)
          }

          span.finish()
        }
      })

      rsf
    } catch {
      case t: Throwable =>
        span.addError(t.getMessage, t)
        span.finish()
        throw t
    }
  }

  private[instrumentation] def getQuery(statement: Statement): String = {
    statement match {
      case st: BoundStatement =>
        st.preparedStatement.getQueryString
      case st: RegularStatement =>
        st.getQueryString
      case _ =>
        ""
    }
  }

  private[instrumentation] def buildSpan(query: String) =
    Kamon.currentContext().get(SpanCustomizer.ContextKey).customize {
      Kamon.buildSpan("execute")
        .withTag("component", "java-cassandra")
        .withTag("span.kind", "client")
        .withTag("db.statement", query)
    }.start()

}