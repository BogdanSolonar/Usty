package com.planning.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.planning.backend.dto.stats.StatsResponse;
import com.planning.backend.service.stats.StatsService;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    // GET /api/stats -> бали обох з моєї пари, хто попереду і МОЯ мотивуюча підказка
    @GetMapping("/api/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }
}
