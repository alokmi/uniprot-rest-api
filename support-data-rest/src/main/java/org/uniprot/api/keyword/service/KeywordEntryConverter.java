package org.uniprot.api.keyword.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

import org.uniprot.core.cv.keyword.KeywordEntry;
import org.uniprot.core.json.parser.keyword.KeywordJsonConfig;
import org.uniprot.store.search.document.keyword.KeywordDocument;

/**
 * @author lgonzales
 */
@Slf4j
public class KeywordEntryConverter implements Function<KeywordDocument, KeywordEntry> {

    private final ObjectMapper objectMapper;

    public KeywordEntryConverter() {
        objectMapper = KeywordJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public KeywordEntry apply(KeywordDocument keywordDocument) {
        try {
            return objectMapper.readValue(keywordDocument.getKeywordObj().array(), KeywordEntry.class);
        } catch (Exception e) {
            log.info("Error converting solr binary to KeywordEntry: ", e);
        }
        return null;
    }

}