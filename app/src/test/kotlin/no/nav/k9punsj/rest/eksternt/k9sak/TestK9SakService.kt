package no.nav.k9punsj.rest.eksternt.k9sak

import no.nav.k9punsj.TestProfil
import no.nav.k9punsj.db.datamodell.FagsakYtelseType
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.rest.web.dto.ArbeidsgiverMedArbeidsforholdId
import no.nav.k9punsj.rest.web.dto.PeriodeDto
import org.springframework.stereotype.Component

@Component
@TestProfil
internal class TestK9SakService : K9SakService {
    override suspend fun hentPerioderSomFinnesIK9(
        søker: NorskIdent,
        barn: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
    ): Pair<List<PeriodeDto>?, String?> = Pair(emptyList(), null)

    override suspend fun hentArbeidsforholdIdFraInntektsmeldinger(
        søker: NorskIdent,
        fagsakYtelseType: FagsakYtelseType,
        periodeDto: PeriodeDto,
    ): Pair<List<ArbeidsgiverMedArbeidsforholdId>?, String?> {
        //simulerer svar på denne
        return if (søker == "02020050123") {
            Pair(listOf(ArbeidsgiverMedArbeidsforholdId("randomOrgNummer", listOf("randomArbeidsforholdId"))), null)
        } else {
            Pair(emptyList(), null)
        }
    }
}

