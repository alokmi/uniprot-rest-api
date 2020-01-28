package org.uniprot.api.uniprotkb.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.uniprotkb.controller.request.PublicationRequest;
import org.uniprot.api.uniprotkb.model.PublicationEntry;
import org.uniprot.core.literature.LiteratureEntry;
import org.uniprot.core.literature.LiteratureStatistics;
import org.uniprot.core.literature.builder.LiteratureEntryBuilder;
import org.uniprot.core.literature.builder.LiteratureStatisticsBuilder;

/**
 * @author lgonzales
 * @since 2019-12-17
 */
class PublicationFacetConfigTest {

    @Test
    void getFacetsWithNullInformation() {
        List<Facet> result = PublicationFacetConfig.getFacets(null, null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFacetsWithEmptyInformation() {
        List<Facet> result = PublicationFacetConfig.getFacets(Collections.emptyList(), "");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFacetsForCategory() {
        List<PublicationEntry> publications = getCategoryPublicationEntries();

        List<Facet> result = PublicationFacetConfig.getFacets(publications, "category");
        assertNotNull(result);
        assertEquals(1, result.size());
        Facet category = result.get(0);
        assertEquals("category", category.getName());
        assertEquals("Category", category.getLabel());
        assertNotNull(category.getValues());

        List<FacetItem> items = category.getValues();
        assertEquals(3, items.size());

        FacetItem first = items.get(0);
        assertNull(first.getLabel());
        assertEquals("function", first.getValue());
        assertEquals(new Long(2L), first.getCount());

        FacetItem second = items.get(1);
        assertNull(second.getLabel());
        assertEquals("interation", second.getValue());
        assertEquals(new Long(1L), second.getCount());
    }

    @Test
    void applyFacetFiltersForCategories() {
        List<PublicationEntry> publications = getCategoryPublicationEntries();
        PublicationRequest request = new PublicationRequest();
        request.setFacets("category");
        request.setQuery("category:ptm");

        PublicationFacetConfig.applyFacetFilters(publications, request);

        assertNotNull(publications);
        assertEquals(1, publications.size());

        PublicationEntry entry = publications.get(0);
        assertTrue(entry.getCategories().contains("ptm"));
    }

    @Test
    void getFacetsForSource() {
        List<PublicationEntry> publications = getSourcePublicationEntries();

        List<Facet> result = PublicationFacetConfig.getFacets(publications, "source");
        assertNotNull(result);
        assertEquals(1, result.size());
        Facet category = result.get(0);
        assertEquals("source", category.getName());
        assertEquals("Source", category.getLabel());
        assertNotNull(category.getValues());

        List<FacetItem> items = category.getValues();
        assertEquals(2, items.size());

        FacetItem first = items.get(0);
        assertNull(first.getLabel());
        assertEquals("source2", first.getValue());
        assertEquals(new Long(2L), first.getCount());

        FacetItem second = items.get(1);
        assertNull(second.getLabel());
        assertEquals("source1", second.getValue());
        assertEquals(new Long(1L), second.getCount());
    }

    @Test
    void applyFacetFiltersForSource() {
        List<PublicationEntry> publications = getSourcePublicationEntries();
        PublicationRequest request = new PublicationRequest();
        request.setFacets("source");
        request.setQuery("source:source1");

        PublicationFacetConfig.applyFacetFilters(publications, request);

        assertNotNull(publications);
        assertEquals(1, publications.size());

        PublicationEntry entry = publications.get(0);
        assertEquals("source1", entry.getPublicationSource());
    }

    @Test
    void getFacetsForScale() {
        List<PublicationEntry> publications = getScalePublicationEntries();

        List<Facet> result = PublicationFacetConfig.getFacets(publications, "scale");
        assertNotNull(result);
        assertEquals(1, result.size());
        Facet category = result.get(0);
        assertEquals("scale", category.getName());
        assertEquals("Scale", category.getLabel());
        assertNotNull(category.getValues());

        List<FacetItem> items = category.getValues();
        assertEquals(2, items.size());

        FacetItem first = items.get(0);
        assertNull(first.getLabel());
        assertEquals("Small", first.getValue());
        assertEquals(new Long(2L), first.getCount());

        FacetItem second = items.get(1);
        assertNull(second.getLabel());
        assertEquals("Large", second.getValue());
        assertEquals(new Long(1L), second.getCount());
    }

    @Test
    void applyFacetFiltersForScale() {
        List<PublicationEntry> publications = getScalePublicationEntries();
        PublicationRequest request = new PublicationRequest();
        request.setFacets("scale");
        request.setQuery("scale:Small");

        PublicationFacetConfig.applyFacetFilters(publications, request);

        assertNotNull(publications);
        assertEquals(2, publications.size());

        PublicationEntry entry = publications.get(0);
        assertFalse(entry.isLargeScale());
        assertEquals(new Long(50), entry.getLiteratureEntry().getPubmedId());

        entry = publications.get(1);
        assertFalse(entry.isLargeScale());
        assertEquals(new Long(49), entry.getLiteratureEntry().getPubmedId());
    }

    @Test
    void getFacetNames() {
        PublicationFacetConfig publicationFacetConfig = new PublicationFacetConfig();

        List<String> facetNames = new ArrayList<>(publicationFacetConfig.getFacetNames());

        assertNotNull(facetNames);
        assertEquals(3, facetNames.size());
        assertEquals("source", facetNames.get(0));
        assertEquals("category", facetNames.get(1));
        assertEquals("scale", facetNames.get(2));
    }

    @Test
    void getFacetPropertyMap() {
        PublicationFacetConfig publicationFacetConfig = new PublicationFacetConfig();
        assertNull(publicationFacetConfig.getFacetPropertyMap());
    }

    private List<PublicationEntry> getSourcePublicationEntries() {
        List<PublicationEntry> publications = new ArrayList<>();
        publications.add(PublicationEntry.builder().publicationSource("source2").build());
        publications.add(PublicationEntry.builder().publicationSource("source1").build());
        publications.add(PublicationEntry.builder().publicationSource("source2").build());
        return publications;
    }

    private List<PublicationEntry> getCategoryPublicationEntries() {
        List<PublicationEntry> publications = new ArrayList<>();
        publications.add(
                PublicationEntry.builder().categories(Arrays.asList("function", "ptm")).build());
        publications.add(
                PublicationEntry.builder()
                        .categories(Arrays.asList("interation", "function"))
                        .build());
        return publications;
    }

    private List<PublicationEntry> getScalePublicationEntries() {
        List<PublicationEntry> publications = new ArrayList<>();
        LiteratureStatistics largeStat =
                new LiteratureStatisticsBuilder().reviewedProteinCount(51).build();
        LiteratureEntry largeEntry =
                new LiteratureEntryBuilder().pubmedId(51L).statistics(largeStat).build();

        LiteratureStatistics smallStat =
                new LiteratureStatisticsBuilder().reviewedProteinCount(50).build();
        LiteratureEntry smallEntry =
                new LiteratureEntryBuilder().pubmedId(50L).statistics(smallStat).build();
        LiteratureEntry smallEntry2 =
                new LiteratureEntryBuilder().pubmedId(49L).statistics(smallStat).build();

        publications.add(PublicationEntry.builder().literatureEntry(largeEntry).build());
        publications.add(PublicationEntry.builder().literatureEntry(smallEntry).build());
        publications.add(PublicationEntry.builder().literatureEntry(smallEntry2).build());
        return publications;
    }
}