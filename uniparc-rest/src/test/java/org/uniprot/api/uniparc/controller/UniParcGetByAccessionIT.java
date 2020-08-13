package org.uniprot.api.uniparc.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.uniparc.UniParcRestApplication;
import org.uniprot.core.util.Utils;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE_VALUE;

/**
 * @author sahmad
 * @since 2020-08-11
 */
@Slf4j
@ContextConfiguration(classes = {UniParcDataStoreTestConfig.class, UniParcRestApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniParcGetByAccessionIT extends AbstractGetByIdTest{

    private static final String getGetByIdEndpoint = "/uniparc/accession/{accession}";

    @Override
    protected String getGetByIdEndpoint() {
        return getGetByIdEndpoint;
    }

    @Test
    void testGetByAccessionSuccess() throws Exception {
        // when
        String accession = "P12301";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].version", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].versionI", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].created", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].lastUpdated", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].properties", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[0].properties", iterableWithSize(4)))
                .andExpect(
                        jsonPath("$.uniParcCrossReferences[*].properties[*].key", notNullValue()))
                .andExpect(
                        jsonPath("$.uniParcCrossReferences[*].properties[*].value", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequence.value", notNullValue()))
                .andExpect(jsonPath("$.sequence.length", notNullValue()))
                .andExpect(jsonPath("$.sequence.molWeight", notNullValue()))
                .andExpect(jsonPath("$.sequence.crc64", notNullValue()))
                .andExpect(jsonPath("$.sequence.md5", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.sequenceFeatures[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].databaseId", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[0].locations", iterableWithSize(2)))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations[*].start", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].locations[*].end", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup.id", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures[*].interproGroup.name", notNullValue()))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)))
                .andExpect(jsonPath("$.taxonomies[*].scientificName", notNullValue()))
                .andExpect(jsonPath("$.taxonomies[*].taxonId", notNullValue()));
    }

    @Test
    void testGetByNonExistingAccession() throws Exception {
        // when
        String accession = "P54321";
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", hasItem("Resource not found")));
    }

    @Test
    void testGetByAccessionWithInvalidAccession() throws Exception {
        String accession = "ABCDEFG";
        // when
        ResultActions response =
                mockMvc.perform(MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                equalTo(
                                        "The 'accession' value has invalid format. It should be a valid UniProtKB accession")));
    }

    @Test
    void testGetByAccessionWithDBFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbTypes = "UniProtKB/TrEMBL,embl";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem(accession)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].database",
                                containsInAnyOrder("UniProtKB/TrEMBL", "EMBL")))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithInvalidDBFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbTypes = "randomDB";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbTypes", dbTypes));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("is invalid UniParc Cross Ref DB Name")));
    }

    @Test
    void testGetByAccessionWithDBIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbIds = "unimes1,P10001,randomId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(2)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(
                        jsonPath(
                                "$.uniParcCrossReferences[*].id",
                                containsInAnyOrder("unimes1", "P10001")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithMoreDBIdsThanSupportedFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String dbIds = "dbId1,dbId2,dbId3,dbId4,dbId5,dbId6,dbId7";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("dbIds", dbIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "is the maximum count limit of comma separated items. You have passed")));
    }

    @Test
    void testGetByAccessionWithTaxonomyIdsFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String taxonIds = "9606,radomTaxonId";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("taxonIds", taxonIds));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(4)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(1)))
                .andExpect(jsonPath("$.taxonomies[0].taxonId", is(9606)));
    }

    @Test
    void testGetByAccessionWithActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "true";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(3)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", hasItem("P12301")))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(
                        jsonPath("$.uniParcCrossReferences[*].active", contains(true, true, true)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @Test
    void testGetByAccessionWithInActiveCrossRefFilterSuccess() throws Exception {
        // when
        String accession = "P12301";
        String active = "false";
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), accession)
                                .param("active", active));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(
                        header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.uniParcId", equalTo("UPI0000083A01")))
                .andExpect(jsonPath("$.uniParcCrossReferences", iterableWithSize(1)))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].id", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].database", notNullValue()))
                .andExpect(jsonPath("$.uniParcCrossReferences[*].active", contains(false)))
                .andExpect(jsonPath("$.sequence", notNullValue()))
                .andExpect(jsonPath("$.sequenceFeatures", iterableWithSize(13)))
                .andExpect(jsonPath("$.taxonomies", iterableWithSize(2)));
    }

    @ParameterizedTest(name = "[{index}] return for fieldName {0} and paths: {1}")
    @MethodSource("getAllReturnedFields")
    void testGetAccessionWithAllAvailableReturnedFields(String name, List<String> paths)
            throws Exception {
        String accession = "P12301";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), accession)
                                .param("fields", name)
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        ResultActions resultActions =
                response.andDo(print())
                        .andExpect(status().is(HttpStatus.OK.value()))
                        .andExpect(
                                header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE));

        for (String path : paths) {
            String returnFieldValidatePath = "$." + path;
            log.info("ReturnField:" + name + " Validation Path: " + returnFieldValidatePath);
            resultActions.andExpect(jsonPath(returnFieldValidatePath).hasJsonPath());
        }
    }

    @Test
    void testGetAccessionWithInvalidReturnedFields() throws Exception {
        String accession = "P12301";
        // when
        ResultActions response =
                mockMvc.perform(
                        get(getGetByIdEndpoint(), accession)
                                .param("fields", "randomField")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.messages", notNullValue()))
                .andExpect(jsonPath("$.messages", iterableWithSize(1)))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString("Invalid fields parameter value ")));
    }

    @ParameterizedTest(name = "[{index}] get by accession with content-type {0}")
    @ValueSource(
            strings = {
                "",
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                UniProtMediaType.FASTA_MEDIA_TYPE_VALUE,
                    TSV_MEDIA_TYPE_VALUE,
                    XLS_MEDIA_TYPE_VALUE, LIST_MEDIA_TYPE_VALUE,
                    LIST_MEDIA_TYPE_VALUE
            })
    void testGetBySupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), "P12301")
                                .header(ACCEPT, contentType));

        if (Utils.nullOrEmpty(contentType)) {
            contentType = MediaType.APPLICATION_JSON_VALUE;
        }
        // then
        response.andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, contentType));
    }

    @ParameterizedTest(name = "[{index}] try to get by accession with content-type {0}")
    @ValueSource(
            strings = {MediaType.APPLICATION_PDF_VALUE, UniProtMediaType.GFF_MEDIA_TYPE_VALUE})
    void testGetByAccessionWithUnsupportedContentTypes(String contentType) throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        MockMvcRequestBuilders.get(getGetByIdEndpoint(), "P12301")
                                .header(ACCEPT, contentType));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(
                        jsonPath(
                                "$.messages[0]",
                                containsString(
                                        "Invalid request received. Requested media type/format not accepted:")));
    }

}
