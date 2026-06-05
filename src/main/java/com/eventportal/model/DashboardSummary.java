package com.eventportal.model;

import java.util.List;

public record DashboardSummary(
        int totalEvents,
        int totalRegistrations,
        int activeStudents,
        List<CategoryStat> categoryDistribution,
        List<PopularEventStat> popularEvents
) {
    public DashboardSummary {
        if (totalEvents < 0 || totalRegistrations < 0 || activeStudents < 0) {
            throw new IllegalArgumentException("dashboard counts cannot be negative");
        }
        categoryDistribution = List.copyOf(categoryDistribution);
        popularEvents = List.copyOf(popularEvents);
    }
}
