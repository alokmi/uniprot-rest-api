package org.uniprot.api.uniref.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.rest.request.SearchRequest;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.service.StoreStreamerSearchService;
import org.uniprot.api.rest.service.query.QueryProcessor;
import org.uniprot.api.uniref.repository.UniRefFacetConfig;
import org.uniprot.api.uniref.repository.UniRefQueryRepository;
import org.uniprot.api.uniref.request.UniRefSearchRequest;
import org.uniprot.api.uniref.request.UniRefStreamRequest;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.uniref.UniRefDocument;

/**
 * @author jluo
 * @date: 20 Aug 2019
 */
@Service
@Import(UniRefSolrQueryConfig.class)
public class UniRefLightSearchService
        extends StoreStreamerSearchService<UniRefDocument, UniRefEntryLight> {
    private final SolrQueryConfig solrQueryConfig;
    private final StoreStreamer<UniRefEntryLight> storeStreamer;
    private static final int ID_LIMIT = 10;
    public static final String UNIREF_ID = "id";
    public static final String UNIREF_UPI = "upi";
    private final SearchFieldConfig searchFieldConfig;
    private final QueryProcessor queryProcessor;

    @Autowired
    public UniRefLightSearchService(
            UniRefQueryRepository repository,
            UniRefFacetConfig facetConfig,
            UniRefSortClause uniRefSortClause,
            UniRefLightQueryResultConverter uniRefQueryResultConverter,
            StoreStreamer<UniRefEntryLight> storeStreamer,
            SolrQueryConfig uniRefSolrQueryConf,
            QueryProcessor uniRefQueryProcessor,
            SearchFieldConfig uniRefSearchFieldConfig) {
        super(
                repository,
                uniRefQueryResultConverter,
                uniRefSortClause,
                facetConfig,
                storeStreamer,
                uniRefSolrQueryConf);
        this.searchFieldConfig = uniRefSearchFieldConfig;
        this.queryProcessor = uniRefQueryProcessor;
        this.solrQueryConfig = uniRefSolrQueryConf;
        this.storeStreamer = storeStreamer;
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId, String fields) {
        return findByUniqueId(uniqueId);
    }

    @Override
    public QueryResult<UniRefEntryLight> search(SearchRequest request) {
        UniRefSearchRequest unirefRequest = (UniRefSearchRequest) request;
        QueryResult<UniRefEntryLight> result = super.search(request);
        if (!unirefRequest.isComplete()) {
            Stream<UniRefEntryLight> content =
                    result.getContent().map(this::removeOverLimitAndCleanMemberId);

            result = QueryResult.of(content, result.getPage(), result.getFacets());
        } else {
            Stream<UniRefEntryLight> content = result.getContent().map(this::cleanMemberId);

            result = QueryResult.of(content, result.getPage(), result.getFacets());
        }
        return result;
    }

    @Override
    public Stream<UniRefEntryLight> stream(StreamRequest request) {
        UniRefStreamRequest unirefRequest = (UniRefStreamRequest) request;
        Stream<UniRefEntryLight> result = super.stream(request);
        if (!unirefRequest.isComplete()) {
            result = result.map(this::removeOverLimitAndCleanMemberId);
        } else {
            result = result.map(this::cleanMemberId);
        }
        return result;
    }

    @Override
    public UniRefEntryLight findByUniqueId(String uniqueId) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support findByUniqueId, try to use UniRefEntryService");
    }

    @Override
    public UniRefEntryLight getEntity(String idField, String value) {
        throw new UnsupportedOperationException(
                "UniRefLightSearchService does not support getEntity, try to use UniRefEntryService");
    }

    public Stream<String> streamRDF(UniRefStreamRequest streamRequest) {
        SolrRequest solrRequest =
                createSolrRequestBuilder(streamRequest, solrSortClause, solrQueryConfig).build();
        return this.storeStreamer.idsToRDFStoreStream(solrRequest);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return this.searchFieldConfig.getSearchFieldItemByName(UNIREF_ID);
    }

    @Override
    protected QueryProcessor getQueryProcessor() {
        return queryProcessor;
    }

    private UniRefEntryLight cleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = removeMemberTypeFromMemberId(entry.getMembers());
        builder.membersSet(members);

        return builder.build();
    }

    private UniRefEntryLight removeOverLimitAndCleanMemberId(UniRefEntryLight entry) {
        UniRefEntryLightBuilder builder = UniRefEntryLightBuilder.from(entry);

        List<String> members = entry.getMembers();
        if (entry.getMembers().size() > ID_LIMIT) {
            members = entry.getMembers().subList(0, ID_LIMIT);
        }

        members = removeMemberTypeFromMemberId(members);
        builder.membersSet(members);

        if (entry.getOrganisms().size() > ID_LIMIT) {
            LinkedHashSet<Organism> organisms =
                    entry.getOrganisms().stream()
                            .limit(ID_LIMIT)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
            builder.organismsSet(organisms);
        }
        return builder.build();
    }

    /**
     * This method remove MemberIdType from member list and return just memberId
     *
     * @param members List of members that are stored in Voldemort with format:
     *     "memberId,MemberIdType"
     * @return List of return clean member with the format "memberId"
     */
    private List<String> removeMemberTypeFromMemberId(List<String> members) {
        return members.stream()
                .map(memberId -> memberId.split(",")[0])
                .collect(Collectors.toList());
    }
}
