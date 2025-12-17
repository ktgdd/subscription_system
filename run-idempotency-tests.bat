@echo off
REM Run idempotency tests
call mvn test -Dtest=IdempotencyServiceTest,IdempotencyApiTest

