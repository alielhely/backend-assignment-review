# Albert Heijn Backend Technical Assignment

## Assignment

The assignment is to implement a microservice using Spring boot. The expected REST endpoints schema is described below. \
The assignment is rather open-ended and expects you to think of code structure and implementation yourself. \
Follow the requirements below and be ready to support decisions made in implementing this service.

### Endpoints

<table>
<tr>
   <td>Endpoint</td><td>Description</td><td>Request body example</td><td>Response body example</td>
</tr>
<!-- POST /deliveries -->
<tr>
   <td>POST /deliveries</td>
   <td>

Creates a new delivery. <br>`status` is only allowed to be `IN_PROGRESS` or `DELIVERED`. For status `IN_PROGRESS` the `finishedAt` field must be `null`. For status `DELIVERED` the `finishedAt` field must be provided.

   </td>
   <td>

   ```json
   {
      "vehicleId": "AHV-589",
      "address": "Example street 15A",
      "startedAt": "2023-10-09T12:45:34.678Z",
      "status": "IN_PROGRESS"
   }
   ```

   </td>
   <td>

   ```json
   {
      "id": "69201507-0ae4-4c56-ac2d-75fbe27efad8",
      "vehicleId": "AHV-589",
      "address": "Example street 15A",
      "startedAt": "2023-10-09T12:45:34.678Z",
      "finishedAt": null,
      "status": "IN_PROGRESS" 
   }
   ```

   </td>
</tr>

<!-- POST /deliveries/invoice -->
<tr>
   <td>POST /deliveries/invoice</td>
   <td>

Uses third party service (as defined in the [mock api](#mock-api)) to send invoices to customers.

   </td>
   <td>

   ```json
   {
      "deliveryIds": [
         "7167fc04-0625-49fc-98a9-8785a4a32b60"
      ]
   }
   ```

   </td>
   <td>

   ```json
   [
      { 
         "deliveryId": "7167fc04-0625-49fc-98a9-8785a4a32b60",
         "invoiceId": "e891827f-487f-4884-a8c3-77316212b81b"
      }
   ]
   ```

   </td>
</tr>

<!-- GET /deliveries/business-summary -->
<tr>
   <td>GET /deliveries/business-summary</td>
   <td colspan="2">

Business wants a summary of yesterday's deliveries (Amsterdam time).<br>The summary must include how many deliveries were **started**. The summary should also include the average time between delivery start. This means if there are 3 deliveries that started at `01:00`, `03:00` and `09:00` the time between starting deliveries is `2 hours` (01:00-03:00) and `6 hours` (03:00 - 09:00) so the average is `4 hours` or `240 minutes`

   </td>
   <td>

   ```json
   {
      "deliveries": 3,
      "averageMinutesBetweenDeliveryStart": 240
   }
   ```

   </td>
</tr>
</table>

## Mock API
A mock API is exposed on port `8000` which is defined in the [docker-compose file](./docker-compose.yml#L4), this mock API must not be modified. The endpoint exposed in this API is used for the `/deliveries/invoice` task. The mock API exposes the following endpoint.
<!-- POST /v1/invoices -->
<table>
<tr>
   <td>Endpoint</td><td>Request body example</td><td>Response body example</td>
</tr>
<tr>
   <td>POST /v1/invoices</td>
   <td>

   ```json
   {
      "deliveryId": "7167fc04-0625-49fc-98a9-8785a4a32b60",
      "address": "Example street 15A"
   }
   ```

   </td>
   <td>

   ```json
   {
      "id": "e891827f-487f-4884-a8c3-77316212b81b",
      "sent": true
   }
   ```

   </td>
</tr>
</table>



## Requirements
- We do not expect you to spend more than 3 hours on the assignment. You can add items to [**To-do and considerations**](#to-do-and-considerations) for anything that you wanted to do but did not have enough time to complete. In the follow-up interview this assignment will be discussed and you can elaborate/expand on decisions made in the assignment.
- Write the assignment in **Kotlin** if you are proficient with it. It's also possible to write the assignment in **Java**. However, we prefer **Kotlin** as it is our primary programming language.
- Use **Git** and commit often, so we can see the iterations made on the code.
- The above REST endpoints are implemented (also following the requirements in the description).
- The data is stored in a `database`, you can choose what type.
- This is a customer facing application, which means a website will use this data to display it to the user.
- Assume this is **production code** that will run in production at Albert Heijn and will be maintained/modified for a long time. Not all requirements for "production ready" are listed here, we expect you to decide what is necessary and to support your reasoning. Anything you weren't able to implement, think should be implemented, and other considerations should be added in the README under [**To-do and considerations**](#to-do-and-considerations).

## Where to start
- An empty application is already set up. You are expected to add the endpoint implementations yourself.
- [A docker-compose file](./docker-compose.yml) already exists that builds and runs the application. Run this to make the [mock API](#mock-api) and database (that you add yourself) available. You can use the following command `docker-compose up --build`

## To-do and considerations

### Implemented Features

#### Core Functionality
- ✅ All 3 REST endpoints implemented as per specification
- ✅ PostgreSQL database with JPA/Hibernate
- ✅ Proper validation for delivery states (IN_PROGRESS vs DELIVERED)
- ✅ Integration with mock invoice service
- ✅ Business summary calculation with Amsterdam timezone handling
- ✅ Global exception handling with proper HTTP status codes

#### Architecture & Design
- **Layered Architecture**: Controller → Service → Repository pattern
- **Entity/DTO separation**: Clean separation between persistence and API layers
- **Validation**: Using Jakarta Bean Validation annotations
- **Error Handling**: Centralized exception handling with meaningful error responses
- **Transaction Management**: Using @Transactional for data consistency

#### Database
- **Type**: PostgreSQL 15
- **Schema**: Auto-generated from JPA entities
- **Connection**: Configured for both local and Docker environments

### Production Considerations Not Implemented (Due to Time Constraints)

#### High Priority
1. **Comprehensive Testing**
    - Unit tests for services and business logic
    - Integration tests for API endpoints
    - Repository tests with test containers
    - Mock tests for external invoice service

2. **API Documentation**
    - Swagger/OpenAPI specification
    - Request/response examples
    - Error code documentation

3. **Monitoring & Observability**
    - Structured logging (SLF4J with JSON formatting)
    - Application metrics (Micrometer/Actuator)
    - Health checks for database and external services
    - Distributed tracing (Sleuth/Zipkin)

4. **Security**
    - Authentication/Authorization (JWT or OAuth2)
    - API rate limiting
    - Input sanitization
    - SQL injection prevention (already handled by JPA, but should be validated)
    - HTTPS enforcement
5. **Invoice Service Resilience**
**Current Implementation:**
- "Fail-fast" approach - if any delivery ID is invalid or invoice service fails, entire request fails
- Single transaction for all deliveries

**Production Improvements:**
- **Partial Success Pattern**: Process each delivery individually and return detailed results per item
```json
  [
    {"deliveryId": "uuid1", "invoiceId": "invoice-uuid"},
    {"deliveryId": "uuid2", "error": "Delivery not found"},
    {"deliveryId": "uuid3", "error": "Invoice service timeout"}
  ]
```
- **Bulk Database Queries**: Use `findAllById()` instead of N individual queries
- **Async Processing**: Use message queue (Kafka/RabbitMQ) for large batches
   - Decouple invoice processing from HTTP request/response cycle
   - Enable retry logic with dead-letter queues
   - Prevent database connection exhaustion
- **Circuit Breaker Pattern**: Prevent cascading failures when invoice service is down
- **Retry Logic**: Implement exponential backoff for transient failures
- **Idempotency**: Ensure duplicate invoice requests don't create duplicate invoices

#### Medium Priority
5. **Resilience**
    - Circuit breaker for invoice service (Resilience4j)
    - Retry logic with exponential backoff
    - Timeout configurations
    - Graceful degradation

6. **Data Management**
    - Database migrations (Flyway or Liquibase)
    - Proper indexing strategy for performance
    - Archiving strategy for old deliveries
    - Database backup/restore procedures

7. **Performance**
    - Caching layer (Redis) for business summaries
    - Database query optimization
    - Connection pooling configuration
    - Async processing for invoice sending

#### Lower Priority
8. **DevOps & Deployment**
    - CI/CD pipeline configuration
    - Environment-specific configurations
    - Kubernetes deployment manifests
    - Load balancing considerations

9. **Code Quality**
    - Static code analysis (SonarQube)
    - Code coverage requirements (minimum 80%)
    - Linting rules
    - Pre-commit hooks

### Design Decisions

1. **Kotlin over Java**: Implemented in Kotlin as requested, leveraging data classes and null safety
2. **PostgreSQL**: Chosen for ACID compliance and reliability in production environments
3. **RestClient**: Used modern Spring 6 RestClient instead of deprecated RestTemplate
4. **UUID for IDs**: Better for distributed systems and security
5. **Instant for timestamps**: UTC-based, prevents timezone issues
6. **Enum for status**: Type-safe status representation

### Known Limitations

1. **Invoice sending is synchronous**: Should be moved to async/message queue for production
2. **No pagination**: Business summary and delivery list endpoints should support pagination
3. **No request rate limiting**: Could be abused by malicious actors
4. **Hard-coded timezone**: Amsterdam timezone is hard-coded, should be configurable
5. **Basic error messages**: Should be more user-friendly and internationalized

### Running the Application

```bash
# Start all services
docker-compose up --build

# The application will be available at:
# - API: http://localhost:8080
# - Mock Invoice Service: http://localhost:8000
# - Database: localhost:5432
```

## Sending in the assignment
- We expect a docker compose file that we can run with `docker-compose up` which should start up a functional application at port 8080 (including dependencies like a database).
- Create a pull request with your changes. Notify us via e-mail when the assignment is ready for review.

Thank you for your interest and time invested into making this assignment.
