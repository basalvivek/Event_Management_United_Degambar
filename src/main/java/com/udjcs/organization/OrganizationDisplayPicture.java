package com.udjcs.organization;

import com.udjcs.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "organization_display_pictures")
public class OrganizationDisplayPicture extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "image_data", columnDefinition = "BYTEA", nullable = false)
    private byte[] imageData;

    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
