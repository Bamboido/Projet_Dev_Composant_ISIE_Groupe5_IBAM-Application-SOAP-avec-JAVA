package com.example.soap.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SoapMetricsService {
    
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> requestCounters;
    private final ConcurrentHashMap<String, Counter> errorCounters;
    private final ConcurrentHashMap<String, Timer> responseTimers;
    
    // Compteur global
    private final Counter globalRequestCounter;
    private final Counter globalErrorCounter;
    
    public SoapMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounters = new ConcurrentHashMap<>();
        this.errorCounters = new ConcurrentHashMap<>();
        this.responseTimers = new ConcurrentHashMap<>();
        
        // Initialiser les compteurs globaux
        this.globalRequestCounter = Counter.builder("soap_requests_total")
            .description("Total number of SOAP requests")
            .register(meterRegistry);
            
        this.globalErrorCounter = Counter.builder("soap_errors_total")
            .description("Total number of SOAP errors")
            .register(meterRegistry);
    }
    
    // Méthodes pour les métriques par méthode
    public void incrementRequest(String methodName) {
        globalRequestCounter.increment();
        
        // Compteur par méthode
        Counter methodCounter = requestCounters.computeIfAbsent(methodName, key ->
            Counter.builder("soap_requests_by_method_total")
                .tag("method", key)
                .description("Number of SOAP calls by method")
                .register(meterRegistry)
        );
        methodCounter.increment();
    }
    
    public void incrementError(String methodName, String errorType) {
        globalErrorCounter.increment();
        
        String key = methodName + "_" + errorType;
        Counter errorCounter = errorCounters.computeIfAbsent(key, k ->
            Counter.builder("soap_errors_by_method_total")
                .tag("method", methodName)
                .tag("error_type", errorType)
                .description("Number of SOAP errors by method")
                .register(meterRegistry)
        );
        errorCounter.increment();
    }
    
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopTimer(Timer.Sample sample, String methodName) {
        if (sample != null) {
            Timer timer = getOrCreateTimer(methodName);
            sample.stop(timer);
        }
    }
    
    // Méthode utilitaire pour obtenir ou créer un Timer
    private Timer getOrCreateTimer(String methodName) {
        return responseTimers.computeIfAbsent(methodName, key ->
            Timer.builder("soap_response_time_seconds")
                .tag("method", key)
                .description("SOAP method response time in seconds")
                .publishPercentileHistogram(true)
                .register(meterRegistry)
        );
    }
    
    // Méthode utilitaire pour enregistrer une requête complète
    public void recordRequest(String methodName, long startTime, boolean success) {
        // NE PAS appeler incrementRequest ici car il est déjà appelé dans l'Aspect
        
        if (!success) {
            incrementError(methodName, "runtime_error");
        }
        
        long duration = System.currentTimeMillis() - startTime;
        Timer timer = getOrCreateTimer(methodName);
        timer.record(duration, TimeUnit.MILLISECONDS);
    }
}