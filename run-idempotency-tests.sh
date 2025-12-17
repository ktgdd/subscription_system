#!/bin/bash

# Run idempotency tests
mvn test -Dtest=IdempotencyServiceTest,IdempotencyApiTest

