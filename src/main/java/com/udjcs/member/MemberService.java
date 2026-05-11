package com.udjcs.member;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class MemberService {

    private final MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }

    public List<Member> findAll() {
        return repository.findAll(Sort.by(Sort.Order.asc("lastName"), Sort.Order.asc("firstName")));
    }

    public Member findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
    }

    public void save(Member member) {
        repository.save(member);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
