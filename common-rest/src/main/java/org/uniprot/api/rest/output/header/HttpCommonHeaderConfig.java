package org.uniprot.api.rest.output.header;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator;
import org.uniprot.api.rest.request.MutableHttpServletRequest;
import org.uniprot.api.rest.service.ServiceInfoConfig;

/**
 * Defines common HTTP headers which can be imported to any REST module.
 *
 * <p>Created 05/09/18
 *
 * @author Edd
 */
@Configuration
@Import({ServiceInfoConfig.class})
public class HttpCommonHeaderConfig {
    public static final String X_RELEASE = "X-Release";
    static final String ALLOW_ALL_ORIGINS = "*";
    private final ServiceInfoConfig.ServiceInfo serviceInfo;
    private final HttpServletRequestContentTypeMutator requestContentTypeMutator;

    @Autowired
    public HttpCommonHeaderConfig(ServiceInfoConfig.ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
        this.requestContentTypeMutator = new HttpServletRequestContentTypeMutator();
    }

    /**
     * Defines a simple request filter that adds an Access-Control-Allow-Origin header with the
     * value '*' to REST requests.
     *
     * <p>The reason for explicitly providing an all origins value, '*', is that web-caching of
     * requests from one origin interferes with those from another origin, even when the same
     * resource is fetched.
     *
     * @return
     */
    @Bean("oncePerRequestFilter")
    public OncePerRequestFilter oncePerRequestFilter(
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
                requestContentTypeMutator.mutate(mutableRequest, requestMappingHandlerMapping);

                response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, ALLOW_ALL_ORIGINS);
                response.addHeader(
                        ACCESS_CONTROL_EXPOSE_HEADERS, "Link, X-TotalRecords, " + X_RELEASE);
                response.addHeader(X_RELEASE, serviceInfo.getRelease());

                chain.doFilter(mutableRequest, response);
            }
        };
    }
}
