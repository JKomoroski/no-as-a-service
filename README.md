# ❌ No-as-a-Service (Java 21)

This is a Java 21 implementation of the No-as-a-Service API. It provides a lightweight API that returns random, creative rejection messages.

## Features

- Uses Java 21's embedded HTTP server with virtual threads
- No external dependencies
- Same API endpoint as the original Node.js version
- Rate limiting (120 requests per minute per IP)
- Supports Cloudflare IP headers

## Requirements

- Java 21 or higher
- Maven

## Building the Application

```bash
cd java
mvn clean package
```

This will create a jar file in the `target` directory.

## Running the Application

```bash
java -jar target/no-as-a-service-1.0.0.jar
```

By default, the server will run on port 8080. You can change this by setting the `PORT` environment variable:

```bash
PORT=5000 java -jar target/no-as-a-service-1.0.0.jar
```

## API Usage

**Base URL**
```
http://localhost:8080/no
```

**Method:** `GET`  
**Rate Limit:** `120 requests per minute per IP`

### Example Request
```http
GET /no
```

### Example Response
```json
{
  "reason": "This feels like something Future Me would yell at Present Me for agreeing to."
}
```

## Implementation Details

- Uses the Java 21 HTTP server with virtual threads for efficient request handling
- Implements rate limiting using a ConcurrentHashMap and scheduled executor
- Supports Cloudflare's `CF-Connecting-IP` header for accurate client IP detection
