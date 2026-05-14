package com.udjcs.member;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
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

    public List<Member> findPending() {
        return repository.findByApprovalStatusOrderByCreatedAtAsc("Pending");
    }

    public long countPending() {
        return repository.countByApprovalStatus("Pending");
    }

    public void approve(Long id) {
        Member member = findById(id);
        member.setApprovalStatus("Approved");
        member.setMembershipType("General");
        repository.save(member);
    }

    public Member updateProfile(Long id, String address, String phone, String email) {
        Member member = findById(id);
        member.setAddress(address);
        member.setPhone(phone);
        member.setEmail(email != null && !email.isBlank() ? email : null);
        return repository.save(member);
    }

    public void savePhoto(Long id, MultipartFile file) throws IOException {
        Member member = findById(id);
        member.setProfilePicture(file.getBytes());
        member.setPhotoMimeType(file.getContentType());
        repository.save(member);
    }

    public void deletePhoto(Long id) {
        Member member = findById(id);
        member.setProfilePicture(null);
        member.setPhotoMimeType(null);
        repository.save(member);
    }
}
