# 2. REST API Contracts

All requests go through the gateway at `http://localhost:8080`. Each service also serves
live OpenAPI docs at `http://localhost:<port>/swagger-ui.html` (spec: `/v3/api-docs`).

## 2.1 Error format (all services)

RFC 7807 Problem Details, `Content-Type: application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Doctor Dr. Rao already has an appointment at 2026-12-01T10:00",
  "instance": "/api/appointments"
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failed (`errors` map lists field → message) |
| 404 | Resource not found |
| 409 | Business rule / state transition violated |
| 422 | Referenced entity does not exist (e.g. unknown patient) |
| 503 | A downstream service needed for the request is unavailable |

## 2.2 patient-service

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/api/patients` | `PatientRequest` | `201` + `PatientResponse`, `Location` header |
| GET | `/api/patients/{id}` | – | `200` `PatientResponse` |
| GET | `/api/patients` | – | `200` `PatientResponse[]` |
| PUT | `/api/patients/{id}` | `PatientRequest` | `200` `PatientResponse` |

```json
// PatientRequest
{ "firstName": "Asha", "lastName": "Menon", "email": "asha@example.com",
  "phone": "+91-9876543210", "dateOfBirth": "1990-04-12", "gender": "FEMALE" }

// PatientResponse
{ "id": 1, "firstName": "Asha", "lastName": "Menon", "email": "asha@example.com",
  "phone": "+91-9876543210", "dateOfBirth": "1990-04-12", "gender": "FEMALE",
  "registeredAt": "2026-11-23T09:15:00Z" }
```

## 2.3 appointment-service

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/api/appointments` | `AppointmentRequest` | `201` `AppointmentResponse` |
| GET | `/api/appointments/{id}` | – | `200` |
| GET | `/api/appointments?patientId={id}` | – | `200` list |
| POST | `/api/appointments/{id}/cancel` | `{ "reason": "..." }` | `200` |
| POST | `/api/appointments/{id}/complete` | – | `200` |

```json
// AppointmentRequest
{ "patientId": 1, "doctorName": "Dr. Rao", "department": "CARDIOLOGY",
  "scheduledAt": "2026-12-01T10:00:00" }

// AppointmentResponse
{ "id": 7, "patientId": 1, "doctorName": "Dr. Rao", "department": "CARDIOLOGY",
  "scheduledAt": "2026-12-01T10:00:00", "status": "BOOKED", "cancelReason": null }
```

## 2.4 billing-service

| Method | Path | Success |
|---|---|---|
| GET | `/api/invoices/{id}` | `200` `InvoiceResponse` |
| GET | `/api/invoices?patientId={id}` | `200` list |
| GET | `/api/invoices/by-appointment/{appointmentId}` | `200` |
| POST | `/api/invoices/{id}/pay` | `200` |

Invoices are **never created over REST**; they are created only from the `appointment.completed` event.

```json
// InvoiceResponse
{ "id": 3, "appointmentId": 7, "patientId": 1, "amount": 1500.00,
  "status": "PENDING", "createdAt": "2026-12-01T10:40:00Z", "paidAt": null }
```

## 2.5 notification-service

| Method | Path | Success |
|---|---|---|
| GET | `/api/notifications?patientId={id}` | `200` list, newest first |

```json
{ "id": 12, "patientId": 1, "eventType": "APPOINTMENT_BOOKED", "channel": "EMAIL",
  "message": "Your CARDIOLOGY appointment with Dr. Rao is confirmed for 2026-12-01T10:00.",
  "createdAt": "2026-11-23T09:20:00Z" }
```

## 2.6 Gateway routes

| Path prefix | Routed to |
|---|---|
| `/api/patients/**` | patient-service |
| `/api/appointments/**` | appointment-service |
| `/api/invoices/**` | billing-service |
| `/api/notifications/**` | notification-service |
