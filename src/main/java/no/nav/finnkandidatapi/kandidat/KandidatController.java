package no.nav.finnkandidatapi.kandidat;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.finnkandidatapi.tilgangskontroll.TilgangskontrollService;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.bekk.bekkopen.person.FodselsnummerValidator.isValid;

@Slf4j
@Protected
@RestController
@RequestMapping("/kandidater")
@RequiredArgsConstructor
public class KandidatController {

    private final KandidatService kandidatService;
    private final TilgangskontrollService tilgangskontroll;

    @GetMapping("/{fnrEllerAktørId}")
    public ResponseEntity<Kandidat> hentKandidat(@PathVariable("fnrEllerAktørId") String fnrEllerAktørId) {
        loggBrukAvEndepunkt("hentKandidat");

        boolean erFnr = isValid(fnrEllerAktørId);
        String aktørId = erFnr ? kandidatService.hentAktørId(fnrEllerAktørId) : fnrEllerAktørId;

        tilgangskontroll.sjekkPilotTilgang();
        tilgangskontroll.sjekkLesetilgangTilKandidat(aktørId);

        Kandidat kandidat = kandidatService.hentNyesteKandidat(aktørId).orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(kandidat);
    }

    /**
     * @deprecated finn-kandidat frontend er slått av så endepunktet blir ikke brukt lengre
     */
    @Deprecated
    @GetMapping("/{fnr}/aktorId")
    public ResponseEntity<String> hentAktørId(@PathVariable("fnr") String fnr) {
        loggBrukAvEndepunkt("hentAktørId");
        tilgangskontroll.sjekkPilotTilgang();

        boolean gyldigFnr = isValid(fnr);
        if (!gyldigFnr) {
            return ResponseEntity.badRequest().body("Ugyldig fødselsnummer");
        }

        String aktørId = kandidatService.hentAktørId(fnr);
        return ResponseEntity.ok(aktørId);
    }

    /**
     * @deprecated finn-kandidat frontend er slått av så endepunktet blir ikke brukt lengre
     */
    @Deprecated
    @GetMapping("/{aktørId}/fnr")
    public ResponseEntity<String> hentFnr(@PathVariable("aktørId") String aktørId) {
        loggBrukAvEndepunkt("hentFnr");
        tilgangskontroll.sjekkPilotTilgang();
        String fnr = kandidatService.hentFnr(aktørId);
        return ResponseEntity.ok(fnr);
    }

    /**
     * @deprecated finn-kandidat frontend er slått av så endepunktet blir ikke brukt lengre
     */
    @Deprecated
    @GetMapping
    public ResponseEntity<List<Kandidat>> hentKandidater() {
        loggBrukAvEndepunkt("hentKandidater");
        tilgangskontroll.sjekkPilotTilgang();
        List<Kandidat> kandidater =
                kandidatService.hentKandidater().stream()
                .filter(kandidat -> tilgangskontroll.harLesetilgangTilKandidat(kandidat.getAktørId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(kandidater);
    }

    @PostMapping
    public ResponseEntity<Kandidat> opprettKandidat(@RequestBody KandidatDto kandidat) {
        loggBrukAvEndepunkt("opprettKandidat");

        tilgangskontroll.sjekkPilotTilgang();

        if (kandidat.getAktørId() == null && kandidat.getFnr() != null) {
            String aktørId = kandidatService.hentAktørId(kandidat.getFnr());
            kandidat.setAktørId(aktørId);
        } else if (kandidat.getFnr() == null && kandidat.getAktørId() != null) {
            String fnr = kandidatService.hentFnr(kandidat.getAktørId());
            kandidat.setFnr(fnr);
        } else if (kandidat.getFnr() == null && kandidat.getAktørId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        tilgangskontroll.sjekkSkrivetilgangTilKandidat(kandidat.getAktørId());

        boolean kandidatEksisterer = kandidatService.kandidatEksisterer(kandidat.getAktørId());
        if (kandidatEksisterer) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .build();
        }

        Veileder veileder = tilgangskontroll.hentInnloggetVeileder();
        Kandidat opprettetKandidat = kandidatService.opprettKandidat(kandidat, veileder)
                .orElseThrow(FinnKandidatException::new);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(opprettetKandidat);
    }

    @PutMapping
    public ResponseEntity<Kandidat> endreKandidat(@RequestBody KandidatDto kandidatDto) {
        loggBrukAvEndepunkt("endreKandidat");
        tilgangskontroll.sjekkPilotTilgang();

        if (kandidatDto.getAktørId() == null && kandidatDto.getFnr() != null) {
            String aktørId = kandidatService.hentAktørId(kandidatDto.getFnr());
            kandidatDto.setAktørId(aktørId);
        } else if (kandidatDto.getAktørId() == null && kandidatDto.getFnr() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        tilgangskontroll.sjekkSkrivetilgangTilKandidat(kandidatDto.getAktørId());

        Veileder veileder = tilgangskontroll.hentInnloggetVeileder();
        Kandidat endretKandidat = kandidatService.endreKandidat(kandidatDto, veileder)
                .orElseThrow(FinnKandidatException::new);
        return ResponseEntity.ok(endretKandidat);
    }

    /**
     * @deprecated finn-kandidat frontend er slått av så endepunktet blir ikke brukt lengre
     */
    @Deprecated
    @GetMapping("/{aktørId}/skrivetilgang")
    public ResponseEntity hentSkrivetilgang(@PathVariable("aktørId") String aktørId) {
        loggBrukAvEndepunkt("hentSkrivetilgang");
        tilgangskontroll.sjekkPilotTilgang();
        tilgangskontroll.sjekkSkrivetilgangTilKandidat(aktørId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{fnrEllerAktørId}")
    public ResponseEntity slettKandidat(@PathVariable("fnrEllerAktørId") String fnrEllerAktørId) {
        loggBrukAvEndepunkt("slettKandidat");
        tilgangskontroll.sjekkPilotTilgang();

        boolean erFnr = isValid(fnrEllerAktørId);
        String aktørId = erFnr ? kandidatService.hentAktørId(fnrEllerAktørId) : fnrEllerAktørId;
        String fnr = erFnr ? fnrEllerAktørId : kandidatService.hentFnr(fnrEllerAktørId);

        tilgangskontroll.sjekkSkrivetilgangTilKandidat(aktørId);

        //Nuller ut tilretteleggingsbehov, men sletter ikke..
        KandidatDto kandidatDto = KandidatDto.builder().fnr(fnr).aktørId(aktørId).build();
        Veileder veileder = tilgangskontroll.hentInnloggetVeileder();
        Optional<Kandidat> endretKandidat = kandidatService.endreKandidat(kandidatDto, veileder);
        if (endretKandidat.isPresent()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private void loggBrukAvEndepunkt(String endepunkt) {
        log.info(
                "Bruker med ident {} kaller endepunktet {}.",
                tilgangskontroll.hentInnloggetVeileder().getNavIdent(),
                endepunkt
        );
    }
}
