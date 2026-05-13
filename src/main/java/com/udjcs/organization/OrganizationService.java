package com.udjcs.organization;

import com.udjcs.common.ImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Transactional
public class OrganizationService {

    private static final String ORG_SUBDIR = "organizations";

    @Value("${app.upload.dir}")
    private String uploadDir;

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
            if (logoFile == null || logoFile.isEmpty()) {
                org.setLogoPath(existing.getLogoPath());
            } else {
                deleteStoredFile(existing.getLogoPath());
                org.setLogoPath(storeFile(logoFile));
            }
        } else {
            if (logoFile != null && !logoFile.isEmpty()) {
                org.setLogoPath(storeFile(logoFile));
            }
        }
        repository.save(org);
    }

    public void deleteById(Long id) {
        Organization org = findById(id);
        deleteStoredFile(org.getLogoPath());
        repository.deleteById(id);
    }

    private String storeFile(MultipartFile file) throws IOException {
        String filename = "org-" + System.currentTimeMillis() + ".jpg";
        Path dir = Paths.get(uploadDir, ORG_SUBDIR);
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), ImageUtil.compressToJpeg(file.getInputStream()));
        return "/uploads/" + ORG_SUBDIR + "/" + filename;
    }

    private void deleteStoredFile(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) return;
        try {
            Path file = Paths.get(uploadDir, logoPath.replace("/uploads/", ""));
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
    }
}
