.PHONY: help dev-up dev-down dev-restart db-reset app-run app-build app-jar clean test docker-build logs pgadmin

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
	./mvnw test

docker-build: ## Build the Docker image
	docker build -t college-event-portal:local .

logs: ## Show PostgreSQL logs
	docker compose logs -f postgres

pgadmin: ## Print pgAdmin URL
	@echo "pgAdmin: http://localhost:5050"
