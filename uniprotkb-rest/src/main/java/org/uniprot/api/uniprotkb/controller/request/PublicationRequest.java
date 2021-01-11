package org.uniprot.api.uniprotkb.controller.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.http.MediaType;
import org.uniprot.api.rest.validation.ValidContentTypes;
import org.uniprot.api.rest.validation.ValidFacets;
import org.uniprot.api.rest.validation.ValidSolrQueryFacetFields;
import org.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import org.uniprot.api.uniprotkb.service.PublicationFacetConfig2;
import org.uniprot.core.util.Utils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.uniprot.api.rest.request.SearchRequest.MAX_RESULTS_SIZE;

/**
 * /search?query=???? /accession/P12345/publications?query=????
 *
 * @author lgonzales
 * @since 2019-07-09
 */
@Data
public class PublicationRequest {

    @Parameter(description = "Size of the result. Defaults to 25")
    @Positive(message = "{search.positive}")
    @Max(value = MAX_RESULTS_SIZE, message = "{search.max.page.size}")
    private Integer size;

    @Parameter(hidden = true)
    private String cursor;

    @Parameter(description = "Facet filter query for Publications")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFacetFields(facetConfig = PublicationFacetConfig2.class)
    private String query;

    @Parameter(description = "Name of the facet search")
    @ValidContentTypes(contentTypes = {MediaType.APPLICATION_JSON_VALUE})
    @ValidFacets(facetConfig = PublicationFacetConfig2.class)
    private String facets;
}
