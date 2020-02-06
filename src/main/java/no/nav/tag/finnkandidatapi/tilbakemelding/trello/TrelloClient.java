package no.nav.tag.finnkandidatapi.tilbakemelding.trello;

import lombok.extern.slf4j.Slf4j;
import no.nav.tag.finnkandidatapi.tilbakemelding.Tilbakemelding;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class TrelloClient {

    private static final String CARDS_URL = "https://api.trello.com/1/cards";
    private static final String LIST_ID = "5e342434b157b81de263d5c3";

    @Value("${TRELLO_KEY}")
    private String key;

    @Value("${TRELLO_TOKEN}")
    private String token;

    @Value("${nais.cluster-name}")
    private String cluster;

    private final RestTemplate restTemplate;

    public TrelloClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void opprettKort(Tilbakemelding tilbakemelding) {
        // TODO: legg til webproxy
//        if (!cluster.equals("prod-fss")) return;
        String kortTittel = tilbakemelding.getTilbakemelding().replaceAll("\\ ", "+");

        String uriString = UriComponentsBuilder.fromHttpUrl(CARDS_URL)
                .queryParam("name", kortTittel)
                .queryParam("idList", LIST_ID)
                .queryParam("key", key)
                .queryParam("token", token)
                .toUriString();

        try {
            restTemplate.exchange(
                    uriString,
                    HttpMethod.POST,
                    new HttpEntity<>(new HttpHeaders()),
                    Object.class
            );
            log.info("Tilbakemelding sendt til Trello");

        } catch (RestClientResponseException exception) {
            log.error("Sending av tilbakemelding til Trello feilet", exception);
        }
    }
}
