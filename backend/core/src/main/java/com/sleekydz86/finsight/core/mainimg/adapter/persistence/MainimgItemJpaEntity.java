package com.sleekydz86.finsight.core.mainimg.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "mainimg_item")
@EntityListeners(AuditingEntityListener.class)
public class MainimgItemJpaEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "domain_id", length = 32)
    private String domainId;

    @Column(name = "image_name", nullable = false, length = 200)
    private String imageName;

    @Column(name = "image", length = 500)
    private String image;

    @Column(name = "image_file", length = 500)
    private String imageFile;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "reflect_yn", nullable = false, length = 1)
    private String reflectYn = "Y";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageFile() {
        return imageFile;
    }

    public void setImageFile(String imageFile) {
        this.imageFile = imageFile;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReflectYn() {
        return reflectYn;
    }

    public void setReflectYn(String reflectYn) {
        this.reflectYn = reflectYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
