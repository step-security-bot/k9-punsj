package no.nav.k9punsj.rest.web

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.k9punsj.db.datamodell.NorskIdent
import no.nav.k9punsj.db.datamodell.Periode
import no.nav.k9punsj.rest.web.dto.JournalpostIdDto
import no.nav.k9punsj.rest.web.dto.SøknadIdDto
import org.springframework.http.HttpStatus

internal val objectMapper = jacksonObjectMapper()

typealias JournalpostId = String

typealias SøknadJson = MutableMap<String, Any?>

internal fun SøknadJson.mergeNy(nySøknad: SøknadJson) : SøknadJson {
    val før = objectMapper.valueToTree<ObjectNode>(this)
    val nytt = objectMapper.valueToTree<ObjectNode>(nySøknad)
    val merged = objectMapper.convertValue<SøknadJson>(merge(før, nytt))
    clear()
    putAll(merged)
    return this
}

data class Innsending(
        val personer: Map<NorskIdent, JournalpostInnhold<SøknadJson>>
)

data class HentSøknad(
    val norskIdent: NorskIdent,
    val periode: Periode
)

data class JournalpostInnhold<T>(
        val journalpostId: JournalpostIdDto,
        val soeknad: T,
        val søknadIdDto: SøknadIdDto? = null
)

internal fun Boolean.httpStatus() = if (this) HttpStatus.OK else HttpStatus.BAD_REQUEST

private fun merge(mainNode: JsonNode, updateNode: JsonNode): JsonNode {
    updateNode.fieldNames().forEach { fieldName ->
        val jsonNode = mainNode.get(fieldName)
        if (jsonNode != null && jsonNode.isObject) {
            merge(jsonNode, updateNode.get(fieldName))
        } else {
            if (mainNode is ObjectNode) {
                val value = updateNode.get(fieldName)
                mainNode.put(fieldName, value)
            }
        }
    }
    return mainNode
}