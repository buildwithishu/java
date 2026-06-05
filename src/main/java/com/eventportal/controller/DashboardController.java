package com.eventportal.controller;

import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.CategoryStat;
import com.eventportal.model.DashboardSummary;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.PopularEventStat;
import com.eventportal.model.Role;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DashboardController {
    private static final int POPULAR_EVENT_LIMIT = 5;

    private final EventDAO eventDAO;
    private final RegistrationDAO registrationDAO;
    private final UserDAO userDAO;

    public DashboardController(EventDAO eventDAO, RegistrationDAO registrationDAO, UserDAO userDAO) {
        this.eventDAO = eventDAO;
        this.registrationDAO = registrationDAO;
        this.userDAO = userDAO;
    }

    public DashboardSummary loadAdminSummary() throws SQLException {
        List<Event> events = eventDAO.findAll();
        return new DashboardSummary(
                events.size(),
                registrationDAO.countConfirmed(),
                userDAO.countByRole(Role.STUDENT),
                buildCategoryDistribution(events),
                buildPopularEvents(events)
        );
    }

    private static List<CategoryStat> buildCategoryDistribution(List<Event> events) {
        Map<EventCategory, Long> counts = new EnumMap<>(EventCategory.class);
        for (EventCategory category : EventCategory.values()) {
            counts.put(category, 0L);
        }
        for (Event event : events) {
            counts.put(event.category(), counts.get(event.category()) + 1);
        }
        return counts.entrySet().stream()
                .map(entry -> new CategoryStat(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static List<PopularEventStat> buildPopularEvents(List<Event> events) {
        return events.stream()
                .sorted(Comparator.comparingInt(Event::registeredCount).reversed())
                .limit(POPULAR_EVENT_LIMIT)
                .map(event -> new PopularEventStat(event.id(), event.title(), event.registeredCount(), event.capacity()))
                .toList();
    }
}
