package no.nav.k9punsj.jobber

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.k9punsj.AbstractContainerBaseTest
import no.nav.k9punsj.fordel.PunsjInnsendingType
import no.nav.k9punsj.journalpost.JournalpostRepository
import no.nav.k9punsj.journalpost.dto.PunsjJournalpost
import no.nav.k9punsj.metrikker.JournalpostMetrikkRepository
import no.nav.k9punsj.metrikker.Metrikk
import no.nav.k9punsj.util.IdGenerator
import no.nav.k9punsj.util.MetricUtils
import no.nav.k9punsj.util.MetricUtils.MetrikkTag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.metrics.MetricsEndpoint
import java.util.*
import java.util.stream.IntStream

internal class MetrikkJobbTest: AbstractContainerBaseTest() {

    private lateinit var metrikkJobb: MetrikkJobb
    private lateinit var metricsEndpoint: MetricsEndpoint

    @Autowired
    lateinit var journalpostRepository: JournalpostRepository

    @Autowired
    lateinit var journalpostMetrikkRepository: JournalpostMetrikkRepository

    @BeforeEach
    internal fun setUp() {
        cleanUpDB()
        val simpleMeterRegistry = SimpleMeterRegistry()
        metrikkJobb = MetrikkJobb(journalpostMetrikkRepository, simpleMeterRegistry)
        metricsEndpoint = MetricsEndpoint(simpleMeterRegistry)
    }

    @AfterEach
    internal fun tearDown() {
        cleanUpDB()
    }

    @Test
    fun sjekk_ferdig_behandlede_journalposter(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode
        )

        journalpostRepository.lagre(punsjJournalpost) { punsjJournalpost }
        journalpostRepository.ferdig(punsjJournalpost.journalpostId)
        metrikkJobb.oppdaterMetrikkMåling()

        MetricUtils.assertGuage(
            metricsEndpoint = metricsEndpoint,
            metric = Metrikk.ANTALL_FERDIG_BEHANDLEDE_JOURNALPOSTER,
            forventetVerdi = 1.0
        )
    }

    @Test
    fun sjekk_uferdig_behandlede_journalposter(): Unit = runBlocking {
        val dummyAktørId = IdGenerator.nesteId()
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = PunsjInnsendingType.PAPIRSØKNAD.kode
        )

        journalpostRepository.lagre(punsjJournalpost) { punsjJournalpost }
        metrikkJobb.oppdaterMetrikkMåling()

        MetricUtils.assertGuage(
            metricsEndpoint = metricsEndpoint,
            metric = Metrikk.ANTALL_UFERDIGE_BEHANDLEDE_JOURNALPOSTER,
            forventetVerdi = 1.0
        )
    }

    @Test
    fun sjekk_journalpostertyper(): Unit = runBlocking {
        genererJournalposter(antall = 9, type = PunsjInnsendingType.PAPIRSØKNAD)
        genererJournalposter(antall = 8, type = PunsjInnsendingType.DIGITAL_ETTERSENDELSE)
        genererJournalposter(antall = 7, type = PunsjInnsendingType.PAPIRETTERSENDELSE)
        genererJournalposter(antall = 6, type = PunsjInnsendingType.KOPI)
        genererJournalposter(antall = 5, type = PunsjInnsendingType.INNLOGGET_CHAT)
        genererJournalposter(antall = 4, type = PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT)
        genererJournalposter(antall = 3, type = PunsjInnsendingType.PAPIRINNTEKTSOPPLYSNINGER)
        genererJournalposter(antall = 2, type = PunsjInnsendingType.SKRIV_TIL_OSS_SPØRMSÅL)
        genererJournalposter(antall = 1, type = PunsjInnsendingType.SKRIV_TIL_OSS_SVAR)
        metrikkJobb.oppdaterMetrikkMåling()

        MetricUtils.assertGuage(
            metricsEndpoint = metricsEndpoint,
            metric = Metrikk.ANTALL_JOURNALPOSTTYPER,
            forventetVerdi = 45.0,
            MetrikkTag(
                "type",
                setOf(
                    PunsjInnsendingType.PAPIRSØKNAD.name,
                    PunsjInnsendingType.DIGITAL_ETTERSENDELSE.name,
                    PunsjInnsendingType.PAPIRETTERSENDELSE.name,
                    PunsjInnsendingType.KOPI.name,
                    PunsjInnsendingType.INNLOGGET_CHAT.name,
                    PunsjInnsendingType.INNTEKTSMELDING_UTGÅTT.name,
                    PunsjInnsendingType.PAPIRINNTEKTSOPPLYSNINGER.name,
                    PunsjInnsendingType.SKRIV_TIL_OSS_SPØRMSÅL.name,
                    PunsjInnsendingType.SKRIV_TIL_OSS_SVAR.name
                )
            )
        )
    }

    private suspend fun opprettJournalpost(dummyAktørId: String, type: PunsjInnsendingType): PunsjJournalpost {
        val punsjJournalpost = PunsjJournalpost(
            uuid = UUID.randomUUID(),
            journalpostId = IdGenerator.nesteId(),
            aktørId = dummyAktørId,
            type = type.kode
        )
        journalpostRepository.lagre(punsjJournalpost) { punsjJournalpost }
        return punsjJournalpost
    }

    private suspend fun genererJournalposter(antall: Int, type: PunsjInnsendingType) {
        IntStream.range(0, antall).forEach {
            runBlocking {
                opprettJournalpost(IdGenerator.nesteId(), type)
            }
        }
    }
}
