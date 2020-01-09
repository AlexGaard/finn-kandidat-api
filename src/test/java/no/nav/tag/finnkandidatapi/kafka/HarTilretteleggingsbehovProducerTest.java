package no.nav.tag.finnkandidatapi.kafka;

import no.nav.tag.finnkandidatapi.kafka.harTilretteleggingsbehov.HarTilretteleggingsbehov;
import no.nav.tag.finnkandidatapi.kafka.harTilretteleggingsbehov.HarTilretteleggingsbehovProducer;
import no.nav.tag.finnkandidatapi.kandidat.KandidatRepository;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TokenUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static no.nav.tag.finnkandidatapi.TestData.enAktørId;
import static no.nav.tag.finnkandidatapi.TestData.enVeileder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"local", "mock"})
@DirtiesContext
public class HarTilretteleggingsbehovProducerTest {

    @Autowired
    private EnKafkaMockServer embeddedKafka;

    @Autowired
    private HarTilretteleggingsbehovProducer harTilretteleggingsbehovProducer;

    @Autowired
    private KandidatRepository repository;

    @MockBean
    private TokenUtils tokenUtils;

    private Consumer<String, String> consumer;

    @Before
    public void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafka.getEmbeddedKafka());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        ConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        embeddedKafka.getEmbeddedKafka().consumeFromAnEmbeddedTopic(consumer, "aapen-tag-kandidatEndret-v1-default");

        when(tokenUtils.hentInnloggetVeileder()).thenReturn(enVeileder());
    }

    @Test
    public void kandidatOppdatert__skal_sende_melding_på_kafka_topic() throws JSONException {
        HarTilretteleggingsbehov harTilretteleggingsbehov = new HarTilretteleggingsbehov(enAktørId(), true);
        harTilretteleggingsbehovProducer.sendKafkamelding(harTilretteleggingsbehov);

        ConsumerRecord<String, String> melding = KafkaTestUtils.getSingleRecord(consumer, "aapen-tag-kandidatEndret-v1-default");

        JSONObject json = new JSONObject(melding.value());
        assertThat(melding.key()).isEqualTo(harTilretteleggingsbehov.getAktoerId());
        assertThat(json.get("aktoerId")).isEqualTo(harTilretteleggingsbehov.getAktoerId());
        assertThat(json.get("harTilretteleggingsbehov")).isEqualTo(harTilretteleggingsbehov.isHarTilretteleggingsbehov());
    }

    @After
    public void tearDown() {
        repository.slettAlleKandidater();
    }
}
