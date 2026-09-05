package com.sleekydz86.finsight.core.mainimg.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "메인이미지 수정 요청")
public class MainimgItemUpdateRequest {

    @Schema(description = "사이트·테넌트 구분", example = "finsight")
    @Size(max = 32)
    private String domainId;

    @Schema(description = "이미지 표시명 (슬라이더 제목)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200)
    private String imageName;

    @Schema(description = "이미지 URL")
    @Size(max = 500)
    private String image;

    @Schema(description = "업로드 파일 경로·식별자")
    @Size(max = 500)
    private String imageFile;

    @Schema(description = "부가 설명 (슬라이더 부제)")
    @Size(max = 1000)
    private String description;

    @Schema(description = "이미지 클릭 시 이동 URL")
    @Size(max = 500)
    private String linkUrl;

    @Schema(description = "노출 시작일 (yyyy-MM-dd)")
    @Size(max = 20)
    private String noticeBegin;

    @Schema(description = "노출 종료일 (yyyy-MM-dd)")
    @Size(max = 20)
    private String noticeEnd;

    @Schema(description = "화면 반영 여부 Y/N", example = "Y", allowableValues = {"Y", "N"})
    @Size(max = 1)
    private String reflectYn = "Y";

    @Schema(description = "표시 순번(작을수록 앞)", example = "1")
    @Min(1)
    private Integer sortOrder;

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

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getNoticeBegin() {
        return noticeBegin;
    }

    public void setNoticeBegin(String noticeBegin) {
        this.noticeBegin = noticeBegin;
    }

    public String getNoticeEnd() {
        return noticeEnd;
    }

    public void setNoticeEnd(String noticeEnd) {
        this.noticeEnd = noticeEnd;
    }

    public String getReflectYn() {
        return reflectYn;
    }

    public void setReflectYn(String reflectYn) {
        this.reflectYn = reflectYn;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
