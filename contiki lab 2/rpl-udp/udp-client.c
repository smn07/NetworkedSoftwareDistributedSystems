#include "contiki.h"
#include "net/routing/routing.h"
#include "random.h"
#include "net/netstack.h"
#include "net/ipv6/simple-udp.h"

#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_INFO

#define WITH_SERVER_REPLY  1
#define UDP_CLIENT_PORT	8765
#define UDP_SERVER_PORT	5678

static struct simple_udp_connection udp_conn;
static uip_ipaddr_t dest_ipaddr;

#define START_INTERVAL		(15 * CLOCK_SECOND)
#define SEND_INTERVAL		  (60 * CLOCK_SECOND)
#define TIMEOUT_TIME      (30 * CLOCK_SECOND)

#define MAX_ITERATIONS 10

static struct simple_udp_connection udp_conn;

static struct ctimer timer;
static struct etimer periodic_timer;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/
static void timeout_expired(){
  // restart the count to zero and send it to the server
  unsigned count = 0;
  LOG_INFO("Sending request %u to ", count);
  LOG_INFO_6ADDR(&dest_ipaddr);
  LOG_INFO_("\n");
  simple_udp_sendto(&udp_conn, &count, sizeof(count), &dest_ipaddr);
  ctimer_restart(&timer);
}

static void
udp_rx_callback(struct simple_udp_connection *c,
         const uip_ipaddr_t *sender_addr,
         uint16_t sender_port,
         const uip_ipaddr_t *receiver_addr,
         uint16_t receiver_port,
         const uint8_t *data,
         uint16_t datalen)
{
  unsigned count = *(unsigned *)data;
  LOG_INFO("Received response %u from ", count);
  LOG_INFO_6ADDR(&dest_ipaddr);
  LOG_INFO_("\n");

  if (count == MAX_ITERATIONS) {
    LOG_INFO("Stopping client\n");
    etimer_restart(periodic_timer);
    return;
  }

  LOG_INFO("Sending request %u to ", count);
  LOG_INFO_6ADDR(&dest_ipaddr);
  LOG_INFO_("\n");
  simple_udp_sendto(&udp_conn, &count, sizeof(count), &dest_ipaddr);

  ctimer_restart(&timer);
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data)
{
  static unsigned count = 0;

  PROCESS_BEGIN();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL,
                      UDP_SERVER_PORT, udp_rx_callback);

  etimer_set(&periodic_timer, random_rand() % SEND_INTERVAL);
  PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

  while(1){
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
    if(NETSTACK_ROUTING.node_is_reachable() && NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {
      /* Send to DAG root */
      LOG_INFO("Sending request %u to ", count);
      LOG_INFO_6ADDR(&dest_ipaddr);
      LOG_INFO_("\n");
      simple_udp_sendto(&udp_conn, &count, sizeof(count), &dest_ipaddr);

      ctimer_set(&timer, TIMEOUT_TIME, timeout_expired, NULL);
      etimer_stop(&periodic_timer);
      continue;
    } else {
      LOG_INFO("Not reachable yet\n");
    }

    /* Add some jitter */
    etimer_set(&periodic_timer, SEND_INTERVAL - CLOCK_SECOND + (random_rand() % (2 * CLOCK_SECOND)));
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
