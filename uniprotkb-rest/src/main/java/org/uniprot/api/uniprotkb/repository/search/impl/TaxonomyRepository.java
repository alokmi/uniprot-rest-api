package org.uniprot.api.uniprotkb.repository.search.impl;

import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Repository
public class TaxonomyRepository extends SolrQueryRepository<TaxonomyDocument> {
    protected TaxonomyRepository(
            SolrTemplate solrTemplate,
            TaxonomyFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrTemplate,
                SolrCollection.taxonomy,
                TaxonomyDocument.class,
                facetConfig,
                requestConverter);
    }
}
