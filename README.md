# Deliverables Documentation

This document describes the deliverables for the Subscription Management Service.

## 1. Postman Collection

### Location
- `postman/Subscription_API.postman_collection.json` - Main collection file
- `postman/README.md` - Detailed setup and usage instructions

### Features
- Complete API coverage for all endpoints
- Environment variables for easy configuration
- Organized by functional areas (Health, Admin, User, Actuator)
- Ready-to-use request examples

### Usage
1. Import `Subscription_API.postman_collection.json` into Postman
2. Set environment variables:
   - `baseUrl`: http://localhost:8080
   - `adminToken`: JWT token with ADMIN role
   - `userToken`: JWT token with USER role
3. Start making requests!

See `postman/README.md` for detailed instructions.

## 2. Dockerization

### Files
- `Dockerfile` - Multi-stage build for optimized image size
- `docker-compose.yml` - Complete stack with all dependencies
- `.dockerignore` - Excludes unnecessary files from build context

### Dockerfile Features
- Multi-stage build (Maven build stage + JRE runtime stage)
- Non-root user for security
- Health check configuration
- Optimized JVM settings for containers
- Alpine-based runtime for smaller image size

### Docker Compose Services
1. **PostgreSQL** (port 5432)
   - Database initialization with schema.sql
   - Persistent volume for data
   - Health checks

2. **Redis** (port 6379)
   - Persistent data with AOF
   - Health checks

3. **Zookeeper** (port 2181)
   - Required for Kafka

4. **Kafka** (ports 9092, 9093)
   - Auto-create topics enabled
   - Health checks

5. **Subscription App** (port 8080)
   - Depends on all services
   - Environment variables configured
   - Health checks
   - Auto-restart on failure

### Usage

#### Start all services:
```bash
docker-compose up -d
```

#### View logs:
```bash
docker-compose logs -f subscription-app
```

#### Stop all services:
```bash
docker-compose down
```

#### Stop and remove volumes (clean slate):
```bash
docker-compose down -v
```

## 3. Makefile

### Location
- `Makefile` - Comprehensive command shortcuts

### Available Commands

#### Build & Test
- `make build` - Build the application
- `make test` - Run tests
- `make test-coverage` - Run tests with coverage
- `make clean` - Clean build artifacts
- `make compile` - Compile only

#### Docker Operations
- `make docker-build` - Build Docker image
- `make docker-up` - Start all services
- `make docker-down` - Stop all services
- `make docker-restart` - Restart services
- `make docker-logs` - View all logs
- `make docker-logs-app` - View app logs only
- `make docker-clean` - Stop and remove volumes
- `make docker-rebuild` - Rebuild and restart

#### Development
- `make run` - Run locally (requires local services)
- `make dev` - Start Docker services + run app locally
- `make quickstart` - Build and start everything

#### Utilities
- `make health` - Check application health
- `make metrics` - View Prometheus metrics
- `make help` - Show all available commands

### Quick Start Example
```bash
# Build and start everything
make quickstart

# Check health
make health

# View logs
make docker-logs-app

# Stop everything
make docker-down
```

## Environment Configuration

### Docker Compose Environment Variables
The `docker-compose.yml` includes all necessary environment variables. Key ones:

- **Database**: Configured to use PostgreSQL service
- **Redis**: Configured to use Redis service
- **Kafka**: Configured to use Kafka service
- **JWT**: Set your secret key in production
- **Payment Service**: Mock URL (update for production)

### Local Development
For local development without Docker, ensure:
1. PostgreSQL running on localhost:5432
2. Redis running on localhost:6379
3. Kafka running on localhost:9092
4. Update `application.properties` accordingly

## Production Considerations

### Security
- Change JWT secret key in production
- Use environment variables for sensitive data
- Enable HTTPS
- Configure proper firewall rules

### Performance
- Adjust JVM heap settings based on container resources
- Configure connection pool sizes
- Tune Kafka consumer/producer settings
- Set appropriate Redis TTL values

### Monitoring
- Configure Prometheus scraping
- Set up Grafana dashboards
- Configure alerting rules
- Enable distributed tracing

## Troubleshooting

### Application won't start
1. Check service dependencies: `docker-compose ps`
2. Check logs: `make docker-logs`
3. Verify health: `make health`

### Database connection issues
1. Verify PostgreSQL is running: `docker-compose ps postgres`
2. Check connection string in environment variables
3. Verify schema.sql was executed

### Kafka connection issues
1. Wait for Kafka to be healthy: `docker-compose ps kafka`
2. Check Kafka logs: `docker-compose logs kafka`
3. Verify topic auto-creation is enabled

### Port conflicts
If ports are already in use, modify `docker-compose.yml` to use different ports.

