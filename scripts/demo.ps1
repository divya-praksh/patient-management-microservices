# Walks through every hospital workflow via the API gateway.
# Start the stack first:  docker compose up --build -d   (wait about a minute for the services to start)
param([string]$Gateway = "http://localhost:8080")

$ErrorActionPreference = "Stop"
function Step($text) { Write-Host "`n== $text" -ForegroundColor Cyan }
function Call($method, $path, $body) {
    $params = @{ Method = $method; Uri = "$Gateway$path"; ContentType = "application/json" }
    if ($body) { $params.Body = ($body | ConvertTo-Json) }
    Invoke-RestMethod @params
}

$suffix = Get-Random -Maximum 99999
$slot1 = (Get-Date).AddDays(3).ToString("yyyy-MM-ddT10:00:00")
$slot2 = (Get-Date).AddDays(4).ToString("yyyy-MM-ddT11:30:00")

Step "WF-1 Register patient"
$patient = Call POST "/api/patients" @{
    firstName = "Asha"; lastName = "Menon"; email = "asha$suffix@example.com"
    phone = "+91-9876543210"; dateOfBirth = "1990-04-12"; gender = "FEMALE"
}
$patient | Format-List

Step "WF-2 Book a cardiology appointment (billing creates the invoice from the event)"
$appt = Call POST "/api/appointments" @{
    patientId = $patient.id; doctorName = "Dr. Rao $suffix"; department = "CARDIOLOGY"; scheduledAt = $slot1
}
$appt | Format-List

Step "WF-2 Double booking the same doctor and slot is rejected (expect 409)"
try {
    Call POST "/api/appointments" @{ patientId = $patient.id; doctorName = "Dr. Rao $suffix"; department = "CARDIOLOGY"; scheduledAt = $slot1 }
} catch {
    # Windows PowerShell 5.1 leaves ErrorDetails empty for application/problem+json, so read the body directly.
    $status = [int]$_.Exception.Response.StatusCode
    $problem = (New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd() | ConvertFrom-Json
    Write-Host "Rejected with $status $($problem.title): $($problem.detail)" -ForegroundColor Yellow
}

Start-Sleep -Seconds 2
Step "WF-5 Pay the invoice"
$invoice = Call GET "/api/invoices/by-appointment/$($appt.id)"
Call POST "/api/invoices/$($invoice.id)/pay" | Format-List

Step "WF-4 Complete the consultation"
Call POST "/api/appointments/$($appt.id)/complete" | Format-List

Step "WF-3 Book a second visit, then cancel it (pending invoice becomes VOID)"
$appt2 = Call POST "/api/appointments" @{
    patientId = $patient.id; doctorName = "Dr. Iyer $suffix"; department = "NEUROLOGY"; scheduledAt = $slot2
}
Start-Sleep -Seconds 2
Call POST "/api/appointments/$($appt2.id)/cancel" @{ reason = "Patient request" } | Format-List

Start-Sleep -Seconds 2
Step "Invoices for the patient"
Call GET "/api/invoices?patientId=$($patient.id)" | Format-Table id, appointmentId, department, amount, status

Step "Notifications sent to the patient (one per event, never duplicated)"
Call GET "/api/notifications?patientId=$($patient.id)" | Format-Table eventType, message -Wrap
