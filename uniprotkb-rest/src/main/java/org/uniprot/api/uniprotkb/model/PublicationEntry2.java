package org.uniprot.api.uniprotkb.model;

import java.util.List;

import lombok.Builder;
import lombok.Data;

import org.uniprot.core.citation.Citation;
import org.uniprot.core.publication.MappedReference;

/** @author Edd */
@Data
@Builder
public class PublicationEntry2 {
    private Citation citation;
    private List<MappedReference> references;
    private Statistics statistics;

    @Data
    @Builder
    public static class Statistics {
        private final long reviewedMappedProteinCount;
        private final long unreviewedMappedProteinCount;
        private final long computationalMappedProteinCount;
        private final long communityMappedProteinCount;
    }
}
