# 1. Hospital Workflows → Service Requirements

This document maps real outpatient hospital workflows to modular service responsibilities.
Each workflow step lists the **owning service**, the **interaction style** (sync REST or async event)
and the **business rules** that service enforces.

## 1.1 Bounded contexts

| Service | Owns (source of truth) | Port | Database |
|---|---|---|---|
| `patient-service` | Patient demographics, contact details | 8081 | `patients` |
| `appointment-service` | Doctor appointments and their lifecycle | 8082 | `appointments` |
| `billing-service` | Invoices and payment status | 8083 | `billing` |
| `notification-service` | Outbound patient notifications (audit log) | 8084 | `notifications` |
| `api-gateway` | Single entry point and routing (no data) | 8080 | – |

Rule: **every service owns its own database.** No service reads another service's tables;
data crosses boundaries only through the REST APIs and the events in [03-event-contracts.md](03-event-contracts.md).

## 1.2 Workflows

### WF-1 Patient registration (front desk)
| Step | Owner | Style | Rules |
|---|---|---|---|
| Front desk enters patient details | patient-service | `POST /api/patients` | Email unique (`409` on duplicate); DOB must be in the past; name, email, phone required |
| Welcome message sent | notification-service | consumes `patient.registered` | One notification per event (idempotent on `eventId`) |

### WF-2 Appointment booking (front desk / patient portal)
| Step | Owner | Style | Rules |
|---|---|---|---|
| Choose patient, doctor, department, time | appointment-service | `POST /api/appointments` | Time must be in the future |
| Verify the patient exists | appointment-service → patient-service | **sync** `GET /api/patients/{id}` | Unknown patient → `422`; patient-service down → `503` (fail fast, never book for an unverified patient) |
| Prevent double booking | appointment-service | – | A doctor cannot have two `BOOKED` appointments at the same time (`409`) |
| Consultation fee invoiced (OPD fees are billed at booking) | billing-service | consumes `appointment.booked` | Exactly **one** invoice per appointment (unique key on `appointmentId`); fee comes from the department fee schedule |
| Confirmation and invoice notices | notification-service | consumes `appointment.booked`, `invoice.created` | Idempotent |

### WF-3 Cancellation
| Step | Owner | Style | Rules |
|---|---|---|---|
| Cancel with a reason | appointment-service | `POST /api/appointments/{id}/cancel` | Only `BOOKED` → `CANCELLED` (`409` otherwise) |
| Settle the invoice | billing-service | consumes `appointment.cancelled` | `PENDING` → `VOID`; `PAID` → `REFUND_DUE` (money is never silently dropped). If the cancel is processed **before** the booking, a `VOID` placeholder is stored so the late booking cannot create a payable invoice |
| Cancellation notice | notification-service | consumes `appointment.cancelled` | Idempotent |

### WF-4 Consultation completed
| Step | Owner | Style | Rules |
|---|---|---|---|
| Doctor marks visit complete | appointment-service | `POST /api/appointments/{id}/complete` | Only `BOOKED` → `COMPLETED`; completed visits cannot be cancelled |
| Thank-you / follow-up notice | notification-service | consumes `appointment.completed` | Idempotent |

### WF-5 Payment
| Step | Owner | Style | Rules |
|---|---|---|---|
| Cashier records payment | billing-service | `POST /api/invoices/{id}/pay` | Only `PENDING` → `PAID` (`409` for `VOID` or already `PAID`) |
| Receipt sent | notification-service | consumes `invoice.paid` | Idempotent |

## 1.3 State machines

```
Appointment:  BOOKED ──complete──▶ COMPLETED
                 │
                 └────cancel────▶ CANCELLED

Invoice:      PENDING ──pay──▶ PAID ──appointment cancelled──▶ REFUND_DUE
                 │
                 └──appointment cancelled──▶ VOID
```

## 1.4 Department fee schedule (billing-service)

| Department | Consultation fee (INR) |
|---|---|
| GENERAL_MEDICINE | 500.00 |
| PEDIATRICS | 700.00 |
| ORTHOPEDICS | 1200.00 |
| CARDIOLOGY | 1500.00 |
| NEUROLOGY | 1800.00 |

## 1.5 Non-functional requirements

* **Consistency:** events are published only **after** the database transaction commits, so no event ever describes data that was rolled back.
* **Idempotency:** Kafka gives at-least-once delivery; every consumer tolerates duplicates.
* **Ordering:** events are keyed by `patientId` (patient topics) or `appointmentId` (appointment and invoice topics), so events for one entity on **one topic** arrive in order. Order **across topics** (e.g. `booked` vs `cancelled`) is not guaranteed, and consumers must handle that (see WF-3).
* **Error contract:** all services return RFC 7807 `application/problem+json` errors (see [02-api-contracts.md](02-api-contracts.md)).
