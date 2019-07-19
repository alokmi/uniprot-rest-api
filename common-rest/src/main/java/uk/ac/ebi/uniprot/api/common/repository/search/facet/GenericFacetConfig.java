package uk.ac.ebi.uniprot.api.common.repository.search.facet;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * This class contains generic facet configuration
 *
 * @author lgonzales
 */
@Getter
@Setter
public abstract class GenericFacetConfig {
    private int mincount;

    private int limit;

    public Collection<String> getFacetNames() {
        return Collections.emptySet();
    }

    public abstract Map<String, FacetProperty> getFacetPropertyMap();
}
