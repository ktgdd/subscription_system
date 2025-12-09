# Docker Setup Verification

## Quick Test Commands

### 1. Validate Configuration
```bash
# Validate docker-compose.yml
docker-compose config

# Check Dockerfile syntax (manual review)
cat Dockerfile
```

### 2. Build Docker Image
```bash
# Build the application image
docker build -t subscription:latest .

# Or use Makefile
make docker-build
```

### 3. Start Services
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f subscription-app
```

### 4. Test Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check all services
docker-compose ps
```

### 5. Clean Up
```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Known Issues Fixed

1. ✅ Removed obsolete `version` field from docker-compose.yml
2. ✅ Added `wget` installation in Dockerfile for health checks
3. ✅ Fixed healthcheck command format in docker-compose.yml

## Service Ports

- **Application**: http://localhost:8080
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Kafka**: localhost:9092
- **Zookeeper**: localhost:2181

## Troubleshooting

### Build fails
- Check Maven dependencies in pom.xml
- Ensure Java 21 is available in build stage
- Check Docker daemon is running

### Services won't start
- Check port conflicts: `lsof -i :8080`
- Check Docker logs: `docker-compose logs`
- Verify Docker has enough resources

### Health checks failing
- Wait for services to fully start (40s start period)
- Check application logs for errors
- Verify database schema was initialized

