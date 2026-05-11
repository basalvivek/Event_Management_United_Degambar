package com.udjcs.payment;

import com.udjcs.supportive.SupportiveOrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository repository;
    private final SupportiveOrganizationRepository organizationRepository;

    public PaymentService(PaymentRepository repository,
                          SupportiveOrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }

    public List<Payment> findAll() {
        return repository.findAllWithDetails();
    }

    public Payment findById(Long id) {
        return repository.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    public void save(Payment payment) {
        if (payment.getOrganizationId() != null) {
            payment.setSupportiveOrganization(
                organizationRepository.findById(payment.getOrganizationId())
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + payment.getOrganizationId()))
            );
        }
        repository.save(payment);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
