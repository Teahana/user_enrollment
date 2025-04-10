package group7.enrollmentSystem.services;

import group7.enrollmentSystem.dtos.classDtos.InvoiceDto;
import group7.enrollmentSystem.dtos.classDtos.PaymentDto;

import java.util.List;

public class FinanceService {
    public List<PaymentDto> getStudentPaymentsByEmail(String email) {
        return null;
    }

    public void updateStudentPayment(Long paymentId, boolean paid) {
        String out = "Successfully updated student payment";
    }

    public List<InvoiceDto> getAllInvoices() {
        return null;
    }

    public InvoiceDto getStudentInvoiceByEmail(String email) {
        return null;
    }
}
