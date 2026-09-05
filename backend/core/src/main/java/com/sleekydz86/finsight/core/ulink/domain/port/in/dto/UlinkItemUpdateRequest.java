package com.sleekydz86.finsight.core.ulink.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "통합링크 수정 요청")
public class UlinkItemUpdateRequest {

    @Schema(description = "사이트·테넌트 구분", example = "finsight")
    @Size(max = 32)
    private String domainId;

    @Schema(
            description = "표시 유형",
            example = "FOOTER_TEXT",
            allowableValues = {"FOOTER_TEXT", "FOOTER_IMAGE", "FOOTER_SERVICE", "FOOTER_POLICY", "FOOTER_SOCIAL"})
    @Size(max = 32)
    private String sectionCode;

    @Schema(description = "링크 구분. 정책 링크는 POLICY")
    @Size(max = 100)
    private String linkGroup;

    @Schema(description = "링크 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200)
    private String linkName;

    @Schema(description = "이동 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 500)
    private String linkUrl;

    @Schema(description = "링크 타겟", example = "_self")
    @Size(max = 32)
    private String linkTarget;

    @Schema(description = "설명")
    @Size(max = 1000)
    private String description;

    @Schema(description = "푸터 이미지 URL (이미지 유형일 때)")
    @Size(max = 500)
    private String imgPath;

    @Schema(description = "표시 순번(작을수록 앞)", example = "1")
    @Min(1)
    private Integer sortOrder;

    @Schema(description = "오픈 여부 Y/N", example = "Y", allowableValues = {"Y", "N"})
    @Size(max = 1)
    private String openYn = "Y";

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getLinkGroup() {
        return linkGroup;
    }

    public void setLinkGroup(String linkGroup) {
        this.linkGroup = linkGroup;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkTarget() {
        return linkTarget;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getOpenYn() {
        return openYn;
    }

    public void setOpenYn(String openYn) {
        this.openYn = openYn;
    }
}
