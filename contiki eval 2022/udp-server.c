#include "contiki.h"
#include "net/routing/routing.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"

#include "sys/log.h"
#include "random.h"

#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define UDP_CLIENT_PORT	8765
#define UDP_SERVER_PORT	5678
#define MAX_RECEIVERS 10
#define MAX_READINGS 10

#define ALERT_THRESHOLD 19

#define BROADCAST_INTERVAL CLOCK_SECOND

static struct simple_udp_connection udp_conn;

static unsigned readings[MAX_READINGS];
static unsigned next_reading=0;

static uip_ipaddr_t receivers[MAX_RECEIVERS];
static unsigned num_receivers = 0;

PROCESS(udp_server_process, "UDP server");
AUTOSTART_PROCESSES(&udp_server_process);
/*---------------------------------------------------------------------------*/
static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  /* add receiver if new */
  if(num_receivers < MAX_RECEIVERS){
    unsigned i;
    for(i = 0; i < num_receivers; i++){
      if(uip_ipaddr_cmp(&receivers[i], receiver_addr)){
        break;
      }
    }

    if(i == num_receivers){
      uip_ipaddr_copy(&receivers[num_receivers++], receiver_addr);
    }
  }


  unsigned reading = *data;

  /* Add reading */
  readings[next_reading++] = reading;
  if (next_reading == MAX_READINGS) {
    next_reading = 0;
  }

  /* Compute average */
  float average;
  unsigned sum = 0;
  unsigned no = 0;
  unsigned k;
  for (k = 0; k < MAX_READINGS; k++) {
    if (readings[k]!=0){
      sum = sum+readings[k];
      no++;
    }
  }
  average = ((float)sum)/no;
  LOG_INFO("Current average is %f \n",average);

  if(average > ALERT_THRESHOLD){
    //static struct etimer broadcast_timer;

    LOG_INFO("ALERT! Temperature is above threshold \n");

    unsigned j;
    for(j = 0; j < num_receivers; j++){
      //PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&broadcast_timer));
      LOG_INFO("Sending alert to receiver %d \n", j);
      simple_udp_sendto(&udp_conn, "high temperature alert", 22, &receivers[j]);
      //etimer_set(&broadcast_timer, BROADCAST_INTERVAL);
    }
  }
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_server_process, ev, data)
{
  PROCESS_BEGIN();

  static uint8_t i=0;
  /* Initialize temperature buffer */
  for (i=0; i<next_reading; i++) {
    readings[i] = 0;
  }  

  /* Initialize DAG root */
  NETSTACK_ROUTING.root_start();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_SERVER_PORT, NULL,
                      UDP_CLIENT_PORT, udp_rx_callback);
  
  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
