package no.nav.tag.finnkandidatapi.tilbakemelding;

import no.nav.tag.finnkandidatapi.kandidat.Veileder;
import no.nav.tag.finnkandidatapi.tilbakemelding.trello.TrelloClient;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TilgangskontrollException;
import no.nav.tag.finnkandidatapi.tilgangskontroll.TilgangskontrollService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

import static no.nav.tag.finnkandidatapi.TestData.enTilbakemelding;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class TilbakemeldingControllerTest {

    @Mock
    private TilbakemeldingRepository repository;

    @Mock
    private TilgangskontrollService tilgangskontrollService;

    @Mock
    private TilbakemeldingConfig tilbakemeldingConfig;

    @Mock
    private TrelloClient trelloClient;

    private TilbakemeldingController tilbakemeldingController;

    @Before
    public void setUp() {
        tilbakemeldingController = new TilbakemeldingController(
                repository,
                tilgangskontrollService,
                tilbakemeldingConfig,
                trelloClient
        );
    }

    @Test
    public void giTilbakemelding__skal_lagre_tilbakemelding() {
        Tilbakemelding tilbakemelding = enTilbakemelding();
        tilbakemeldingController.giTilbakemelding(tilbakemelding);
        verify(repository, times(1)).lagreTilbakemelding(tilbakemelding);
    }

    @Test
    public void giTilbakemelding__skal_returnere_201_created() {
        Tilbakemelding tilbakemelding = enTilbakemelding();
        assertThat(tilbakemeldingController.giTilbakemelding(tilbakemelding).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    public void hentAlleTilbakemeldinger__skal_gi_alle_tilbakemeldinger() {
        giLesetilgangTil("X12345");
        værInnloggetSom("X12345");

        List<Tilbakemelding> alleTilbakemeldinger = Arrays.asList(
                enTilbakemelding(),
                enTilbakemelding()
        );
        when(repository.hentAlleTilbakemeldinger()).thenReturn(alleTilbakemeldinger);

        assertThat(tilbakemeldingController.hentAlleTilbakemeldinger()).isEqualTo(alleTilbakemeldinger);
    }

    @Test(expected = TilgangskontrollException.class)
    public void hentAlleTilbakemeldinger__skal_returnere_403_hvis_bruker_ikke_har_tilgang() {
        giLesetilgangTil("Z99999");
        værInnloggetSom("X12345");
        tilbakemeldingController.hentAlleTilbakemeldinger();
    }

    private void giLesetilgangTil(String ... identer) {
        when(tilbakemeldingConfig.getNavIdenterSomHarLesetilgangTilTilbakemeldinger()).thenReturn(Arrays.asList(identer));
    }

    private void værInnloggetSom(String navIdent) {
        when(tilgangskontrollService.hentInnloggetVeileder()).thenReturn(new Veileder(navIdent));
    }
}
