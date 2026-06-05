package com.eventportal.controller;

import com.eventportal.dao.CertificateDAO;
import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.Certificate;
import com.eventportal.model.Event;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Registration;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.AppConfig;
import com.eventportal.util.PDFGenerator;
import com.eventportal.util.SessionManager;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class CertificateController {
    private final CertificateDAO certificateDAO;
    private final RegistrationDAO registrationDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;
    private final NotificationController notificationController;
    private final SessionManager sessionManager;
    private final AppConfig appConfig;

    public CertificateController(
            CertificateDAO certificateDAO,
            RegistrationDAO registrationDAO,
            EventDAO eventDAO,
            UserDAO userDAO,
            NotificationController notificationController,
            SessionManager sessionManager,
            AppConfig appConfig
    ) {
        this.certificateDAO = certificateDAO;
        this.registrationDAO = registrationDAO;
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
        this.notificationController = notificationController;
        this.sessionManager = sessionManager;
        this.appConfig = appConfig;
    }

    public Certificate generateForRegistration(long registrationId) throws SQLException, IOException {
        User user = requireLoggedInUser();
        Registration registration = registrationDAO.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));

        if (user.role() != Role.ADMIN && registration.userId() != user.id()) {
            throw new IllegalStateException("You can only generate certificates for your own registrations");
        }
        if (!registration.isActive()) {
            throw new IllegalArgumentException("Certificate can only be generated for confirmed registrations");
        }

        Event event = eventDAO.findById(registration.eventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + registration.eventId()));
        if (event.status() != EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Certificate can only be generated after the event is completed");
        }
        User certificateOwner = userDAO.findById(registration.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + registration.userId()));
        boolean alreadyGenerated = certificateDAO.findByRegistration(registration.id()).isPresent();
        Path certificatePath = PDFGenerator.generateCertificate(
                certificateOwner,
                event,
                registration,
                Path.of(appConfig.certificatesDirectory())
        );

        Certificate certificate = certificateDAO.saveCertificate(registration.id(), certificatePath.toString());
        if (!alreadyGenerated) {
            notificationController.createCertificateReady(registration.userId(), event.id(), event.title());
        }
        return certificate;
    }

    public List<Certificate> findMyCertificates() throws SQLException {
        User user = requireLoggedInUser();
        return certificateDAO.findByUser(user.id());
    }

    private User requireLoggedInUser() {
        return sessionManager.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Login is required"));
    }
}
