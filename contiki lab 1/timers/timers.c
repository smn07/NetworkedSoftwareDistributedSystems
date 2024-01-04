#include "contiki.h"

#include <stdio.h>

// global variables
int previous_ctimer = 0;
int previous_rtimer = 0;

/*---------------------------------------------------------------------------*/
static void ctimer_callback(void *data);
#define CTIMER_INTERVAL 2 * CLOCK_SECOND
static struct ctimer print_ctimer;
/*---------------------------------------------------------------------------*/
static void rtimer_callback(struct rtimer *t, void *data);
#define RTIMER_HARD_INTERVAL 2 * RTIMER_SECOND
static struct rtimer print_rtimer;
/*---------------------------------------------------------------------------*/
PROCESS(hello_world_ctimer, "Hello world process");
AUTOSTART_PROCESSES(&hello_world_ctimer);
/*---------------------------------------------------------------------------*/
static void ctimer_callback(void *data){
  int delta_times = previous_rtimer - previous_ctimer;
  previous_ctimer = RTIMER_NOW();
  printf("%s", (char *)data);
  
  /* Reschedule the ctimer. */
  ctimer_set(&print_ctimer, CTIMER_INTERVAL + delta_times, ctimer_callback, "Hello world CT\n");
}
/*---------------------------------------------------------------------------*/
static void rtimer_callback(struct rtimer *t, void *data){
  previous_rtimer = RTIMER_NOW();
  printf("%s", (char *)data);
  
  /* Reschedule the rtimer. */
  rtimer_set(&print_rtimer, RTIMER_NOW()+RTIMER_HARD_INTERVAL, 0, rtimer_callback, "Hello world RT\n");
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(hello_world_ctimer, ev, data)
{
  PROCESS_BEGIN();


  rtimer_init();

  /* Schedule the rtimer: absolute time! */
  rtimer_set(&print_rtimer, RTIMER_NOW()+RTIMER_HARD_INTERVAL, 0, rtimer_callback, "Hello world RT\n");

  /* Schedule the ctimer. */
  ctimer_set(&print_ctimer, CTIMER_INTERVAL, ctimer_callback, "Hello world CT\n");

  /* Only useful for platform native. */
  PROCESS_WAIT_EVENT();

  PROCESS_END();
}
