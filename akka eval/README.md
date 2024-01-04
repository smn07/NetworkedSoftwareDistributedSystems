# Evaluation lab - Akka

## Group number: 24

## Group members

- Marco Barbieri
- Lucrezia Sorrentino
- Simone Di Ienno

## Description of message flows
We start by creating the sensors and the dispatcher. Then we ask the dispatcher to create the processors.
We assumed that the processors are created at the start and the set of processors doesn't change at run time.
With the SetDispatcherMsg we give the dispatcher reference to the sensors.
With the SetProcessorMsg we give the processors references to the dispatcher.
We then ask the temperature sensors to generate some readings with the GenerateMsg message, the sensors generate TemperatureMsg messages and send them to the dispatcher.
Initially the dispatcher is set to the load balancer mode, so when it receives the temperature messages, it looks to the sensor-processor map and forwards the temperature messages to the correct processor.
The sensor-processor map is created dynamically as the dispatcher receives new messages from the sensors, it is created so that the processors manage more or less the same number of sensors.
When the proceesor receives a temperature message, it updates its average temperature and prints it.
We then send a DispatchLogicMsg to inform the dispatcher to change mode into round robin and then we ask the sensors to generate some more data.
To simulate the fault tolerance, we create a faulty sensor that generates negative temperatures.
We ask every sensor to geenrate some data (including the faulty one) and verify that the fault tolerance is handled correctly.
