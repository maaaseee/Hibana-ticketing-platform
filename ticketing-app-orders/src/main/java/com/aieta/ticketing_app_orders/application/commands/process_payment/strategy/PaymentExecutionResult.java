package com.aieta.ticketing_app_orders.application.commands.process_payment.strategy;

public record PaymentExecutionResult(
        boolean success,
        String transactionId,
        String providerReference,
        String message
) {
    public static PaymentExecutionResult ofSuccess(String transactionId, String providerReference) {
        return new PaymentExecutionResult(true, transactionId, providerReference, "Payment processed successfully");
    }

    public static PaymentExecutionResult ofFailure(String message) {
        return new PaymentExecutionResult(false, null, null, message);
    }
}
