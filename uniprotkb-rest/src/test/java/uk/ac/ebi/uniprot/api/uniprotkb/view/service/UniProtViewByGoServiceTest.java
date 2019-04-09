package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;

import uk.ac.ebi.uniprot.api.uniprotkb.view.ViewBy;


@RunWith(MockitoJUnitRunner.class)
class UniProtViewByGoServiceTest {
	 @Mock
	 private SolrClient solrClient;
	 

	 private GoService goService;
	 private UniProtViewByGoService service;
	 
	 
	@BeforeEach
	public void setup() {
		solrClient =Mockito.mock(SolrClient.class);
		goService =new GoService(new RestTemplate());

		service = new UniProtViewByGoService( solrClient, "uniprot", goService);
	 }
	
	@Test
	void test() throws IOException, SolrServerException {
		Map<String, Long> counts = new HashMap<>();
		counts.put("GO:0008150", 78l);
		counts.put("GO:0005575", 70l);	
		counts.put("GO:0003674", 73l);	
		MockServiceHelper.mockServiceQueryResponse( solrClient, "go_id", counts);
		List<ViewBy> viewBys = service.get("", "");
		assertEquals(3, viewBys.size());
		ViewBy viewBy1 = MockServiceHelper.createViewBy("GO:0008150", "biological_process", 78l, UniProtViewByGoService.URL_PREFIX +"GO:0008150" , true);
		assertTrue(viewBys.contains(viewBy1));
		ViewBy viewBy2 = MockServiceHelper.createViewBy("GO:0005575", "cellular_component", 70l, UniProtViewByGoService.URL_PREFIX +"GO:0005575" , true);
		assertTrue(viewBys.contains(viewBy2));
		ViewBy viewBy3 = MockServiceHelper.createViewBy("GO:0003674", "molecular_function", 73l, UniProtViewByGoService.URL_PREFIX +"GO:0003674" , true);
		assertTrue(viewBys.contains(viewBy3));
	}
	
	
}
