package org.uniprot.api.uniref.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.UNIREF;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.pagination.PaginatedResultsEvent;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.api.uniref.service.UniRefEntryResult;
import org.uniprot.api.uniref.service.UniRefEntryService;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.util.Utils;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.store.search.field.validator.FieldRegexConstants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author jluo
 * @date: 22 Aug 2019
 */
@RestController
@Validated
@RequestMapping("/uniref")
public class UniRefEntryController extends BasicSearchController<UniRefEntry> {

    private final UniRefEntryService entryService;

    @Autowired
    public UniRefEntryController(
            ApplicationEventPublisher eventPublisher,
            UniRefEntryService entryService,
            MessageConverterContextFactory<UniRefEntry> converterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(eventPublisher, converterContextFactory, downloadTaskExecutor, UNIREF);
        this.entryService = entryService;
    }

    @Tag(
            name = "uniref",
            description =
                    "The UniProt Reference Clusters (UniRef) provide clustered sets of sequences from the UniProt Knowledgebase (including isoforms) and selected UniParc records. This hides redundant sequences and obtains complete coverage of the sequence space at three resolutions: UniRef100, UniRef90 and UniRef50.")
    @GetMapping(
            value = "/{id}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                FASTA_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_XML_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    @Operation(
            summary = "Retrieve an UniRef cluster by id.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UniRefEntry.class)),
                            @Content(
                                    mediaType = APPLICATION_XML_VALUE,
                                    schema = @Schema(implementation = Entry.class)),
                            @Content(mediaType = TSV_MEDIA_TYPE_VALUE),
                            @Content(mediaType = LIST_MEDIA_TYPE_VALUE),
                            @Content(mediaType = XLS_MEDIA_TYPE_VALUE),
                            @Content(mediaType = FASTA_MEDIA_TYPE_VALUE)
                        })
            })
    public ResponseEntity<MessageConverterContext<UniRefEntry>> getById(
            @Parameter(description = "Unique identifier for the UniRef cluster")
                    @PathVariable("id")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.id.value}")
                    String id,
            @Valid @ModelAttribute UniRefIdRequest idRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        UniRefEntryResult entryResult = entryService.getEntity(id, idRequest);
        if (Utils.notNull(entryResult.getPage())) {
            this.eventPublisher.publishEvent(
                    new PaginatedResultsEvent(this, request, response, entryResult.getPage()));
        }
        return super.getEntityResponse(entryResult.getEntry(), idRequest.getFields(), request);
    }

    @Tag(name = "uniref")
    @GetMapping(
            value = "/{id}/facets",
            produces = {APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Retrieve UniRef Entry facets.",
            responses = {
                @ApiResponse(
                        content = {
                            @Content(
                                    mediaType = APPLICATION_JSON_VALUE,
                                    array =
                                            @ArraySchema(
                                                    schema = @Schema(implementation = Facet.class)))
                        })
            })
    public List<Facet> getFacets(
            @Parameter(description = "Unique identifier for the UniRef cluster")
                    @PathVariable("id")
                    @Pattern(
                            regexp = FieldRegexConstants.UNIREF_CLUSTER_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.invalid.id.value}")
                    String id) {
        return entryService.getFacets(id);
    }

    @Override
    protected String getEntityId(UniRefEntry entity) {
        return entity.getId().getValue();
    }

    @Override
    protected Optional<String> getEntityRedirectId(UniRefEntry entity) {
        return Optional.empty();
    }
}
