name := "Akka Actor Workflow"

version := "0.3"

scalaVersion := "2.10.0-M7"


//Specs2
libraryDependencies ++= Seq(
 "org.specs2" % "specs2_2.10.0-M7" % "1.12.1.1",
   "org.slf4j" % "slf4j-log4j12" % "1.6.3" % "test" ,
   "junit" % "junit" % "4.7"
   )


resolvers += "Typesafe" at "https://oss.sonatype.org/content/repositories/releases/"

 resolvers ++= Seq("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                    "releases"  at "http://oss.sonatype.org/content/repositories/releases")
 
libraryDependencies ++= Seq("com.typesafe.akka" % "akka-actor_2.10.0-M7" % "2.1-M2",
                            "com.typesafe.akka" % "akka-camel_2.10.0-M7" % "2.1-M2"
                           )

 //parallelExecution in Test := false


