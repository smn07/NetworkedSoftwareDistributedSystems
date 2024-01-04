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

#define MAX_READINGS 10
#define SEND_INTERVAL (60 * CLOCK_SECOND)
#define RECONNECT_INTERVAL (5 * CLOCK_SECOND)
#define INITIAL_INTERVAL (120 * CLOCK_SECOND)
#define FAKE_TEMPS 5

static struct etimer periodic_timer;
static struct etimer reconnect_timer;
static float temperature;
static float temperature_buffer[MAX_READINGS];
static unsigned next_reading = 0;
static int is_disconnected = 0;

/*---------------------------------------------------------------------------*/
PROCESS(udp_client_process, "UDP client");
AUTOSTART_PROCESSES(&udp_client_process);
/*---------------------------------------------------------------------------*/
static float
get_temperature()
{
  static float fake_temps [FAKE_TEMPS] = {30.0, 25.0, 20.0, 15.0, 10.0};
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
  // ...
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(udp_client_process, ev, data)
{
  PROCESS_BEGIN();

  /* Initialize UDP connection */
  simple_udp_register(&udp_conn, UDP_CLIENT_PORT, NULL,
                      UDP_SERVER_PORT, udp_rx_callback);

  etimer_set(&periodic_timer, INITIAL_INTERVAL);

  while(1){
    PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&periodic_timer));

    // generate the temperature reading
    temperature = get_temperature();

    if(NETSTACK_ROUTING.node_is_reachable() && NETSTACK_ROUTING.get_root_ipaddr(&dest_ipaddr)) {
      // if is_disconnected is true, it means that the node just reconnected and need to send the average
      if(is_disconnected == 1){
        // send the average of the buffered temperatures
        float average;
        float sum = 0;
        unsigned no = 0;
        int i;
        for (i=0; i<MAX_READINGS; i++) {
          if (temperature_buffer[i]!=0){
            sum = sum+temperature_buffer[i];
            no++;
          }
        }

        if(no != 0){
          average = sum/no;
          LOG_INFO("Sending buffered average %f \n", average);

          simple_udp_sendto(&udp_conn, &average, sizeof(average), &dest_ipaddr);

          // wait for the outgoing buffer to be freed
          etimer_set(&reconnect_timer, RECONNECT_INTERVAL);

          // clear the buffer
          for (i=0; i<next_reading; i++) {
            temperature_buffer[i] = 0;
          }
          next_reading = 0;

          PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&reconnect_timer));
        }
      }

      LOG_INFO("Sending temperature reading %f \n", temperature);
      simple_udp_sendto(&udp_conn, &temperature, sizeof(temperature), &dest_ipaddr);

      is_disconnected = 0;
    } else {
      is_disconnected = 1;
      LOG_INFO("Not reachable yet, buffering the temperatures\n");

      // buffer the temperature reading
      temperature_buffer[next_reading] = temperature;
      next_reading++;

      if (next_reading == MAX_READINGS){
        next_reading = 0;
      }
    }

    etimer_set(&periodic_timer, SEND_INTERVAL);
  }

  PROCESS_END();
}
/*---------------------------------------------------------------------------*/
