package org.uniprot.api.uniprotkb.repository.search.impl;

import static java.util.Collections.emptyList;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrQueryConfigFileReader;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.rest.service.query.UniProtQueryProcessor;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.validation.config.WhitelistFieldConfig;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;

@Configuration
public class PublicationSolrQueryConfig {
    private static final String RESOURCE_LOCATION = "/publication-query.config";

    @Bean(name = "publicationQueryConfig")
    public SolrQueryConfig publicationSolrQueryConf() {
        return new SolrQueryConfigFileReader(RESOURCE_LOCATION).getConfig();
    }

    @Bean
    public SearchFieldConfig publicationSearchFieldConfig() {
        return SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.PUBLICATION);
    }

    @Bean
    public QueryProcessor publicationQueryProcessor(WhitelistFieldConfig whiteListFieldConfig) {
        Map<String, String> publicationWhiteListFields =
                whiteListFieldConfig
                        .getField()
                        .getOrDefault(
                                UniProtDataType.PUBLICATION.toString().toLowerCase(),
                                new HashMap<>());
        return UniProtQueryProcessor.newInstance(
                UniProtQueryProcessorConfig.builder()
                        .optimisableFields(emptyList())
                        .whiteListFields(publicationWhiteListFields)
                        .build());
    }
}
