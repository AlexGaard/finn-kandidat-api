package no.nav.finnkandidatapi.kandidat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.util.Map;

import static no.nav.finnkandidatapi.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KandidatRepositoryEnhetTest {
    @Mock
    private KandidatMapper kandidatMapper;

    @Mock
    private HarTilretteleggingsbehovMapper harTilretteleggingsbehovMapper;

    @Mock
    private SimpleJdbcInsert jdbcInsert;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Captor
    ArgumentCaptor<Map<String, Object>> requestCaptor;

    private KandidatRepository repository;

    @Before
    public void setUp() {
        when(jdbcInsert.withTableName(anyString())).thenReturn(jdbcInsert);
        when(jdbcInsert.usingGeneratedKeyColumns(anyString())).thenReturn(jdbcInsert);
        when(jdbcInsert.executeAndReturnKey(any(Map.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(any(), any(), eq(kandidatMapper))).thenReturn(enKandidat());

        repository = new KandidatRepository(jdbcTemplate, jdbcInsert, kandidatMapper, harTilretteleggingsbehovMapper);
    }

    @Test
    public void slettKandidat__skal_sette_veileder_som_brukertype() {
        repository.slettKandidatSomVeileder(enKandidat().getAktørId(), enVeileder(), now());
        verify(jdbcInsert, times(1)).executeAndReturnKey(requestCaptor.capture());
        assertThat(requestCaptor.getValue().get("registrert_av_brukertype")).isEqualTo(Brukertype.VEILEDER.name());
    }

    @Test
    public void slettKandidatSomMaskinbruker__skal_sette_maskin_som_brukertype() {
        repository.slettKandidatSomMaskinbruker(enKandidat().getAktørId(), now());
        verify(jdbcInsert, times(1)).executeAndReturnKey(requestCaptor.capture());
        assertThat(requestCaptor.getValue().get("registrert_av_brukertype")).isEqualTo(Brukertype.SYSTEM.name());
    }
}
