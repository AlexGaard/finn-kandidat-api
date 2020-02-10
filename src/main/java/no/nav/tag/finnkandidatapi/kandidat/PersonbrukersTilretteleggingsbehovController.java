package no.nav.tag.finnkandidatapi.kandidat;

import no.nav.security.oidc.api.ProtectedWithClaims;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TokenUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@ProtectedWithClaims(issuer = "selvbetjening")
@RestController
public class PersonbrukersTilretteleggingsbehovController {

    private final KandidatService kandidatService;
    private final TokenUtils tokenUtils;
    private final VeilarbOppfolgingClient veilarbOppfolgingClient;

    public PersonbrukersTilretteleggingsbehovController(
            KandidatService kandidatService,
            TokenUtils tokenUtils,
            VeilarbOppfolgingClient veilarbOppfolgingClient
    ) {
        this.kandidatService = kandidatService;
        this.tokenUtils = tokenUtils;
        this.veilarbOppfolgingClient = veilarbOppfolgingClient;
    }

    @GetMapping("/tilretteleggingsbehov")
    public ResponseEntity<Kandidat> hentTilretteleggingsbehov() {
        String fnr = tokenUtils.hentInnloggetBruker();
        String aktørId = kandidatService.hentAktørId(fnr);
        Optional<Kandidat> kandidat = kandidatService.hentNyesteKandidat(aktørId);
        return kandidat
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returnerer om en bruker er registrert som arbeidssøker.
     */
    @GetMapping("/oppfolgingsstatus")
    public ResponseEntity<Oppfølgingsstatus> hentOppfølgingsstatus() {
        Oppfølgingsstatus oppfølgingsstatus = veilarbOppfolgingClient.hentOppfølgingsstatus();
        return ResponseEntity.ok(oppfølgingsstatus);
    }
}