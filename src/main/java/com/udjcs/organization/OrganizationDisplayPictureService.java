package com.udjcs.organization;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class OrganizationDisplayPictureService {

    private final OrganizationDisplayPictureRepository repository;

    public OrganizationDisplayPictureService(OrganizationDisplayPictureRepository repository) {
        this.repository = repository;
    }

    public List<OrganizationDisplayPicture> findByOrganization(Long orgId) {
        return repository.findByOrganizationIdOrderByDisplayOrderAsc(orgId);
    }

    public long countByOrganization(Long orgId) {
        return repository.countByOrganizationId(orgId);
    }

    public OrganizationDisplayPicture findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Display picture not found: " + id));
    }

    public void upload(Long orgId, MultipartFile file) throws IOException {
        long count = countByOrganization(orgId);
        if (count >= 5) throw new IllegalArgumentException("Maximum 5 display pictures allowed.");
        OrganizationDisplayPicture pic = new OrganizationDisplayPicture();
        pic.setOrganizationId(orgId);
        pic.setImageData(file.getBytes());
        pic.setMimeType(file.getContentType());
        pic.setDisplayOrder((int) count);
        repository.save(pic);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
