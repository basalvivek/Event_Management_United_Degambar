package com.udjcs.supportive;

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
public class SupportiveOrganizationService {

    private static final String SUBDIR = "supportive";

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final SupportiveOrganizationRepository repository;

    public SupportiveOrganizationService(SupportiveOrganizationRepository repository) {
        this.repository = repository;
    }

    public List<SupportiveOrganization> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public SupportiveOrganization findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supportive organization not found: " + id));
    }

    public void save(SupportiveOrganization org, MultipartFile logoFile) throws IOException {
        if (org.getId() != null) {
            SupportiveOrganization existing = findById(org.getId());
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
        SupportiveOrganization org = findById(id);
        deleteStoredFile(org.getLogoPath());
        repository.deleteById(id);
    }

    private String storeFile(MultipartFile file) throws IOException {
        String filename = "supportive-" + System.currentTimeMillis() + ".jpg";
        Path dir = Paths.get(uploadDir, SUBDIR);
        Files.createDirectories(dir);
        Files.write(dir.resolve(filename), ImageUtil.compressToJpeg(file.getInputStream()));
        return "/uploads/" + SUBDIR + "/" + filename;
    }

    private void deleteStoredFile(String logoPath) {
        if (logoPath == null || logoPath.isBlank()) return;
        try {
            Path file = Paths.get(uploadDir, logoPath.replace("/uploads/", ""));
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
    }
}
