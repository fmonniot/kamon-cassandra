# Kamon Cassandra playground <img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/>

--------------------

## Metrics and Tracing for cassandra with Akka-HTTP in 3 steps

Here is a quick guide on how to integrates Kamon-Cassandra into your project.
It also comes with a [`docker-compose.yml`][1] file if you want to try it out on your computer. 


### Step 1: Add the Kamon Libraries
```scala
libraryDependencies ++= Seq(
  "io.kamon" %% "kamon-core" % "1.0.0",
  "io.kamon" %% "kamon-system-metrics" % "1.0.0",
  "io.kamon" %% "kamon-prometheus" % "1.0.0",
  "io.kamon" %% "kamon-zipkin" % "1.0.0",
  "io.kamon" %% "kamon-jaeger" % "1.0.0",
  "eu.monniot.kamon" %% "kamon-cassandra" % "1.0.0"
)
```

### Step 2: Setting up the AspectJ Weaver agent

This project is designed to be packaged with the [`sbt-native-packager`][2] plugin, so we will be adding the 
[`sbt-aspectjweaver`][3] plugin to the build.

```scala
addSbtPlugin("com.gilt.sbt" % "sbt-aspectjweaver" % "0.1.0")

enablePlugins(JavaAgent)

javaAgents += "io.kamon" % "kamon-agent" % "0.0.8-experimental" % "runtime"
```

If you plan to use it from SBT itself, then see the [Official Kamon documentation][4].

### Step 3: Start Reporting your Data

The last step in the process: start reporting your data! You can register as many reporters as you want by using the
`Kamon.addReporter(...)` function:

```scala
Kamon.addReporter(new PrometheusReporter())
Kamon.addReporter(new ZipkinReporter())
```

Now you can simply `sbt docker:publishLocal` the application and using `docker-compose up` you can get access to the application.
 
The Prometheus metrics are exposed on <http://localhost:9095/> and the message traces are sent to Zipkin!
Refer to the [`application.conf`][5] file to know how to configure prometheus and/or Zipkin.


#### Metrics


Once you configure this target in Prometheus you are ready to run some queries like this:

<img class="img-fluid" src="/playground/img/akkahttpmetrics.png">

Now you can go ahead, make your custom metrics and create your own dashboards!

#### Traces

Assuming that you have a Zipkin instance running locally with the default ports (via the compose file for example),
you can go to <http://localhost:9411> and start investigating traces for this application.
Once you find a trace you are interested in you will see something like this:

<img class="img-fluid" src="/playground/img/traces.png">

Clicking on a span will bring up a details view where you can see all tags for the selected span:

<img class="img-fluid" src="/playground/img/details.png">


### Enjoy!

That's it, you are now collecting metrics and tracing information from a [cassandra][6] database.


[1]: https://github.com/fmonniot/kamon-cassandra/tree/master/playground/docker-compose.yml
[2]: https://github.com/sbt/sbt-native-packager
[3]: https://github.com/gilt/sbt-aspectjweaver
[4]: http://kamon.io/documentation/1.x/recipes/adding-the-aspectj-weaver/
[5]: https://github.com/fmonniot/kamon-cassandra/tree/master/playground/src/main/resources/application.conf
[6]: https://cassandra.apache.org/
