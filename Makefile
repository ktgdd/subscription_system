.PHONY: help build test clean run docker-build docker-up docker-down docker-logs docker-restart

# Variables
APP_NAME := subscription
DOCKER_COMPOSE := docker-compose
MAVEN := mvn

# Colors for output
GREEN := \033[0;32m
YELLOW := \033[0;33m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "$(GREEN)Available targets:$(NC)"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(YELLOW)%-20s$(NC) %s\n", $$1, $$2}'

# Maven Commands
build: ## Build the application
	@echo "$(GREEN)Building application...$(NC)"
	$(MAVEN) clean package -DskipTests

test: ## Run tests
	@echo "$(GREEN)Running tests...$(NC)"
	$(MAVEN) test

test-coverage: ## Run tests with coverage
	@echo "$(GREEN)Running tests with coverage...$(NC)"
	$(MAVEN) test jacoco:report

clean: ## Clean build artifacts
	@echo "$(GREEN)Cleaning...$(NC)"
	$(MAVEN) clean
	rm -rf target/

compile: ## Compile the application
	@echo "$(GREEN)Compiling...$(NC)"
	$(MAVEN) compile

# Docker Commands
docker-build: ## Build Docker image
	@echo "$(GREEN)Building Docker image...$(NC)"
	docker build -t $(APP_NAME):latest .

docker-up: ## Start all services with Docker Compose
	@echo "$(GREEN)Starting services...$(NC)"
	$(DOCKER_COMPOSE) up -d
	@echo "$(GREEN)Services started. Waiting for health checks...$(NC)"
	@echo "$(YELLOW)Application will be available at http://localhost:8080$(NC)"

docker-down: ## Stop all services
	@echo "$(GREEN)Stopping services...$(NC)"
	$(DOCKER_COMPOSE) down

docker-restart: ## Restart all services
	@echo "$(GREEN)Restarting services...$(NC)"
	$(DOCKER_COMPOSE) restart

docker-logs: ## Show logs from all services
	$(DOCKER_COMPOSE) logs -f

docker-logs-app: ## Show logs from application only
	$(DOCKER_COMPOSE) logs -f subscription-app

docker-clean: ## Stop services and remove volumes
	@echo "$(GREEN)Stopping services and removing volumes...$(NC)"
	$(DOCKER_COMPOSE) down -v
	docker system prune -f

docker-rebuild: ## Rebuild and restart services
	@echo "$(GREEN)Rebuilding and restarting services...$(NC)"
	$(DOCKER_COMPOSE) up -d --build

# Database Commands
db-migrate: ## Run database migrations (if using Flyway/Liquibase)
	@echo "$(GREEN)Running database migrations...$(NC)"
	$(MAVEN) flyway:migrate

db-reset: ## Reset database (WARNING: Deletes all data)
	@echo "$(YELLOW)WARNING: This will delete all data!$(NC)"
	$(DOCKER_COMPOSE) down -v postgres
	$(DOCKER_COMPOSE) up -d postgres
	@sleep 5
	@echo "$(GREEN)Database reset complete$(NC)"

# Development Commands
run: ## Run the application locally (requires local PostgreSQL, Redis, Kafka)
	@echo "$(GREEN)Running application locally...$(NC)"
	$(MAVEN) spring-boot:run

dev: docker-up ## Start development environment (Docker services + local app)
	@echo "$(GREEN)Starting development environment...$(NC)"
	@echo "$(YELLOW)Starting Docker services...$(NC)"
	@make docker-up
	@sleep 10
	@echo "$(YELLOW)Starting local application...$(NC)"
	@make run

# Utility Commands
check: ## Check code quality
	@echo "$(GREEN)Running code quality checks...$(NC)"
	$(MAVEN) checkstyle:check spotbugs:check

format: ## Format code
	@echo "$(GREEN)Formatting code...$(NC)"
	$(MAVEN) formatter:format

# Health Checks
health: ## Check application health
	@echo "$(GREEN)Checking application health...$(NC)"
	@curl -s http://localhost:8080/actuator/health | jq . || echo "Application not running or jq not installed"

health-simple: ## Simple health check
	@curl -s http://localhost:8080/health || echo "Application not running"

# Monitoring
metrics: ## View Prometheus metrics
	@echo "$(GREEN)Fetching metrics...$(NC)"
	@curl -s http://localhost:8080/actuator/prometheus | head -50

# Quick Start
quickstart: docker-build docker-up ## Quick start: build and run everything
	@echo "$(GREEN)Quick start complete!$(NC)"
	@echo "$(YELLOW)Application: http://localhost:8080$(NC)"
	@echo "$(YELLOW)Health: http://localhost:8080/actuator/health$(NC)"
	@echo "$(YELLOW)Metrics: http://localhost:8080/actuator/prometheus$(NC)"

# Default target
.DEFAULT_GOAL := help

