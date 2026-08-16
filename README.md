# KEC BusConnect — Backend (Spring Boot 3 + MongoDB)

> **High-Performance Real-Time Transit Tracking & REST API Service for Kuppam Engineering College (KEC)**  
> Built with Spring Boot 3.2, Java 21, Spring Data MongoDB, and Spring WebSocket STOMP.

---

## 🌟 Core Capabilities

- 🚌 **Bus & Fleet Management:** REST endpoints for bus registration, driver assignments, and active fleet retrieval.
- 📡 **Real-Time STOMP WebSockets:** Broker endpoints on `/ws` with channel broadcasting on `/topic/bus/{busNumber}` and `/topic/buses`.
- 🗺️ **Official Corridor:** Pre-seeded single pilot route (**`Attikuppam → KEC (via MDR87)`**, 16 stops, 39.8 km, 1 hr 18 min).
- 📍 **Nominatim Reverse Geocoding:** Built-in caching proxy service to convert driver/student coordinates into readable address strings.
- 🔐 **JWT Authentication & Security:** Role-based security (`STUDENT`, `DRIVER`, `ADMIN`, `TRACKER`) with bcrypt hashing.
- 👥 **Student Check-In:** Active boarding confirmation endpoint `POST /api/students/board/{busId}`.

---

## 🚀 Quick Start (Local Setup)

### 1. Prerequisites
- **Java 21 JDK**: (Oracle OpenJDK or Eclipse Temurin)
- **MongoDB**: MongoDB Atlas or Local MongoDB 6.0+

### 2. Configuration
Create a `.env` file in the `backend/` directory:
```properties
MONGODB_URI=mongodb://username:password@cluster.mongodb.net/kec_busconnect?ssl=true&replicaSet=atlas-xxx&authSource=admin&retryWrites=true&w=majority
JWT_SECRET=8527D6E77D382B2943A59AF16F4F5A6D63E6F74A6E7134A2973E2F485764B296
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:5174
```

### 3. Build & Run
Using the Maven wrapper:
```bash
# Windows
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw clean compile
./mvnw spring-boot:run
```

---

## 📡 REST API Documentation

### Authentication
- `POST /api/auth/register` — Register a student with roll number, department, and boarding point.
- `POST /api/auth/login` — Authenticate and receive JWT token + user role.
- `GET /api/auth/me` — Retrieve current authenticated user profile.

### Fleet & Routes
- `GET /api/buses` — Retrieve active bus list (`KEC-07`).
- `GET /api/buses/{id}` — Retrieve full bus information and assigned route stops.
- `GET /api/routes` — Retrieve active routes and 16 waypoint coordinates.

### Live GPS & Tracking
- `POST /api/buses/{busId}/location` — Broadcast new driver GPS location (Role: `DRIVER`, `TRACKER`, `ADMIN`).
- `GET /api/buses/{busId}/location` — Get latest GPS position and heading.
- `POST /api/students/board/{busId}` — Check in as boarded on the current active trip.

### Geocoding
- `GET /api/geocoding/reverse?lat={lat}&lng={lng}` — Reverse geocode coordinates to human-readable address.

---

## 🐳 Docker Deployment

### Run with Docker Compose
```bash
docker-compose up --build -d
```

---

## 🔒 Default Seed Credentials

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@kec.ac.in` | `admin123` |
| **Driver** | `driver@kec.ac.in` | `password` |
| **Student** | `student@kec.ac.in` | `password` |

---

## 📄 License
Kuppam Engineering College — KEC BusConnect Platform.
