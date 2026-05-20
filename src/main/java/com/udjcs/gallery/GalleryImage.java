package com.udjcs.gallery;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "gallery_images")
public class GalleryImage extends BaseEntity {

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "image_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] imageData;

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }
}
