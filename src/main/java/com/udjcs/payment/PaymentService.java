package com.udjcs.payment;

import com.udjcs.member.MemberRepository;
import com.udjcs.supportive.SupportiveOrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository repository;
    private final SupportiveOrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;

    public PaymentService(PaymentRepository repository,
                          SupportiveOrganizationRepository organizationRepository,
                          MemberRepository memberRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
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
        } else {
            payment.setSupportiveOrganization(null);
        }
        if (payment.getMemberId() != null) {
            payment.setMember(
                memberRepository.findById(payment.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("Member not found: " + payment.getMemberId()))
            );
        } else {
            payment.setMember(null);
        }
        repository.save(payment);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
