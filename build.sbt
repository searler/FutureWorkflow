name := "Akka Actor Workflow"

version := "0.1"

scalaVersion := "2.9.1"

//sourceDirectory := file("/home/richard/work/FutureWorkflow/sbt/src")

//Specs2
libraryDependencies ++= Seq(
   "org.specs2" %% "specs2" % "1.6.1",
   "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
    "org.slf4j" % "slf4j-log4j12" % "1.6.3" % "test" ,
   "junit" % "junit" % "4.7"
   )

resolvers ++= Seq( "releases"  at "http://scala-tools.org/repo-releases")

//AKKA

libraryDependencies ++= Seq(
   "se.scalablesolutions.akka" % "akka-actor" % "1.2"
   ,"se.scalablesolutions.akka" % "akka-camel" % "1.2"
   ,"se.scalablesolutions.akka" % "akka-remote" % "1.2"
   ,"org.apache.camel" % "camel-scala" % "2.6.0"
 )

resolvers ++= Seq("Akka Repository" at "http://akka.io/repository")


 //parallelExecution in Test := false

