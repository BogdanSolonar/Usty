package com.planning.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class PhotoSearchServiceTest {

    @Test
    void keywordsDropServiceWords() {
        assertEquals(List.of("ride", "atvs"), PhotoSearchService.keywords("Let's go for a ride on ATVs."));
        assertEquals(List.of("iphone"), PhotoSearchService.keywords("new iPhone"));
        assertEquals(List.of("picnic", "park"), PhotoSearchService.keywords("Picnic in the park"));
    }

    @Test
    void relevanceMatchesWordStarts() {
        List<String> flowers = PhotoSearchService.keywords("flowers");
        assertEquals(1, PhotoSearchService.relevance("rose, rose flower, orange rose", flowers));
        assertEquals(0, PhotoSearchService.relevance("sunset, beach, sea", flowers));
        assertEquals(1, PhotoSearchService.relevance("atv, quad, mud", PhotoSearchService.keywords("ATVs")));
        assertEquals(2, PhotoSearchService.relevance("people, park, picnic", PhotoSearchService.keywords("Picnic in the park")));
    }
}
