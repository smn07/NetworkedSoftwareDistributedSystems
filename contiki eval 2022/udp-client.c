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

#define SEND_INTERVAL (60 * CLOCK_SECOND)
#define FAKE_TEMPS 5

static struct simple_udp_connection udp_conn;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/
static unsigned
get_temperature()
{
  static unsigned fake_temps [FAKE_TEMPS] = {30, 25, 20, 15, 10};
  return fake_temps[random_rand() % FAKE_TEMPS];
  
}
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
  /* print the received message */
  LOG_INFO("Received response '%.*s'", datalen, (char *) data);
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data)
{
  PROCESS_BEGIN();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL,
                      UDP_SERVER_PORT, udp_rx_callback);

  while(1) {
    static uip_ipaddr_t dest_ipaddr;
    static struct etimer periodic_timer;
    static unsigned temperature;
    static char str[32];

    while(1){
      if(NETSTACK_ROUTING.node_is_reachable() && NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {    
        /* Compute temperature */
        temperature = get_temperature();

        /* Prepare message */
        snprintf(str, sizeof(str), "%u", temperature);

        /* Send message */
        simple_udp_sendto(&udp_conn, str, strlen(str), &dest_ipaddr);

        /* Wait some time */
        etimer_set(&periodic_timer, SEND_INTERVAL);

        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));
      } else {
        LOG_INFO("Not reachable yet\n");
      }
    }
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
