package uk.ac.ebi.uniprot.api.proteome.request;

import com.google.common.base.Strings;
import lombok.Data;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.rest.request.SearchRequest;
import uk.ac.ebi.uniprot.api.rest.validation.*;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 *
 * @author jluo
 * @date: 17 May 2019
 *
*/
@Data
public class GeneCentricRequest  implements SearchRequest{
	 private static final int DEFAULT_RESULTS_SIZE = 25;

	    @NotNull(message = "{search.required}")
	    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
        @ValidSolrQueryFields(fieldValidatorClazz = GeneCentricField.Search.class, messagePrefix = "search.genecentric")
	    private String query;

	    @ValidSolrSortFields(sortFieldEnumClazz = GeneCentricField.Sort.class)
	    private String sort;

	    private String cursor;

    @ValidReturnFields(fieldValidatorClazz = GeneCentricField.ResultFields.class)
	    private String fields;

	    @ValidFacets(facetConfig = GeneCentricFacetConfig.class)
	    private String facets;

	    @Positive(message = "{search.positive}")
	    private int size = DEFAULT_RESULTS_SIZE;
	    private static final String DEFAULT_FIELDS="accession_id";
	    @Override
	    public String getFields() {
	    	if(Strings.isNullOrEmpty(fields)) {
	    		fields =DEFAULT_FIELDS;
	    	}else if(!fields.contains(GeneCentricField.ResultFields.accession_id.name())) {
	    		String temp = "accession_id,"+fields;
	    		this.fields= temp;
	    	}
	    	return fields;
	    }
}
