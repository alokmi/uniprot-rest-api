package org.uniprot.api.common.repository.store;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.slf4j.Logger;
import org.springframework.data.domain.Sort;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.store.datastore.UniProtStoreClient;
import org.uniprot.store.datastore.voldemort.VoldemortClient;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created 22/08/18
 *
 * @author Edd
 */
@ExtendWith(MockitoExtension.class)
class StoreStreamerIT {
    private static final String ID = "id";
    private static final String DEFAULTS = "defaults";
    private static final String FAKE_QUERY = "any query";
    private static final String FAKE_FILTER_QUERY = "any filter query";
    private static final Sort FAKE_SORT = new Sort(Sort.Direction.ASC, "any field");
    private FakeUniProtStoreClient fakeUniProtStoreClient;
    private StoreStreamer<String> storeStreamer;
    private SolrRequest solrRequest;
    private static final Logger LOGGER = getLogger(StoreStreamerIT.class);

    @Mock
    private VoldemortClient<String> fakeClient;

    static String transformString(String id) {
        return id + "-transformed";
    }

    @BeforeEach
    void setUp() {
        fakeUniProtStoreClient = new FakeUniProtStoreClient(fakeClient);
        solrRequest = SolrRequest.builder()
                .query(FAKE_QUERY)
                .sort(FAKE_SORT)
                .filterQuery(FAKE_FILTER_QUERY)
                .build();
    }

    @Test
    void canCreateSearchStoreStream() {
        createSearchStoreStream(1, tupleStream(singletonList("a")));
        assertThat(storeStreamer, is(notNullValue()));
    }

    @Test
    void canTransformSourceStreamWithUnaryBatchSize() {
        createSearchStoreStream(1, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer.idsToStoreStream(solrRequest);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithIntermediateBatchSize() {
        createSearchStoreStream(3, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer.idsToStoreStream(solrRequest);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithBiggerBatchSize() {
        createSearchStoreStream(4, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer.idsToStoreStream(solrRequest);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    @Test
    void canTransformSourceStreamWithBatchSizeGreaterThanSourceElements() {
        createSearchStoreStream(10, tupleStream(asList("a", "b", "c", "d", "e")));
        Stream<String> storeStream = storeStreamer.idsToStoreStream(solrRequest);
        List<String> results = storeStream.collect(Collectors.toList());
        assertThat(results, contains(
                transformString("a"),
                transformString("b"),
                transformString("c"),
                transformString("d"),
                transformString("e")));
    }

    private void createSearchStoreStream(int streamerBatchSize, TupleStream tupleStream) {
        TupleStreamTemplate mockTupleStreamTemplate = mock(TupleStreamTemplate.class);
        when(mockTupleStreamTemplate.create(ArgumentMatchers.any(),ArgumentMatchers.any())).thenReturn(tupleStream);
        this.storeStreamer = StoreStreamer.<String>builder()
                .storeClient(fakeUniProtStoreClient)
                .streamerBatchSize(streamerBatchSize)
                .id(ID)
                .tupleStreamTemplate(mockTupleStreamTemplate)
                .defaultsField(DEFAULTS)
                .defaultsConverter(s -> s)
                .build();
    }

    private TupleStream tupleStream(Collection<String> values) {
        TupleStream mockTupleStream = mock(TupleStream.class);

        try {
            OngoingStubbing<Tuple> ongoingStubbing = when(mockTupleStream.read());
            for (String value : values) {
                LOGGER.debug("hello " + value);
                ongoingStubbing = ongoingStubbing.thenReturn(tuple(value));
            }

            ongoingStubbing.thenReturn(endTuple());
        } catch (IOException e) {
            LOGGER.error("Error when tupleStream",e);
        }

        return mockTupleStream;
    }

    private Tuple tuple(String accession) {
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put(ID, accession);
        return new Tuple(valueMap);
    }

    private Tuple endTuple() {
        Map<String, String> eofMap = new HashMap<>();
        eofMap.put("EOF", "");
        return new Tuple(eofMap);
    }

    private static class FakeUniProtStoreClient extends UniProtStoreClient<String> {
        FakeUniProtStoreClient(VoldemortClient<String> client) {
            super(client);
        }

        @Override
        public String getStoreName() {
            return null;
        }

        @Override
        public Optional<String> getEntry(String s) {
            return Optional.empty();
        }

        @Override
        public List<String> getEntries(Iterable<String> iterable) {
            return StreamSupport.stream(iterable.spliterator(), false)
                    .map(StoreStreamerIT::transformString)
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, String> getEntryMap(Iterable<String> iterable) {
            return null;
        }

        @Override
        public void saveEntry(String s) {

        }
    }
}