package org.uniprot.api.uniparc.request;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Data;

import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.validation.*;
import org.uniprot.api.uniparc.repository.UniParcFacetConfig;
import org.uniprot.store.config.UniProtDataType;

/**
 * @author jluo
 * @date: 20 Jun 2019
 */
@Data
public class UniParcRequest implements SearchRequest {
    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(
            uniProtDataType = UniProtDataType.UNIPARC,
            messagePrefix = "search.uniparc")
    private String query;

    @ValidSolrSortFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String sort;

    private String cursor;

    @ValidReturnFields(uniProtDataType = UniProtDataType.UNIPARC)
    private String fields;

    @ValidFacets(facetConfig = UniParcFacetConfig.class)
    private String facets;

    @Positive(message = "{search.positive}")
    private Integer size;

    public static final String DEFAULT_FIELDS =
            "upi,organism,accession,first_seen,last_seen,length";
}
