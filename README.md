# Kamon Cassandra <img align="right" src="https://rawgit.com/kamon-io/Kamon/master/kamon-logo.svg" height="150px" style="padding-left: 20px"/> 
[![Build Status](https://travis-ci.org/fmonniot/kamon-cassandra.svg?branch=master)](https://travis-ci.org/fmonniot/kamon-cassandra)
[ ![Download](https://api.bintray.com/packages/fmonniot/maven/kamon-cassandra/images/download.svg) ](https://bintray.com/fmonniot/maven/kamon-cassandra/_latestVersion)

--------------------


The **`kamon-cassandra`** module provides bytecode instrumentation that brings automatic traces and metrics to the [official Cassandra driver].

### Adding the Module

Supported releases and dependencies are shown below.

| kamon-cassandra  | status       | jdk  | scala      | driver |
|:----------------:|:------------:|:----:|:----------:|:------:|
| 1.0.1            | experimental | 1.8+ | 2.11, 2.12 | 3.4.0  |


To get started with SBT, simply add the following to your `build.sbt` file:

```scala
// The library is publish on JCenter, so add the following line if not already present
resolvers += Resolver.jcenterRepo
// Or if you like living on the edge:
// resolvers += Resolver.bintrayRepo("fmonniot", "snapshots")

libraryDependencies += "eu.monniot.kamon" %% "kamon-cassandra" % "1.0.1"
```

### Run

The `kamon-cassandra` module requires you to start your application using the AspectJ Weaver Agent.
You can achieve that quickly with the [sbt-aspectj-runner] plugin or take a look at the [documentation] for other options.

An example is available in the [playground] submodule. It also contains a quick guide on how to start.

### Enjoy!

That's it, you are now collecting metrics and tracing information from a Cassandra driver.


[official Cassandra driver]: https://github.com/datastax/java-driver
[sbt-aspectj-runner]: https://github.com/kamon-io/sbt-aspectj-runner
[documentation]: http://kamon.io/documentation/1.x/recipes/adding-the-aspectj-weaver/
[playground]: https://github.com/fmonniot/kamon-cassandra/tree/master/playground
