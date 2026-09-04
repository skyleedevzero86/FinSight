package com.sleekydz86.finsight.core.ulink.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "통합링크 등록 요청")
public class UlinkItemCreateRequest {

    @Schema(description = "사이트·테넌트 구분", example = "finsight")
    @Size(max = 32)
    private String domainId;

    @Schema(
            description = "구역 코드",
            example = "FOOTER_SERVICE",
            allowableValues = {"FOOTER_SERVICE", "FOOTER_POLICY", "FOOTER_SOCIAL"})
    @Size(max = 32)
    private String sectionCode;

    @Schema(description = "같은 구역 내 그룹명 (선택)", example = "서비스")
    @Size(max = 100)
    private String linkGroup;

    @Schema(description = "링크 표시명", example = "뉴스", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200)
    private String linkName;

    @Schema(description = "이동 URL", example = "/news", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 500)
    private String linkUrl;

    @Schema(description = "링크 타겟", example = "_self", allowableValues = {"_blank", "_self"})
    @Size(max = 32)
    private String linkTarget;

    @Schema(description = "비고. FOOTER_SOCIAL이면 아이콘 키(FACEBOOK|INSTAGRAM|YOUTUBE|TWITTER)", example = "FACEBOOK")
    @Size(max = 1000)
    private String description;

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
}
