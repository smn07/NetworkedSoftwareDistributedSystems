# Evaluation lab - Contiki-NG

## Group number: 24

## Group members

- Marco Barbieri
- Lucrezia Sorrentino
- Simone Di Ienno

## Solution description

Within the client, we created a while loop that sends temperature readings every minute through a periodic etimer.
To check if the client is connecter or disconnected we use the NETSTACK_ROUTING.node_is_reachable() method and set the is_disconnected flag accordingly.
When we discover that the server is not reachable, we start to buffer the temperature readings into a circular buffer.
When the server becomes reachable, we check the previous value of is_disconnected flag to know if we need to send the average of the buffered temperatures. if so we compute the average value and send it.
For the purpose of recalculating new averages, the buffer is cleared once the values have been successfully transmitted.
Once we transmit the average, we also send the current temperature reading. This send happens almost after the send of the average, so we use an etimer to wait 5 seconds after the first send.
To deal with the type difference of the readings and the temperature, we modified the template so that we have float temperature values: in this way the server only needs to process float varaibles.