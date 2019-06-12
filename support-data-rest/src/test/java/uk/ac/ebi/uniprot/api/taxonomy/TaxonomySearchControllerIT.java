package uk.ac.ebi.uniprot.api.taxonomy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ebi.uniprot.api.DataStoreTestConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.support_data.SupportDataApplication;
import uk.ac.ebi.uniprot.api.taxonomy.repository.TaxonomyFacetConfig;
import uk.ac.ebi.uniprot.domain.taxonomy.TaxonomyEntry;
import uk.ac.ebi.uniprot.domain.taxonomy.builder.TaxonomyEntryBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.taxonomy.TaxonomyJsonConfig;
import uk.ac.ebi.uniprot.search.document.taxonomy.TaxonomyDocument;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ContextConfiguration(classes= {DataStoreTestConfig.class, SupportDataApplication.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class, TaxonomySearchControllerIT.TaxonomySearchContentTypeParamResolver.class,
        TaxonomySearchControllerIT.TaxonomySearchParameterResolver.class})
public class TaxonomySearchControllerIT extends AbstractSearchControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;

    @Autowired
    private TaxonomyFacetConfig facetConfig;

    @Override
    protected void cleanEntries() {
        storeManager.cleanSolr(DataStoreManager.StoreType.TAXONOMY);
    }

    @Override
    protected MockMvc getMockMvc() {
        return mockMvc;
    }

    @Override
    protected String getSearchRequestPath() {
        return "/taxonomy/search";
    }

    @Override
    protected int getDefaultPageSize() {
        return 25;
    }

    @Override
    protected List<SearchField> getAllSearchFields() {
        return Arrays.asList(TaxonomyField.Search.values());
    }

    @Override
    protected String getFieldValueForValidatedField(SearchField searchField) {
        String value = "";
        switch (searchField.getName()) {
            case "id":
            case "tax_id":
            case "host":
                value = "10";
                break;
        }
        return value;
    }

    @Override
    protected List<String> getAllSortFields() {
        return Arrays.stream(TaxonomyField.Sort.values())
                .map(TaxonomyField.Sort::name)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getAllFacetFields() {
        return new ArrayList<>(facetConfig.getFacetNames());
    }

    @Override
    protected List<String> getAllReturnedFields() {
        return Arrays.stream(TaxonomyField.ResultFields.values())
                .map(TaxonomyField.ResultFields::name)
                .collect(Collectors.toList());
    }

    @Override
    protected void saveEntries(int numberOfEntries) {
        LongStream.rangeClosed(1,numberOfEntries).forEach(i -> saveEntry(i,i%2==0));
    }

    @Override
    protected void saveEntry(SaveScenario saveContext) {
        saveEntry(10,true);
        saveEntry(20,false);
    }

    private void saveEntry(long taxId,boolean facet) {

        TaxonomyEntryBuilder entryBuilder = new TaxonomyEntryBuilder();
        TaxonomyEntry taxonomyEntry = entryBuilder
                .taxonId(taxId)
                .scientificName("scientific"+taxId)
                .mnemonic("mnemonic"+taxId)
                .commonName("common"+taxId)
                .addSynonyms("synonym"+taxId)
                .parentId(taxId-1)
                .build();

        TaxonomyDocument document = TaxonomyDocument.builder()
                .id(String.valueOf(taxId))
                .taxId(taxId)
                .synonym("synonym"+taxId)
                .scientific("scientific"+taxId)
                .common("common"+taxId)
                .mnemonic("mnemonic"+taxId)
                .rank("rank")
                .strain(Collections.singletonList("strain"))
                .host(Collections.singletonList(10L))
                .complete(facet)
                .reference(facet)
                .reviewed(facet)
                .annotated(facet)
                .linked(facet)
                .active(facet)
                .taxonomyObj(getTaxonomyBinary(taxonomyEntry))
                .build();

        storeManager.saveDocs(DataStoreManager.StoreType.TAXONOMY,document);
    }

    private ByteBuffer getTaxonomyBinary(TaxonomyEntry entry) {
        try {
            return ByteBuffer.wrap(TaxonomyJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
        }
    }

    static class TaxonomySearchParameterResolver extends AbstractSearchParameterResolver {

        @Override
        protected SearchParameter searchCanReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("tax_id:"+10))
                    .resultMatcher(jsonPath("$.results.*.taxonId",contains(10)))
                    .resultMatcher(jsonPath("$.results.*.scientificName",contains("scientific10")))
                    .resultMatcher(jsonPath("$.results.*.commonName",contains("common10")))
                    .build();
        }

        @Override
        protected SearchParameter searchCanReturnNotFoundParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("tax_id:9999"))
                    .resultMatcher(jsonPath("$.results.size()",is(0)))
                    .build();
        }

        @Override
        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("scientific:*"))
                    .resultMatcher(jsonPath("$.results.*.taxonId",contains(10,20)))
                    .resultMatcher(jsonPath("$.results.*.scientificName",contains("scientific10","scientific20")))
                    .resultMatcher(jsonPath("$.results.*.commonName",contains("common10","common20")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("scientific:[1 TO 10]"))
                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*",contains("'scientific' filter type 'range' is invalid. Expected 'term' filter type")))
                    .build();
        }

        @Override
        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("tax_id:INVALID OR id:INVALID " +
                            "OR host:INVALID OR linked:invalid OR active:invalid OR complete:invalid " +
                            "OR reference:invalid OR reviewed:invalid OR annotated:invalid"))
                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                    .resultMatcher(jsonPath("$.messages.*",containsInAnyOrder(
                            "The taxonomy active filter value should be a boolean",
                            "The taxonomy complete filter value should be a boolean",
                            "The taxonomy reference filter value should be a boolean",
                            "The taxonomy id filter value should be a number",
                            "The taxonomy linked filter value should be a boolean",
                            "The taxonomy id filter value should be a number",
                            "The taxonomy host filter value should be a number",
                            "The taxonomy reviewed filter value should be a boolean",
                            "The taxonomy annotated filter value should be a boolean")))
                    .build();
        }

        @Override
        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("sort", Collections.singletonList("name desc"))
                    .resultMatcher(jsonPath("$.results.*.taxonId",contains(20,10)))
                    .resultMatcher(jsonPath("$.results.*.scientificName",contains("scientific20","scientific10")))
                    .resultMatcher(jsonPath("$.results.*.commonName",contains("common20","common10")))
                    .build();
        }

        @Override
        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("fields", Collections.singletonList("common_name,mnemonic"))
                    .resultMatcher(jsonPath("$.results.*.taxonId",contains(10,20)))
                    .resultMatcher(jsonPath("$.results.*.scientificName").doesNotExist())
                    .resultMatcher(jsonPath("$.results.*.commonName",contains("common10","common20")))
                    .resultMatcher(jsonPath("$.results.*.mnemonic",contains("mnemonic10","mnemonic20")))
                    .build();
        }

        @Override
        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
            return SearchParameter.builder()
                    .queryParam("query", Collections.singletonList("*:*"))
                    .queryParam("facets", Collections.singletonList("reviewed,reference"))
                    .resultMatcher(jsonPath("$.results.*.taxonId",contains(10,20)))
                    .resultMatcher(jsonPath("$.results.*.scientificName",contains("scientific10","scientific20")))
                    .resultMatcher(jsonPath("$.results.*.commonName",contains("common10","common20")))
                    .resultMatcher(jsonPath("$.results.*.mnemonic",contains("mnemonic10","mnemonic20")))
                    .resultMatcher(jsonPath("$.facets",notNullValue()))
                    .resultMatcher(jsonPath("$.facets",not(empty())))
                    .resultMatcher(jsonPath("$.facets.*.name",contains("reviewed","reference")))
                    .build();
        }
    }


    static class TaxonomySearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver{

        @Override
        protected SearchContentTypeParam searchSuccessContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("tax_id:10 OR tax_id:20")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.results.*.taxonId",contains(10,20)))
                            .resultMatcher(jsonPath("$.results.*.scientificName",contains("scientific10","scientific20")))
                            .resultMatcher(jsonPath("$.results.*.commonName",contains("common10","common20")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("10")))
                            .resultMatcher(content().string(containsString("20")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(containsString("Taxon\tMnemonic\tScientific name\tCommon name\tOther Names\tReviewed\tRank\tLineage\tParent\tVirus hosts")))
                            .resultMatcher(content().string(containsString("10\tmnemonic10\tscientific10\tcommon10\t\t\t\t\t9")))
                            .resultMatcher(content().string(containsString("20\tmnemonic20\tscientific20\tcommon20\t\t\t\t\t19")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
                            .build())
                    .build();
        }

        @Override
        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
            return SearchContentTypeParam.builder()
                    .query("tax_id:invalid")
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(MediaType.APPLICATION_JSON)
                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
                            .resultMatcher(jsonPath("$.messages.*",contains("The taxonomy id filter value should be a number")))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .contentTypeParam(ContentTypeParam.builder()
                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
                            .resultMatcher(content().string(isEmptyString()))
                            .build())
                    .build();
        }
    }

}