package no.nav.tag.finnkandidatapi.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

class KafkaTestUtil {

    static BlockingQueue<ConsumerRecord<String, String>> receiveRecords(final EmbeddedKafkaBroker kafka, final String topicName) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "false", kafka);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        ContainerProperties containerProperties = new ContainerProperties(topicName);
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(cf, containerProperties);
        BlockingQueue<ConsumerRecord<String, String>> receivedRecords = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, String>) record -> {
            System.out.println("AAA " + record);
            receivedRecords.add(record);
        });
//        container.setBeanName(this.getClass().getSimpleName());
        container.start();
        ContainerTestUtils.waitForAssignment(container, kafka.getPartitionsPerTopic());
        return receivedRecords;
    }

    static List<String> recordValues(BlockingQueue<ConsumerRecord<String, String>> records, int numberOfMessages) throws InterruptedException {
        if (numberOfMessages < 1) return List.of();
        List<String> msgs = new ArrayList<>();
        long startTimeMillies = System.currentTimeMillis();
        Optional.ofNullable(records.poll(10, SECONDS)).ifPresent(r -> msgs.add(r.value()));
        for (int i = 1; i < numberOfMessages; i++) {
            Optional.ofNullable(records.poll(1, SECONDS)).ifPresent(r -> msgs.add(r.value()));
        }
        long waitTimeSeconds = Duration.ofMillis(System.currentTimeMillis() - startTimeMillies).toSeconds();
        if (msgs.size() < numberOfMessages) {
            String msg = "Only " + msgs.size() + " of expected " + numberOfMessages + " messages received within the given duration of " + waitTimeSeconds + " seconds..";
            throw new AssertionError(msg);
        }
        return Collections.unmodifiableList(msgs);
    }

    static List<String> readKafkaMessages(final EnKafkaMockServer embeddedKafka, final int minExpectedMsgs) {
        return readKafkaMessages(embeddedKafka, minExpectedMsgs, Duration.ofSeconds(10));
    }

    static List<String> readKafkaMessages(final EnKafkaMockServer embeddedKafka, final int minExpectedMsgs, final Duration maxWaitDuration) {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka.getEmbeddedKafka());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OffsetResetStrategy.EARLIEST.toString().toLowerCase());

        final List<String> receivedMessages = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(minExpectedMsgs);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            KafkaConsumer<Integer, String> kafkaConsumer = new KafkaConsumer<>(consumerProps);
            kafkaConsumer.subscribe(Collections.singletonList(embeddedKafka.topicName));
            try {
                while (true) {
                    ConsumerRecords<Integer, String> records = kafkaConsumer.poll(Duration.ofMillis(100L));
                    records.iterator().forEachRemaining(record -> {
                        receivedMessages.add(record.value());
                        latch.countDown();
                    });
                }
            } finally {
                kafkaConsumer.close();
            }
        });

        final long maxWaitSeconds = maxWaitDuration.toSeconds() <= 0 ? 1 : maxWaitDuration.toSeconds();
        final boolean waitTimeIsExhausted;
        try {
            waitTimeIsExhausted = !latch.await(maxWaitSeconds, SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (waitTimeIsExhausted) {
            String msg = "Only " + receivedMessages.size() + " of expected " + minExpectedMsgs + " messages received within the given duration of " + maxWaitSeconds + " seconds.";
            throw new AssertionError(msg);
        } else {
            return Collections.unmodifiableList(receivedMessages);
        }
    }
}
