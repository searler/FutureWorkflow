# Introduction

This project illustrates how the Akka Future implementation can be used to create the same workflows as covered in the
ScalaWorkflow project, with greatly reduced simplicity. 

Note the lack of a src directory. The implementation is sufficiently simple that all the required code can be sensibly placed in the test category. 

## Discussion
[Implementation](http://cognitiveentity.wordpress.com/2012/01/16/choreography-using-akka-futures-1/)

[Unit testing](http://cognitiveentity.wordpress.com/2012/01/17/unit-testing-akka-future-based-choreography/)

# Structure

## Data.scala
Defines case classes for the problem domain

## ValueMaps.scala
Defines test data

## Flow.scala
Provides a common trait to define the interaction of a Flow with the environment.ZZ

## Flows.scala
Defines a collection of workflows.

## FlowsTest.scala
Exercises the Flow instances using the simplest possible implementation.

## AkkaFlowsTest.scala
Exercises the Flow instances using simple Akka actors.

## CamelTest.scala
Exercises the Flow instances using Camel to provide a context that is closer to a real world implementation.

