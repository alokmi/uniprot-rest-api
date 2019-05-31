package uk.ac.ebi.uniprot.api.suggester.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.solr.core.SolrTemplate;
import uk.ac.ebi.uniprot.api.common.exception.InvalidRequestException;
import uk.ac.ebi.uniprot.api.suggester.Suggestion;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDictionary;
import uk.ac.ebi.uniprot.search.document.suggest.SuggestDocument;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Created 18/05/19
 *
 * @author Edd
 */
class SuggesterServiceTest {
    private SuggesterService service;

    @BeforeEach
    void setup() {
        this.service = new SuggesterService(mock(SolrTemplate.class), SolrCollection.suggest);
    }

    @Test
    void correctDictionaryIsFound() {
        SuggestDictionary dict = service.getDictionary("taxonomy");
        assertThat(dict, is(SuggestDictionary.TAXONOMY));
    }

    @Test
    void invalidDictionaryCausesException() {
        assertThrows(InvalidRequestException.class, () -> service.getDictionary("WRONG"));
    }

    @Test
    void emptyDocsConvertsToEmptySuggestions() {
        assertThat(service.convertDocs(emptyList()), is(emptyList()));
    }

    @Test
    void singleDocsConvertsToSingleSuggestion() {
        String id = "id";
        String value = "value";
        String altValue = "altValue";
        String dict = "dict";

        List<Suggestion> actual = service.convertDocs(singletonList(SuggestDocument.builder()
                                                                            .id(id)
                                                                            .value(value)
                                                                            .altValue(altValue)
                                                                            .dictionary(dict)
                                                                            .build()));
        List<Suggestion> expected = singletonList(Suggestion.builder()
                                                          .id(id)
                                                          .value(value + " (" + altValue + ")")
                                                          .build());

        assertThat(actual, is(expected));
    }

    @Test
    void multipleDocsConvertsToMultipleSuggestions() {
        String id = "id";
        String id2 = "id";
        String value = "value";
        String value2 = "value";
        String altValue = "altValue";
        String altValue2A = "altValue2A";
        String altValue2B = "altValue2B";
        String dict = "dict";
        String dict2 = "dict";

        List<Suggestion> actual = service.convertDocs(asList(SuggestDocument.builder()
                                                                     .id(id)
                                                                     .value(value)
                                                                     .altValue(altValue)
                                                                     .dictionary(dict)
                                                                     .build(),
                                                             SuggestDocument.builder()
                                                                     .id(id2)
                                                                     .value(value2)
                                                                     .altValue(altValue2A)
                                                                     .altValue(altValue2B)
                                                                     .dictionary(dict2)
                                                                     .build()));
        List<Suggestion> expected = asList(Suggestion.builder()
                                                   .id(id)
                                                   .value(value + " (" + altValue + ")")
                                                   .build(),
                                           Suggestion.builder()
                                                   .id(id2)
                                                   .value(value2 + " (" + altValue2A + "/" + altValue2B + ")")
                                                   .build());

        assertThat(actual, is(expected));
    }

    @Test
    void singleDocsWithNoAltValueConvertsToSingleSuggestion() {
        String id = "id";
        String value = "value";
        String dict = "dict";

        List<Suggestion> actual = service.convertDocs(singletonList(SuggestDocument.builder()
                                                                            .id(id)
                                                                            .value(value)
                                                                            .dictionary(dict)
                                                                            .build()));
        List<Suggestion> expected = singletonList(Suggestion.builder()
                                                          .id(id)
                                                          .value(value)
                                                          .build());

        assertThat(actual, is(expected));
    }
}