package com.sleekydz86.finsight.core.mainimg.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "메인이미지 등록 요청")
public class MainimgItemCreateRequest {

    @Schema(description = "사이트·테넌트 구분", example = "finsight")
    @Size(max = 32)
    private String domainId;

    @Schema(description = "이미지 표시명 (슬라이더 제목)", example = "finsight 뉴스룸", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200)
    private String imageName;

    @Schema(description = "이미지 URL", example = "https://images.unsplash.com/photo-1611162617474-5b21e879e113?w=1920&h=600&fit=crop")
    @Size(max = 500)
    private String image;

    @Schema(description = "업로드 파일 경로·식별자")
    @Size(max = 500)
    private String imageFile;

    @Schema(description = "부가 설명 (슬라이더 부제)", example = "평일 저녁 7시 50분")
    @Size(max = 1000)
    private String description;

    @Schema(description = "화면 반영 여부 Y/N", example = "Y", allowableValues = {"Y", "N"})
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
