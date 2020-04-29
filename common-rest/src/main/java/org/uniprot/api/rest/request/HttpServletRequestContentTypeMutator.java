package org.uniprot.api.rest.request;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.uniprot.api.rest.output.UniProtMediaType;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.parseMediaType;
import static org.uniprot.api.rest.output.UniProtMediaType.UNKNOWN_MEDIA_TYPE_VALUE;
import static org.uniprot.core.util.Utils.notNullNotEmpty;
import static org.uniprot.core.util.Utils.nullOrEmpty;

/**
 * A helper class that mutates an {@link HttpServletRequest} based on its values, and if necessary
 * sets the request's content type in the 'Accept' header.
 *
 * <p>Created 03/12/2019
 *
 * @author Edd
 */
@Slf4j
public class HttpServletRequestContentTypeMutator {
    public static final String ERROR_MESSAGE_ATTRIBUTE =
            "org.uniprot.api.rest.request.HttpServletRequestContentTypeMutator.errorMessageAttribute";
    static final String FORMAT = "format";
    static final Map<String, Collection<MediaType>> RESOURCE_PATH_2_MEDIA_TYPES = new HashMap<>();
    static final List<String> RESOURCE_PATH_2_MEDIA_TYPES_KEYS = new ArrayList<>();
    private static final Set<String> VALID_EXTENSIONS =
            UniProtMediaType.ALL_TYPES.stream()
                    .map(UniProtMediaType::getFileExtension)
                    .collect(Collectors.toSet());
    private static final Pattern BROWSER_PATTERN =
            Pattern.compile("Mozilla|AppleWebKit|Edg|OPR|Chrome|Vivaldi");

    private HttpServletRequestContentTypeMutator() {}

    public static void mutate(
            MutableHttpServletRequest request,
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        initResourcePath2MediaTypesMap(requestMappingHandlerMapping);

        Collection<MediaType> validMediaTypes = getValidMediaTypesForPath(request);
        if (validMediaTypes.isEmpty()) {
            return;
        }

        ExtensionValidationResult result = checkExtensionIsKnown(request);

        if (notNullNotEmpty(result.getExtensionUsed())) {
            // an known extension was used
            handleRequestedExtension(
                    request, validMediaTypes, result.getExtensionUsed(), result.isEntryResource());
        } else {
            if (notNullNotEmpty(result.getUnvalidatedRequestFormat())) {
                // format parameter is not known
                handleMediaTypeNotAcceptable(request, result.getUnvalidatedRequestFormat());
            } else {
                String requestedAcceptHeader = request.getHeader(HttpHeaders.ACCEPT);
                String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                // for empty accept headers, or browsers, use JSON
                if (nullOrEmpty(requestedAcceptHeader) || isBrowserAsFarAsWeKnow(userAgent)) {
                    request.addHeader(HttpHeaders.ACCEPT, APPLICATION_JSON_VALUE);
                } else {
                    if (!validMediaTypes.contains(parseMediaType(requestedAcceptHeader))) {
                        // if accept header is not valid for this request path
                        handleMediaTypeNotAcceptable(request, requestedAcceptHeader);
                    }
                }
            }
        }
    }

    static void initResourcePath2MediaTypesMap(
            RequestMappingHandlerMapping requestMappingHandlerMapping) {
        if (RESOURCE_PATH_2_MEDIA_TYPES.isEmpty()) {
            requestMappingHandlerMapping
                    .getHandlerMethods()
                    .keySet()
                    .forEach(
                            mappingInfo ->
                                    mappingInfo
                                            .getPatternsCondition()
                                            // for every resource path
                                            .getPatterns()
                                            .forEach(
                                                    pattern ->
                                                            // .. update map with: resource path ->
                                                            // its valid mediatypes
                                                            RESOURCE_PATH_2_MEDIA_TYPES.put(
                                                                    pattern,
                                                                    mappingInfo
                                                                            .getProducesCondition()
                                                                            .getProducibleMediaTypes())));

            RESOURCE_PATH_2_MEDIA_TYPES_KEYS.addAll(RESOURCE_PATH_2_MEDIA_TYPES.keySet());
            orderKeysSoPathVariablesLast(RESOURCE_PATH_2_MEDIA_TYPES_KEYS);
        }
    }

    /**
     * This method gets all known paths and orders them so that paths containing
     * {@code @PathVariable}s appear last. This is important so that when finding matching paths in
     * {@link HttpServletRequestContentTypeMutator#getMatchingPathPattern(String)} will return
     * preferably a path that has no path (if two match), alternatively it will match the one with a
     * path variable. E.g,. Given [1] /a/b/{c} and [2]/a/b/resource, a request to /a/b/resource will
     * match [2]; on the other hand, a request to /a/b/resource2 would match [1].
     */
    static void orderKeysSoPathVariablesLast(List<String> pathVariables) {
        Comparator<String> stringComparator =
                Comparator.<String>naturalOrder()
                        .thenComparingLong(path -> path.chars().filter(c -> c == '{').count());
        pathVariables.sort(stringComparator);
    }

    private static ExtensionValidationResult checkExtensionIsKnown(
            MutableHttpServletRequest request) {
        ExtensionValidationResult.ExtensionValidationResultBuilder resultBuilder =
                ExtensionValidationResult.builder();

        String requestURL = request.getRequestURL().toString();
        String unvalidatedRequestedFormat = request.getParameter(FORMAT);
        resultBuilder.unvalidatedRequestFormat(unvalidatedRequestedFormat);
        String extensionUsed = null;
        for (String validExtension : VALID_EXTENSIONS) {
            // check endsWith so we can handle tricky cases like, /a/XXX.1.txt for resource /a/XXX.1
            if (requestURL.endsWith("." + validExtension)) {
                resultBuilder.isEntryResource(true);
                extensionUsed = validExtension;
            } else {
                if (notNullNotEmpty(unvalidatedRequestedFormat)
                        && unvalidatedRequestedFormat.equals(validExtension)) {
                    extensionUsed = unvalidatedRequestedFormat;
                }
            }

            if (notNullNotEmpty(extensionUsed)) {
                // extension has been identified, so stop looping
                break;
            }
        }

        resultBuilder.extensionUsed(extensionUsed);
        return resultBuilder.build();
    }

    private static void handleRequestedExtension(
            MutableHttpServletRequest request,
            Collection<MediaType> validMediaTypesForPath,
            String extensionUsed,
            boolean isEntryResource) {

        MediaType mediaTypeForFileExtension =
                UniProtMediaType.getMediaTypeForFileExtension(extensionUsed);
        // user provided extension refers to a valid mediatype for this path
        if (validMediaTypesForPath.contains(mediaTypeForFileExtension)) {
            request.addHeader(HttpHeaders.ACCEPT, mediaTypeForFileExtension.toString());

            if (isEntryResource) {
                setURI(request, extensionUsed);
                setURL(request, extensionUsed);
            }

        } else {
            handleMediaTypeNotAcceptable(request, validMediaTypesForPath, extensionUsed);
        }
    }

    private static Collection<MediaType> getValidMediaTypesForPath(
            MutableHttpServletRequest request) {
        String matchingPath = getMatchingPathPattern(request.getRequestURI());
        return RESOURCE_PATH_2_MEDIA_TYPES.getOrDefault(matchingPath, Collections.emptySet());
    }

    private static void handleMediaTypeNotAcceptable(
            MutableHttpServletRequest request,
            Collection<MediaType> validMediaTypesForPath,
            String extension) {
        String validMediaTypesMessage =
                "Requested media type/format not accepted, '"
                        + extension
                        + "'. Valid media types/formats for this end-point include: "
                        + validMediaTypesForPath.stream()
                                .map(MimeType::toString)
                                .collect(Collectors.joining(", "))
                        + ".";
        request.addHeader(HttpHeaders.ACCEPT, UNKNOWN_MEDIA_TYPE_VALUE);
        request.setAttribute(ERROR_MESSAGE_ATTRIBUTE, validMediaTypesMessage);
    }

    private static void handleMediaTypeNotAcceptable(
            MutableHttpServletRequest request, String requestedFormat) {
        request.addHeader(HttpHeaders.ACCEPT, UNKNOWN_MEDIA_TYPE_VALUE);
        request.setAttribute(
                ERROR_MESSAGE_ATTRIBUTE,
                "Requested media type/format not accepted: '" + requestedFormat + "'.");
    }

    static String getMatchingPathPattern(String requestURI) {
        String[] requestURLParts = requestURI.split("/");
        for (String pathPattern : RESOURCE_PATH_2_MEDIA_TYPES_KEYS) {
            String[] pathPatternParts = pathPattern.split("/");

            if (pathPatternMatchesRequestURL(pathPatternParts, requestURLParts)) {
                return pathPattern;
            }
        }

        return null;
    }

    private static boolean pathPatternMatchesRequestURL(
            String[] pathPatternParts, String[] requestURLParts) {
        if (pathPatternParts.length == requestURLParts.length) {
            for (int i = 0; i < pathPatternParts.length; i++) {
                if (pathPatternParts[i].startsWith("{")
                        || pathPatternParts[i].equals(requestURLParts[i])) {
                    if (i == pathPatternParts.length - 1) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private static boolean isBrowserAsFarAsWeKnow(String userAgent) {
        return notNullNotEmpty(userAgent) && BROWSER_PATTERN.matcher(userAgent).find();
    }

    private static void setURL(MutableHttpServletRequest request, String extension) {
        request.setRequestURL(
                request.getRequestURL()
                        .substring(0, request.getRequestURL().length() - (extension.length() + 1)));
    }

    private static void setURI(MutableHttpServletRequest request, String extension) {
        request.setRequestURI(
                request.getRequestURI()
                        .substring(0, request.getRequestURI().length() - (extension.length() + 1)));
    }

    @Getter
    @Builder
    private static class ExtensionValidationResult {
        private final boolean isEntryResource;
        private final String extensionUsed;
        private final String unvalidatedRequestFormat;
    }
}
