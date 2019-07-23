package uk.ac.ebi.uniprot.api.taxonomy.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfig;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
@Getter
@Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class TaxonomyFacetConfig extends FacetConfig {

    private Map<String, FacetProperty> taxonomy = new HashMap<>();

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return taxonomy;
    }

    public Collection<String> getFacetNames() {
        return taxonomy.keySet();
    }

}
