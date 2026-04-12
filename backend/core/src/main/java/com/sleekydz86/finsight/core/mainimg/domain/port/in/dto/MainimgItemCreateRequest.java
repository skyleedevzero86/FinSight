package com.sleekydz86.finsight.core.mainimg.domain.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MainimgItemCreateRequest {

    @Size(max = 32)
    private String domainId;

    @NotBlank
    @Size(max = 200)
    private String imageName;

    @Size(max = 500)
    private String image;

    @Size(max = 500)
    private String imageFile;

    @Size(max = 1000)
    private String description;

    @Size(max = 1)
    private String reflectYn = "Y";

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
}
