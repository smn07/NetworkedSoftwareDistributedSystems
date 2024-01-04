#include "contiki.h"

#include <stdio.h>
#include <stdlib.h>

#define MAX_QUEUE_SIZE 10
#define MAX_PRODUCER_WAIT_SECS 3
#define MAX_CONSUMER_WAIT_SECS 5


// global variables
static process_event_t not_full_event;
static process_event_t not_empty_event;
int *queue;
int queue_size = 0;


// queue functions

int enqueue(int value) {
  if (queue_size == MAX_QUEUE_SIZE) {
    return -1;
  }

  queue[queue_size] = value;
  queue_size++;

  return queue_size;
}

int dequeue() {
  if (queue_size == 0) {
    return -1;
  }

  int value = queue[0];
  for (int i = 0; i < queue_size; i++) {
    queue[i] = queue[i + 1];
  }
  queue_size--;

  return value;
}

/*---------------------------------------------------------------------------*/
PROCESS(producer, "producer");
PROCESS(consumer, "consumer");
AUTOSTART_PROCESSES(&producer, &consumer);
/*---------------------------------------------------------------------------*/

// protothread used to push elements in the queue
PROCESS_THREAD(producer, ev, data)
{
  static struct etimer producer_timer;
  int retValue;

  PROCESS_BEGIN();

  queue = (int *)calloc(sizeof(int) * MAX_QUEUE_SIZE, sizeof(int));

  /* Setup a periodic timer that expires after a rand amount of seconds. */
  etimer_set(&producer_timer, CLOCK_SECOND * (rand() % MAX_PRODUCER_WAIT_SECS + 1));

  while(1) {
    int value = rand() % 100;
    fflush(stdout);
    retValue = enqueue(value);

    if (retValue == -1) {
      printf("Queue is full\n");
      fflush(stdout);

      PROCESS_WAIT_EVENT_UNTIL(ev == not_full_event);
    }else{
      printf("Pushing %d in the queue\n", value);
      printf("queue size: %d\n", queue_size);
      fflush(stdout);

      if(queue_size == 1){
        // send an event to signal that the queue is not empty
        not_empty_event = process_alloc_event();
        process_post(&consumer, not_empty_event, NULL);
      }

      PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&producer_timer));

      etimer_set(&producer_timer, CLOCK_SECOND * (rand() % MAX_PRODUCER_WAIT_SECS + 1));
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/


// protothread used to pull elements from the queue
PROCESS_THREAD(consumer, ev, data)
{
  static struct etimer consumer_timer;
  int retValue;

  PROCESS_BEGIN();

  etimer_set(&consumer_timer, CLOCK_SECOND * (rand() % MAX_CONSUMER_WAIT_SECS + 1));

  while(1) {
    fflush(stdout);
    retValue = dequeue();

    if (retValue == -1) {
      printf("Queue is empty\n");
      fflush(stdout);

      PROCESS_WAIT_EVENT_UNTIL(ev == not_empty_event);
    }else{
      printf("pulling %d from the queue\n", retValue);
      printf("queue size: %d\n", queue_size);
      fflush(stdout);
      
      if(queue_size == MAX_QUEUE_SIZE -1){
        // send an event to signal that the queue is not full
        not_full_event = process_alloc_event();
        process_post(&producer, not_full_event, NULL);
      }

      PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&consumer_timer));

      etimer_set(&consumer_timer, CLOCK_SECOND * (rand() % MAX_CONSUMER_WAIT_SECS + 1));
    }
  }

  PROCESS_END();
}
