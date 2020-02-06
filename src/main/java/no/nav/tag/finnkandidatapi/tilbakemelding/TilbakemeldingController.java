package no.nav.tag.finnkandidatapi.tilbakemelding;

import no.nav.metrics.MetricsFactory;
import no.nav.security.oidc.api.Protected;
import no.nav.tag.finnkandidatapi.tilbakemelding.trello.TrelloClient;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TilgangskontrollException;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TilgangskontrollService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Protected
@RestController
@RequestMapping("/tilbakemeldinger")
public class TilbakemeldingController {

    private final TilbakemeldingRepository repository;
    private final TilgangskontrollService tilgangskontrollService;
    private final TilbakemeldingConfig config;
    private final TrelloClient trelloClient;

    public TilbakemeldingController(
            TilbakemeldingRepository repository,
            TilgangskontrollService tilgangskontrollService,
            TilbakemeldingConfig config, TrelloClient trelloClient) {
        this.repository = repository;
        this.tilgangskontrollService = tilgangskontrollService;
        this.config = config;
        this.trelloClient = trelloClient;
    }

    @PostMapping
    public ResponseEntity giTilbakemelding(@RequestBody Tilbakemelding tilbakemelding) {
        repository.lagreTilbakemelding(tilbakemelding);
        MetricsFactory.createEvent("finn-kandidat.tilbakemelding.mottatt")
                .addFieldToReport("behov", tilbakemelding.getBehov())
                .addFieldToReport("tilbakemelding", tilbakemelding.getTilbakemelding())
                .report();

        trelloClient.opprettKort(tilbakemelding);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public List<Tilbakemelding> hentAlleTilbakemeldinger() {
        sjekkLesetilgangTilTilbakemeldinger();
        return repository.hentAlleTilbakemeldinger();
    }

    private void sjekkLesetilgangTilTilbakemeldinger() {
        String innloggetNavIdent = tilgangskontrollService.hentInnloggetVeileder().getNavIdent();
        if (!config.getNavIdenterSomHarLesetilgangTilTilbakemeldinger().contains(innloggetNavIdent)) {
            throw new TilgangskontrollException("Bruker med ident " + innloggetNavIdent + " har ikke tilgang til å se tilbakemeldinger");
        }
    }
}
