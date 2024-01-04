# Evaluation lab - Apache Kafka

## Group number: 24

## Group members

- Marco Barbieri
- Lucrezia Sorrentino
- Simone Di Ienno

## Exercise 1

- Number of partitions allowed for inputTopic (1, n)
// where n is the number of keys (1000 in our case)
// having a number of partitions greater than the number of keys is useless because some partitions will be empty because they are not mapped to any key
- Number of consumers allowed (1, p)
// where p is the number of partitions
// having a number of consumers (in the same group) greater than the number of partitions is useless because some consumers will remain in idle (because some consumers won't have a partition assigned)
    - Consumer 1: atMostOnceGroup
    - Consumer 2: atMostOnceGroup
    - ...
    - Consumer n: atMostOnceGroup

## Exercise 2

- Number of partitions allowed for inputTopic (1, n)
- Number of consumers allowed (1, 1)
// n is defined as before
// we need only one consumer in a group because otherwise, if we have multiple consumers in a single group, then they will split the messages between each other and they won't calculate the max number (they will have different keys and they can calculate the max number only based on their keys). if we want multiple consumers, we can use multiple groups and put each consumer in a different group.
    - Consumer 1: popularTopicGroup1
    - Consumer 2: popularTopicGroup2
    - ...
    - Consumer n: popularTopicGroupn