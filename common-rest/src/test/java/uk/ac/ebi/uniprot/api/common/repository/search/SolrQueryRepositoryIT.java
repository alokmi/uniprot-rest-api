package uk.ac.ebi.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.core.CoreContainer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.web.util.UriComponentsBuilder;
import uk.ac.ebi.uniprot.api.common.exception.InvalidRequestException;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FakeFacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.page.impl.CursorPage;
import uk.ac.ebi.uniprot.indexer.ClosableEmbeddedSolrClient;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.indexer.SolrDataStoreManager;
import uk.ac.ebi.uniprot.indexer.uniprot.mockers.UniProtDocMocker;
import uk.ac.ebi.uniprot.search.SolrCollection;
import uk.ac.ebi.uniprot.search.document.uniprot.UniProtDocument;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author lgonzales
 */
class SolrQueryRepositoryIT {

    private static GeneralSolrQueryRepository queryRepo;

    private static DataStoreManager storeManager;

    @BeforeAll
    static void setUp() {
        try {
            SolrDataStoreManager solrStoreManager = new SolrDataStoreManager();
            CoreContainer container = new CoreContainer(new File(System.getProperty(ClosableEmbeddedSolrClient.SOLR_HOME)).getAbsolutePath());
            container.load();
            ClosableEmbeddedSolrClient solrClient = new ClosableEmbeddedSolrClient(container, SolrCollection.uniprot);
            storeManager = new DataStoreManager(solrStoreManager);
            storeManager.addSolrClient(DataStoreManager.StoreType.UNIPROT, solrClient);

            SolrTemplate template = new SolrTemplate(solrClient);
            template.afterPropertiesSet();
            queryRepo = new GeneralSolrQueryRepository(template);
        } catch (Exception e) {
            fail("Error to setup SolrQueryRepositoryTest", e);
        }
    }

    @AfterEach
    void cleanUp() {
        storeManager.cleanSolr(DataStoreManager.StoreType.UNIPROT);
    }

    // getEntry -------------------
    @Test
    void getEntrySucceeds() {
        // given
        String acc = "P12345";
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, UniProtDocMocker.createDoc(acc));

        // when
        Optional<UniProtDocument> entry = queryRepo.getEntry(queryWithoutFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(true));
        assertThat(entry.orElse(new UniProtDocument()).accession, CoreMatchers.is(acc));
    }

    @Test
    void getEntryWhenNotPresent() {
        // when
        String acc = "XXXXXX";
        Optional<UniProtDocument> entry = queryRepo.getEntry(queryWithoutFacets("accession:" + acc));

        // then
        assertThat(entry.isPresent(), CoreMatchers.is(false));
    }

    @Test
    void invalidQueryExceptionReturned() {
        QueryRetrievalException thrown =
                assertThrows(QueryRetrievalException.class,
                             () -> queryRepo.getEntry(queryWithoutFacets("invalid:invalid")));

        assertEquals("Error executing solr query", thrown.getMessage());
    }

    @Test
    void singlePageResult() {
        // given
        int docCount = 2;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs = docs.stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        CursorPage page = CursorPage.of(null, 2);
        QueryResult<UniProtDocument> queryResult = queryRepo
                .searchPage(queryWithoutFacets(accQuery), page.getCursor(), 2);
        List<String> page1Accs = queryResult.getContent().stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        // then
        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertFalse(page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test")).isPresent());

        assertThat(page1Accs, CoreMatchers.is(expectedPage1Accs));

        assertNotNull(queryResult.getFacets());
        assertTrue(queryResult.getFacets().isEmpty());
    }

    @Test
    void defaultSearch() {
        // given
        UniProtDocument doc = UniProtDocMocker.createDoc("P21802");
        doc.content.add("default value");
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc);

        // when attempt to fetch page 1
        String accQuery = "default value";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery), null, null);

        // then
        assertNotNull(queryResult.getPage());
        CursorPage page = (CursorPage) queryResult.getPage();
        assertFalse(page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test")).isPresent());

        assertNotNull(queryResult.getContent());
        assertEquals(1, queryResult.getContent().size());

        assertNotNull(queryResult.getFacets());
        assertTrue(queryResult.getFacets().isEmpty());
    }

    @Test
    void defaultSearchWithMatchedFieldsRequested() {
        // given
        UniProtDocument doc1 = UniProtDocMocker.createDoc("P21802");
        String findMe = "FIND_ME";
        doc1.proteinNames.add("this is a protein name " + findMe + ".");
        UniProtDocument doc2 = UniProtDocMocker.createDoc("P21803");
        doc2.keywords.add("this is a keyword " + findMe + ", yes it is.");

        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc1, doc2);

        // when attempt to fetch page 1
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithMatchedFields(findMe), null, null);

        // then
        assertNotNull(queryResult.getMatchedFields());
//        assertThat(queryResult.getMatchedFields(), is(not(emptyList())));
        // TODO: 14/06/19 fix this
    }

    @Test
    void invalidDefaultSearchWithMatchedFieldsRequested() {
        // given
        UniProtDocument doc1 = UniProtDocMocker.createDoc("P21802");
        String findMe = "FIND_ME";
        doc1.proteinNames.add("this is a protein name " + findMe + ".");
        UniProtDocument doc2 = UniProtDocMocker.createDoc("P21803");
        doc2.keywords.add("this is a keyword " + findMe + ", yes it is.");

        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, doc1, doc2);

        // when attempt to fetch then error occurs
        assertThrows(InvalidRequestException.class, () -> queryRepo
                .searchPage(queryWithMatchedFields("accession:" + findMe), null, null));
    }

    @Test
    void iterateOverAllThreePages() {
        // given
        int docCount = 5;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);
        List<String> savedAccs = docs.stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        List<String> expectedPage1Accs = asList(savedAccs.get(0), savedAccs.get(1));
        List<String> expectedPage2Accs = asList(savedAccs.get(2), savedAccs.get(3));
        List<String> expectedPage3Accs = Collections.singletonList(savedAccs.get(4));

        // when attempt to fetch page 1
        String accQuery = "accession:*";
        CursorPage page = CursorPage.of(null, 2);
        QueryResult<UniProtDocument> queryResult = queryRepo
                .searchPage(queryWithoutFacets(accQuery), page.getCursor(), 2);
        List<String> page1Accs = queryResult.getContent().stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertTrue(page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test")).isPresent());
        String nextCursor = page.getEncryptedNextCursor();

        // ... and attempt to fetch page 2
        queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery), nextCursor, 2);
        List<String> page2Accs = queryResult.getContent().stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        assertNotNull(queryResult.getPage());
        page = (CursorPage) queryResult.getPage();
        assertTrue(page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test")).isPresent());
        nextCursor = page.getEncryptedNextCursor();

        // ... and attempt to fetch last page 3
        queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery), nextCursor, 2);
        List<String> page3Accs = queryResult.getContent().stream()
                .map(doc -> doc.accession)
                .collect(Collectors.toList());

        page = (CursorPage) queryResult.getPage();
        assertFalse(page.getNextPageLink(UriComponentsBuilder.fromHttpUrl("http://localhost/test")).isPresent());

        // then
        assertThat(page1Accs, CoreMatchers.is(expectedPage1Accs));
        assertThat(page2Accs, CoreMatchers.is(expectedPage2Accs));
        assertThat(page3Accs, CoreMatchers.is(expectedPage3Accs));
    }

    @Test
    void facetsNotRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with no facets
        String accQuery = "accession:*";
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(queryWithoutFacets(accQuery), null, 2);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(0));
    }

    @Test
    void singleFacetRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with facets
        String accQuery = "accession:*";
        SolrRequest query = queryWithFacets(accQuery, Collections.singletonList("reviewed"));
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(query, null, 2);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(Matchers.is(1)));
    }

    @Test
    void multiplesFacetRequested() {
        // given
        int docCount = 10;
        List<UniProtDocument> docs = UniProtDocMocker.createDocs(docCount);
        storeManager.saveDocs(DataStoreManager.StoreType.UNIPROT, docs);

        // when attempt to fetch results with facets
        String accQuery = "accession:*";
        SolrRequest query = queryWithFacets(accQuery, asList("reviewed", "fragment"));
        QueryResult<UniProtDocument> queryResult = queryRepo.searchPage(query, null, 2);

        // then
        assertThat(queryResult.getFacets(), IsCollectionWithSize.hasSize(Matchers.is(2)));
    }

    private SolrRequest queryWithFacets(String query, List<String> facets) {
        return SolrRequest.builder()
                .query(query)
                .defaultQueryOperator(Query.Operator.AND)
                .filterQuery("active:true")
                .facetConfig(new FakeFacetConfigConverter())
                .facets(facets)
                .sort(new Sort(Sort.Direction.ASC, "accession_id"))
                .build();
    }

    private SolrRequest queryWithoutFacets(String query) {
        return SolrRequest.builder()
                .query(query)
                .sort(new Sort(Sort.Direction.ASC, "accession_id"))
                .build();
    }

    private SolrRequest queryWithMatchedFields(String query) {
        return SolrRequest.builder()
                .query(query)
                .termQuery(query)
                .termField("keyword")
                .termField("name")
                .sort(new Sort(Sort.Direction.ASC, "accession_id"))
                .build();
    }

    private static class GeneralSolrQueryRepository extends SolrQueryRepository<UniProtDocument> {
        GeneralSolrQueryRepository(SolrTemplate template) {
            super(template, SolrCollection.uniprot, UniProtDocument.class, new FakeFacetConfigConverter(), new GeneralSolrRequestConverter());
        }
    }

    private static class GeneralSolrRequestConverter extends SolrRequestConverter {
        @Override
        public SolrQuery toSolrQuery(SolrRequest request) {
            SolrQuery solrQuery = super.toSolrQuery(request);

            // required for tests, because EmbeddedSolrServer is not sharded
            solrQuery.setParam("distrib", "false");
            solrQuery.setParam("terms.mincount", "1");

            return solrQuery;
        }
    }
}