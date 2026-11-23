package com.hospital.appointment;

import com.hospital.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Synchronous check against patient-service. Booking must not proceed for an unverified patient,
 * so failures are surfaced to the caller instead of being retried in the background.
 */
@Component
public class PatientClient {

    private static final Logger log = LoggerFactory.getLogger(PatientClient.class);

    private final RestClient restClient;

    public PatientClient(RestClient.Builder builder,
                         @Value("${clients.patient-service.url}") String baseUrl,
                         @Value("${clients.patient-service.connect-timeout}") Duration connectTimeout,
                         @Value("${clients.patient-service.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restClient = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** @return true if the patient exists, false on 404; throws 503 if patient-service cannot be reached. */
    public boolean patientExists(Long patientId) {
        try {
            restClient.get().uri("/api/patients/{id}", patientId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            log.warn("patient-service unavailable while verifying patient {}: {}", patientId, e.getMessage());
            throw ApiException.unavailable("Patient service is unavailable; try again shortly");
        }
    }
}
