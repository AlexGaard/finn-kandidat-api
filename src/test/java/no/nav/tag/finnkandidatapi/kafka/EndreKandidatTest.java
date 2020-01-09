package no.nav.tag.finnkandidatapi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.tag.finnkandidatapi.kafka.harTilretteleggingsbehov.HarTilretteleggingsbehov;
import no.nav.tag.finnkandidatapi.kandidat.FysiskBehov;
import no.nav.tag.finnkandidatapi.kandidat.GrunnleggendeBehov;
import no.nav.tag.finnkandidatapi.kandidat.KandidatDto;
import no.nav.tag.finnkandidatapi.kandidat.KandidatRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static no.nav.tag.finnkandidatapi.TestData.enKandidatDto;
import static no.nav.tag.finnkandidatapi.kafka.KafkaTestUtil.readKafkaMessages;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "mock"})
@DirtiesContext
public class EndreKandidatTest {

    @Autowired
    private EnKafkaMockServer embeddedKafka;

    private Consumer<String, String> consumer;

    private TestRestTemplate restTemplate = new TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES);

    @Autowired
    private KandidatRepository repository;

    @LocalServerPort
    private int port;

    private String localBaseUrl() {
        return "http://localhost:" + port + "/finn-kandidat-api";
    }

    @Before
    public void setUp() {
        String loginUrl = localBaseUrl() + "/local/isso-login";
        restTemplate.getForObject(loginUrl, String.class);
    }

    @Test
    public void nårMottarHttpRequest_skalSendeKafkaMelding() throws JsonProcessingException, InterruptedException {
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
        final List<String> receivedMessages = readKafkaMessages(embeddedKafka, 2);
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
    }
}