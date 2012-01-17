name := "Akka Actor Workflow"

version := "0.1"

scalaVersion := "2.9.1"


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
   "se.scalablesolutions.akka" % "akka-actor" % "1.3-RC5"
   ,"se.scalablesolutions.akka" % "akka-camel" % "1.3-RC5"
   ,"se.scalablesolutions.akka" % "akka-remote" % "1.3-RC5"
   ,"org.apache.camel" % "camel-scala" % "2.8.2"
 )

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

 //parallelExecution in Test := false


