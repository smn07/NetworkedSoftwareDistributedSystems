package it.polimi.nsds.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PopularTopicConsumer {
    private static final String topic = "inputTopic";
    private static final String serverAddr = "localhost:9092";

    public static void main(String[] args) {
        String groupId = args[0];

        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // TODO: complete the implementation by adding code here
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

        KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        Map<String, Integer> messageCountMap = new HashMap<>();

        while(true){
            final ConsumerRecords<String, Integer> records = consumer.poll(Duration.of(5, ChronoUnit.MINUTES));

            for (final ConsumerRecord<String, Integer> record : records) {
                String key = record.key();

                Integer messageCount;

                if(!messageCountMap.containsKey(key))
                    messageCount = 1;
                else
                    messageCount = messageCountMap.get(key)+1;

                messageCountMap.put(key, messageCount);

                int max = Collections.max(messageCountMap.values());

                for (Map.Entry<String, Integer> entry : messageCountMap.entrySet()) {
                    if (entry.getValue() == max) {
                        System.out.println(
                                "\tmax Key: " + entry.getKey() +
                                "\tValue: " + entry.getValue()
                        );
                    }
                }
                System.out.println("----------------------------------\n");
            }
        }
    }
}
