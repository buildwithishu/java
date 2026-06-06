package com.eventportal.controller;

import com.eventportal.dao.CertificateDAO;
import com.eventportal.dao.EventDAO;
import com.eventportal.dao.RegistrationDAO;
import com.eventportal.dao.UserDAO;
import com.eventportal.model.Certificate;
import com.eventportal.model.Event;
import com.eventportal.model.EventCategory;
import com.eventportal.model.EventStatus;
import com.eventportal.model.Registration;
import com.eventportal.model.RegistrationStatus;
import com.eventportal.model.Role;
import com.eventportal.model.User;
import com.eventportal.util.SessionManager;
import com.eventportal.util.TestAppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateControllerTest {
    @Mock
    private CertificateDAO certificateDAO;
    @Mock
    private RegistrationDAO registrationDAO;
    @Mock
    private EventDAO eventDAO;
    @Mock
    private UserDAO userDAO;
    @Mock
    private NotificationController notificationController;

    @TempDir
    private Path certificateDirectory;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private CertificateController certificateController;

    @BeforeEach
    void setUp() {
        sessionManager.logout();
        certificateController = new CertificateController(
                certificateDAO,
                registrationDAO,
                eventDAO,
                userDAO,
                notificationController,
                sessionManager,
                TestAppConfig.withCertificatesDirectory(certificateDirectory)
        );
    }

    @AfterEach
    void tearDown() {
        sessionManager.logout();
    }

    @Test
    void generateForRegistration_shouldCreatePdfPersistRecordAndNotify() throws Exception {
        User student = student(7);
        Registration registration = registration(21, student.id(), RegistrationStatus.CONFIRMED);
        Event event = event(EventStatus.COMPLETED);
        sessionManager.login(student);
        when(registrationDAO.findById(registration.id())).thenReturn(Optional.of(registration));
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));
        when(userDAO.findById(student.id())).thenReturn(Optional.of(student));
        when(certificateDAO.findByRegistration(registration.id())).thenReturn(Optional.empty());
        when(certificateDAO.saveCertificate(anyLong(), anyString())).thenAnswer(invocation ->
                new Certificate(31, invocation.getArgument(0), invocation.getArgument(1), OffsetDateTime.now())
        );

        Certificate certificate = certificateController.generateForRegistration(registration.id());

        Path generatedPath = Path.of(certificate.certificatePath()).toAbsolutePath().normalize();
        assertThat(generatedPath).startsWith(certificateDirectory.toAbsolutePath().normalize());
        assertThat(Files.readAllBytes(generatedPath)).startsWith("%PDF".getBytes());
        verify(notificationController).createCertificateReady(student.id(), event.id(), event.title());
    }

    @Test
    void generateForRegistration_shouldRejectAnotherStudentsRegistration() throws SQLException {
        User student = student(7);
        Registration registration = registration(21, 99, RegistrationStatus.CONFIRMED);
        sessionManager.login(student);
        when(registrationDAO.findById(registration.id())).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> certificateController.generateForRegistration(registration.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("your own registrations");
        verifyNoInteractions(eventDAO, userDAO, certificateDAO, notificationController);
    }

    @Test
    void generateForRegistration_shouldRequireCompletedEvent() throws SQLException {
        User student = student(7);
        Registration registration = registration(21, student.id(), RegistrationStatus.CONFIRMED);
        Event event = event(EventStatus.UPCOMING);
        sessionManager.login(student);
        when(registrationDAO.findById(registration.id())).thenReturn(Optional.of(registration));
        when(eventDAO.findById(event.id())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> certificateController.generateForRegistration(registration.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after the event is completed");
        verify(certificateDAO, never()).saveCertificate(anyLong(), anyString());
        verifyNoInteractions(userDAO, notificationController);
    }

    @Test
    void resolveCertificatePath_shouldConstrainStoredPathToConfiguredDirectory() throws Exception {
        Path generated = certificateDirectory.resolve("generated.pdf");
        Files.writeString(generated, "certificate");
        Certificate certificate = new Certificate(31, 21, generated.toString(), OffsetDateTime.now());

        assertThat(certificateController.resolveCertificatePath(certificate)).isEqualTo(generated.toRealPath());

        Path outside = certificateDirectory.getParent().resolve("outside.pdf");
        Files.writeString(outside, "outside");
        Certificate escaped = new Certificate(32, 22, outside.toString(), OffsetDateTime.now());
        assertThatThrownBy(() -> certificateController.resolveCertificatePath(escaped))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("outside");
    }

    private static User student(long id) {
        return new User(
                id,
                "student",
                "student@college.edu",
                "hash",
                Role.STUDENT,
                "Student User",
                "Computer Science",
                OffsetDateTime.now()
        );
    }

    private static Registration registration(long id, long userId, RegistrationStatus status) {
        return new Registration(id, userId, 12, OffsetDateTime.now(), "REG-12-7-ABC12345", status);
    }

    private static Event event(EventStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new Event(
                12,
                "Code Sprint",
                "A programming competition.",
                EventCategory.TECHNICAL,
                "Lab Block",
                LocalDate.now().minusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                40,
                10,
                status,
                1,
                now,
                now
        );
    }
}
