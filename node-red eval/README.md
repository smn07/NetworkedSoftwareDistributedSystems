# Evaluation lab - Node-RED

## Group number: 24

## Group members

- Marco Barbieri
- Lucrezia Sorrentino
- Simone Di Ienno

## Description of message flows
first of all we use a node to set the context of the forecast and wind speed to null (on both cities and both days).
once we receive a message we identify which type it is through a switch:
-if the message is a forecast one, it goes in the forecast branch;
-if the message is a expected wind speed one, it goes to the wind speed branch;
-otherwise it gives an "unknown query" exception.

either way, it checks that the city is correct (Rome or Milan) and queries the OpenWeatherMap service for the 5 days forecast.
once we have the forecast we extract the correct information using the properties given in the slides.
in particular to select tomorrow forecast we use msg.payload[7] and to select two days we use msg.payload[15], this is done because these values are the closest to 24h and 48h.

once we receive the response from the OpenweatherMap service, we increment the messageCounter. this is done because we want to increment the counter only when we receive corerct queries.
every minute we store to a file the messageCount and then, once the write has finished we reset the counter so that we can start the count for the next minute.

correct queries:
-what's tomorrow forecast in <city>?
-what's the forecast in <city> in two days?
-what's the expected wind speed tomorrow in <city>?
-What’s the expected wind speed in two days in <city>?

## Extensions
we used the same palette as the ones used in the labs

## Bot URL
https://t.me/marcheobot

bot token: 6864324866:AAHjvK2DwcxnCxCRnJVplVjzeT0gskvm9fc
