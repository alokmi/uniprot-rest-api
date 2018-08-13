package uk.ac.ebi.uniprot.uuw.advanced.search.model.response;


import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.facet.Facet;
import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.page.Page;

import java.util.Collection;

/**
 * Solr Repository response entity
 *
 * @author lgonzales
 */
public class QueryResult<T> {

    private final Page page;
    private final Collection<T> content;
    private final Collection<Facet> facets;

    private  QueryResult(Collection<T> content, Page page,Collection<Facet> facets) {
        this.content = content;
        this.page = page;
        this.facets = facets;
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page){
        return new QueryResult<>(content,page,null);
    }

    public static <T> QueryResult<T> of(Collection<T> content, Page page,Collection<Facet> facets){
        return new QueryResult<>(content,page,facets);
    }

    public Page getPage() {
        return page;
    }

    public Collection<T> getContent() {
        return content;
    }

    public Collection<Facet> getFacets() {
        return facets;
    }
}
