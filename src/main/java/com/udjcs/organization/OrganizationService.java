package com.udjcs.organization;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    public List<Organization> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Organization findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
    }

    public void save(Organization org, MultipartFile logoFile) throws IOException {
        if (org.getId() != null) {
            Organization existing = findById(org.getId());
            org.setBannerImage(existing.getBannerImage());
            org.setBannerMimeType(existing.getBannerMimeType());
            if (logoFile == null || logoFile.isEmpty()) {
                org.setLogoData(existing.getLogoData());
                org.setLogoMimeType(existing.getLogoMimeType());
            } else {
                org.setLogoData(logoFile.getBytes());
                org.setLogoMimeType(logoFile.getContentType());
            }
        } else {
            if (logoFile != null && !logoFile.isEmpty()) {
                org.setLogoData(logoFile.getBytes());
                org.setLogoMimeType(logoFile.getContentType());
            }
        }
        repository.save(org);
    }

    public void saveBanner(Long orgId, MultipartFile bannerFile) throws IOException {
        Organization org = findById(orgId);
        org.setBannerImage(bannerFile.getBytes());
        org.setBannerMimeType(bannerFile.getContentType());
        repository.save(org);
    }

    public void deleteBanner(Long orgId) {
        Organization org = findById(orgId);
        org.setBannerImage(null);
        org.setBannerMimeType(null);
        repository.save(org);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
