package com.udjcs.gallery;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class GalleryService {

    private final GalleryImageRepository repository;

    public GalleryService(GalleryImageRepository repository) {
        this.repository = repository;
    }

    public List<GalleryImage> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public List<Long> findAllIds() {
        return repository.findAllIds();
    }

    public GalleryImage findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Gallery image not found: " + id));
    }

    public static final int MAX_PHOTOS = 25;

    public int count() {
        return (int) repository.count();
    }

    public void upload(MultipartFile file) throws IOException {
        if (repository.count() >= MAX_PHOTOS) {
            throw new IllegalStateException("Gallery is full. Maximum " + MAX_PHOTOS + " photos allowed. Delete some photos first.");
        }
        GalleryImage img = new GalleryImage();
        img.setOriginalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "photo");
        img.setMimeType(file.getContentType() != null ? file.getContentType() : "image/jpeg");
        img.setImageData(file.getBytes());
        repository.save(img);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
