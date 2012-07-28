name := "Akka Actor Workflow"

version := "0.2"

scalaVersion := "2.9.1"


//Specs2
libraryDependencies ++= Seq(
   "org.specs2" %% "specs2" % "1.6.1",
   "org.specs2" %% "specs2-scalaz-core" % "6.0.1" % "test",
    "org.slf4j" % "slf4j-log4j12" % "1.6.3" % "test" ,
   "junit" % "junit" % "4.7"
   )

scalaVersion := "2.9.1"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.2"

 //parallelExecution in Test := false


