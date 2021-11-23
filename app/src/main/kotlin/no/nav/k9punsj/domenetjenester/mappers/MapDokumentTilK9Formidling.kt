package no.nav.k9punsj.domenetjenester.mappers

import kotlinx.coroutines.runBlocking
import no.nav.k9.formidling.kontrakt.hendelse.Dokumentbestilling
import no.nav.k9.formidling.kontrakt.kodeverk.*
import no.nav.k9.søknad.felles.Feil
import no.nav.k9punsj.brev.BrevId
import no.nav.k9punsj.brev.DokumentbestillingDto
import no.nav.k9punsj.db.datamodell.AktørId
import no.nav.k9punsj.db.datamodell.JsonB
import no.nav.k9punsj.objectMapper
import no.nav.k9punsj.rest.eksternt.pdl.PdlService
import org.slf4j.LoggerFactory

internal class MapDokumentTilK9Formidling(
    brevId: BrevId,
    dto: DokumentbestillingDto,
    val pdlService: PdlService
) {

    private val bestilling = Dokumentbestilling()
    private val feil = mutableListOf<Feil>()

    init {
        kotlin.runCatching {
            dto.journalpostId.leggTilEksternRefernase()
            brevId.leggTilDokumentbestillingId()
            dto.saksnummer.leggTilSaksnummer()
            dto.soekerId.transformTilAktørId()
            dto.mottaker.leggTilMottaker()
            dto.fagsakYtelseType.leggTilFagsakTyelse()
            dto.dokumentMal.leggTilDokumentMal()
            dto.dokumentdata.leggTilDokumentData()

            //TODO fix når k9punsj er lag til
            bestilling.avsenderApplikasjon = AvsenderApplikasjon.K9SAK

            feil.addAll(validator.validate(bestilling).map { Feil(it.propertyPath.toString(), "kode", it.message) })
        }
    }

    internal fun bestilling() = bestilling
    internal fun feil() = feil.toList()
    internal fun bestillingOgFeil() = bestilling() to feil()


    private fun String.leggTilEksternRefernase() {
        bestilling.eksternReferanse = this

    }
    private fun String.leggTilDokumentbestillingId() {
        bestilling.dokumentbestillingId = this
    }

    private fun String?.leggTilSaksnummer() {
        bestilling.saksnummer = this ?: "GSAK"
    }

    private fun AktørId.transformTilAktørId() {
        val søkerId = this
        runBlocking {
            val identifikator = pdlService.identifikator(søkerId)
            val hentIdenter = identifikator?.identPdl?.data?.hentIdenter
            if (hentIdenter != null) {
                bestilling.aktørId = hentIdenter.identer[0].ident
            } else {
                feil.add(Feil("AktørId", "AktørId", "Kunne ikke finne person i pdl"))
            }
        }
    }

    private fun DokumentbestillingDto.Mottaker.leggTilMottaker() {
        kotlin.runCatching { Mottaker(this.id, IdType.valueOf(this.type))}
            .onSuccess { bestilling.overstyrtMottaker = it }
            .onFailure { feil.add(Feil("Mottaker", "Mottaker", it.message)) }
    }

    private fun FagsakYtelseType.leggTilFagsakTyelse() {
        bestilling.ytelseType = this
    }

    private fun String.leggTilDokumentMal() {
        kotlin.runCatching { DokumentMalType.fraKode(this) }
            .onSuccess { bestilling.dokumentMal = it.kode }
            .onFailure { feil.add(Feil("DokumentMalType", "DokumentMalType", it.message)) }
    }

    private fun JsonB?.leggTilDokumentData() {
        kotlin.runCatching { objectMapper().writeValueAsString(this) }
            .onSuccess { bestilling.dokumentdata = it }
            .onFailure { feil.add(Feil("DokumentData", "DokumentData", it.message)) }
    }

    internal companion object {
        private val logger = LoggerFactory.getLogger(MapDokumentTilK9Formidling::class.java)
        private val validator = javax.validation.Validation.buildDefaultValidatorFactory().validator

    }
}