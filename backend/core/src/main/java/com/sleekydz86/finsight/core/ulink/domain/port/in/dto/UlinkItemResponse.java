package com.sleekydz86.finsight.core.ulink.domain.port.in.dto;

import com.sleekydz86.finsight.core.ulink.domain.UlinkItem;

import java.time.LocalDateTime;

public class UlinkItemResponse {
    private String id;
    private String domainId;
    private String sectionCode;
    private String linkGroup;
    private String linkName;
    private String linkUrl;
    private String linkTarget;
    private String description;
    private String imgPath;
    private Integer sortOrder;
    private String openYn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UlinkItemResponse from(UlinkItem d) {
        UlinkItemResponse r = new UlinkItemResponse();
        r.setId(d.getId());
        r.setDomainId(d.getDomainId());
        r.setSectionCode(d.getSectionCode());
        r.setLinkGroup(d.getLinkGroup());
        r.setLinkName(d.getLinkName());
        r.setLinkUrl(d.getLinkUrl());
        r.setLinkTarget(d.getLinkTarget());
        r.setDescription(d.getDescription());
        r.setImgPath(d.getImgPath());
        r.setSortOrder(d.getSortOrder());
        r.setOpenYn(d.getOpenYn());
        r.setCreatedAt(d.getCreatedAt());
        r.setUpdatedAt(d.getUpdatedAt());
        return r;
    }

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
