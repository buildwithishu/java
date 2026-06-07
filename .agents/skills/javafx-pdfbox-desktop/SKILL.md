---
name: javafx-pdfbox-desktop
description: JavaFX desktop UI and Apache PDFBox certificate guidance for this project.
---

# JavaFX And PDFBox Desktop Guidance

Use this skill for JavaFX screens, desktop packaging, AtlantaFX styling, and certificate PDFs.

## JavaFX

- Build views programmatically unless an explicit FXML decision is made later.
- Keep views thin: collect input, display state, and delegate business rules to controllers.
- Use JavaFX properties or explicit refresh methods for state updates; avoid database calls on the UI thread.
- Use background tasks for slow work such as database queries, dashboard aggregation, and PDF generation.
- Surface validation errors near the affected controls and keep messages user-friendly.
- Keep navigation centralized in `NavigationManager`.
- Apply AtlantaFX once at application startup and keep custom CSS scoped and minimal.

## Desktop Runtime

- The GUI runs locally with `mvn javafx:run`.
- Docker images are for CI builds, test execution, and distributable artifact creation, not interactive GUI runtime.
- Keep a non-JavaFX `Launcher` class for Maven Shade and modular JavaFX launch behavior.

## PDFBox Certificates

- Generate certificates through a utility/service boundary, not directly inside views.
- Use deterministic certificate IDs based on registration data where possible.
- Sanitize file names and constrain output to the configured certificate directory.
- Close every `PDDocument`, stream, and font resource with try-with-resources.
- Write tests for certificate creation that verify a PDF file exists, is non-empty, and contains expected metadata/text where practical.

## UI Quality

- Prioritize clear student/admin workflows over decorative screens.
- Keep forms keyboard-friendly and validation-first.
- Use tables or dense cards for event management where admins need scanning and comparison.
