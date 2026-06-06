.PHONY: help dev-up dev-down dev-restart db-reset app-run app-build app-jar clean test verify screenshots docker-build logs pgadmin

help: ## Show available commands
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z0-9_-]+:.*##/ {printf "%-18s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

dev-up: ## Start PostgreSQL and pgAdmin
	docker compose up -d

dev-down: ## Stop all Docker services
	docker compose down

dev-restart: ## Restart Docker services
	docker compose down
	docker compose up -d

db-reset: ## Reset database volumes and recreate seed data
	docker compose down -v
	docker compose up -d postgres

app-run: ## Run the JavaFX application locally
	./mvnw javafx:run

app-build: ## Build the JAR package
	./mvnw clean package

app-jar: ## Build fat JAR for distribution
	./mvnw clean package -DskipTests

clean: ## Clean build artifacts
	./mvnw clean

test: ## Run unit tests
	@if [ -z "$$DISPLAY" ] && command -v xvfb-run >/dev/null 2>&1; then \
		xvfb-run -a ./mvnw test; \
	else \
		./mvnw test; \
	fi

verify: ## Run tests, coverage gate, and packaging
	@if [ -z "$$DISPLAY" ] && command -v xvfb-run >/dev/null 2>&1; then \
		xvfb-run -a ./mvnw verify -P headless; \
	else \
		./mvnw verify -P headless; \
	fi

screenshots: ## Regenerate README screenshots from JavaFX views
	./mvnw -Dtest=DocumentationScreenshotTest -DgenerateDocsScreenshots=true test

docker-build: ## Build the Docker image
	docker build -t college-event-portal:local .

logs: ## Show PostgreSQL logs
	docker compose logs -f postgres

pgadmin: ## Print pgAdmin URL
	@echo "pgAdmin: http://localhost:5050"
