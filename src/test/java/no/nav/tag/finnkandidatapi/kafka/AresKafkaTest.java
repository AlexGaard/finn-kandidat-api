package no.nav.tag.finnkandidatapi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.tag.finnkandidatapi.kafka.harTilretteleggingsbehov.HarTilretteleggingsbehov;
import no.nav.tag.finnkandidatapi.kandidat.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static no.nav.tag.finnkandidatapi.TestData.enKandidatDto;
import static no.nav.tag.finnkandidatapi.kafka.KafkaTestUtil.*;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "mock"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(partitions = 1,
        topics = {AresKafkaTest.topicPropertyPlaceholder_oppfolgingAvsluttet,
                AresKafkaTest.topicPropertyPlaceholder_oppfolgingEndret,
                AresKafkaTest.topicPropertyPlaceholder_kandidatEndret},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AresKafkaTest {
    static final String topicPropertyPlaceholder_oppfolgingAvsluttet = "${oppfolging-avsluttet.topic}";
    static final String topicPropertyPlaceholder_oppfolgingEndret = "${oppfolging-endret.topic}";
    static final String topicPropertyPlaceholder_kandidatEndret = "${kandidat-endret.topic}";

    @Autowired
    EmbeddedKafkaBroker kafka;

    @Value(AresKafkaTest.topicPropertyPlaceholder_kandidatEndret)
    String topic;

    TestRestTemplate restTemplate = new TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES);

    @Autowired
    KandidatRepository repository;

    @LocalServerPort
    int port;

    String localBaseUrl() {
        return "http://localhost:" + port + "/finn-kandidat-api";
    }

    BlockingQueue<ConsumerRecord<String, String>> receivedRecords; // TODO Are: Bedre navn. Future records?


    @Before
    public void setUp() {
        String loginUrl = localBaseUrl() + "/local/isso-login";
        restTemplate.getForObject(loginUrl, String.class);

        if (receivedRecords == null) {
            receivedRecords = receiveRecords(kafka, topic);
        }
    }

    @Test
    public void opprett() throws InterruptedException, JsonProcessingException {
        // Given
        URI uri = URI.create(localBaseUrl() + "/kandidater");
        KandidatDto dto = enKandidatDto();
        dto.setAktørId("1856024171652");

        // When
        restTemplate.postForEntity(uri, dto, String.class);

        final List<String> receivedMessages = recordValues(receivedRecords, 1);
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.size()).isEqualTo(List.of("opprett").size());
        HarTilretteleggingsbehov actualTilretteleggingsbehov = new ObjectMapper().readValue(receivedMessages.get(0), HarTilretteleggingsbehov.class);
        List<String> actualBehov = actualTilretteleggingsbehov.getBehov();
        final Set<String> expectedBehov = Set.of(
                ArbeidsmiljøBehov.behovskategori,
                FysiskBehov.behovskategori,
                GrunnleggendeBehov.behovskategori
        );
        assertThat(actualTilretteleggingsbehov.getAktoerId()).isEqualTo(dto.getAktørId());
        assertThat(actualTilretteleggingsbehov.isHarTilretteleggingsbehov()).isTrue();
        assertThat(actualBehov).containsAll(expectedBehov);
        assertThat(actualBehov).hasSameSizeAs(expectedBehov);

        //        ConsumerRecord<String, String> consumerRecord = receivedMessages.poll(10, TimeUnit.SECONDS);
//        System.out.println("BBB " + consumerRecord);
    }


    @Test
    public void endre() throws JsonProcessingException, InterruptedException {
        // Given
        URI uri = URI.create(localBaseUrl() + "/kandidater");
        KandidatDto dto = enKandidatDto();
        dto.setAktørId("1856024171652");
        restTemplate.postForEntity(uri, dto, String.class); // Opprett kandidat

        // When
        assertThat(dto.getArbeidsmiljøBehov()).isNotEmpty();
        dto.setArbeidsmiljøBehov(Set.of());
        restTemplate.put(uri, dto);

        // Then
        final List<String> receivedMessages = recordValues(receivedRecords, 2);
        assertThat(receivedMessages).isNotEmpty();
        assertThat(receivedMessages.size()).isEqualTo(List.of("opprett", "endre").size());
        HarTilretteleggingsbehov actualTilretteleggingsbehov = new ObjectMapper().readValue(receivedMessages.get(1), HarTilretteleggingsbehov.class);
        List<String> actualBehov = actualTilretteleggingsbehov.getBehov();
        final Set<String> expectedBehov = Set.of(
                FysiskBehov.behovskategori,
                GrunnleggendeBehov.behovskategori
        );
        assertThat(actualTilretteleggingsbehov.getAktoerId()).isEqualTo(dto.getAktørId());
        assertThat(actualTilretteleggingsbehov.isHarTilretteleggingsbehov()).isTrue();
        assertThat(actualBehov).containsAll(expectedBehov);
        assertThat(actualBehov).hasSameSizeAs(expectedBehov);
    }



    @After
    public void tearDown() {
        repository.slettAlleKandidater();
    } // TODO Are: Unødvendig pga @DirtiesContext?
}
