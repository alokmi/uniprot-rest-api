package uk.ac.ebi.uniprot.api.rest.respository;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.solr.core.SolrTemplate;

import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Configure spring-data-solr repository beans, that are used to retrieve data from a solr instance.
 *
 * @author lgonzales
 */
@Configuration
public class RepositoryConfig {

    @Bean
    public RepositoryConfigProperties repositoryConfigProperties() {
        return new RepositoryConfigProperties();
    }

    @Bean
    @Profile("live")
    public HttpClient httpClient(RepositoryConfigProperties config) {
        // I am creating HttpClient exactly in the same way it is created inside CloudSolrClient.Builder,
        // but here I am just adding Credentials
        ModifiableSolrParams param = null;
        if (!config.getUsername().isEmpty() && !config.getPassword().isEmpty()) {
            param = new ModifiableSolrParams();
            param.add(HttpClientUtil.PROP_BASIC_AUTH_USER, config.getUsername());
            param.add(HttpClientUtil.PROP_BASIC_AUTH_PASS, config.getPassword());
        }
        return HttpClientUtil.createClient(param);
    }

    @Bean
    @Profile("live")
    public SolrClient uniProtSolrClient(HttpClient httpClient, RepositoryConfigProperties config) {
        String zookeeperhost = config.getZkHost();
        if (zookeeperhost != null && !zookeeperhost.isEmpty()) {
            String[] zookeeperHosts = zookeeperhost.split(",");
            return new CloudSolrClient.Builder(asList(zookeeperHosts), Optional.empty())
                    .withHttpClient(httpClient)
                    .withConnectionTimeout(config.getConnectionTimeout())
                    .withSocketTimeout(config.getSocketTimeout())
                    .build();
        } else if (!config.getHttphost().isEmpty()) {
            return new HttpSolrClient.Builder().withHttpClient(httpClient).withBaseSolrUrl(config.getHttphost())
                    .build();
        } else {
            throw new BeanCreationException("make sure your application.properties has eight solr zookeeperhost or httphost properties");
        }
    }

    @Bean
    public SolrTemplate solrTemplate(SolrClient uniProtSolrClient) {
        return new SolrTemplate(uniProtSolrClient);
    }
}