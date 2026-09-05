package com.sleekydz86.finsight.core.mainimg.domain.port.in.dto;

import com.sleekydz86.finsight.core.mainimg.domain.MainimgItem;

import java.time.LocalDateTime;

public class MainimgItemResponse {
    private String id;
    private String domainId;
    private String imageName;
    private String image;
    private String imageFile;
    private String description;
    private String linkUrl;
    private String noticeBegin;
    private String noticeEnd;
    private String reflectYn;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MainimgItemResponse from(MainimgItem d) {
        MainimgItemResponse r = new MainimgItemResponse();
        r.setId(d.getId());
        r.setDomainId(d.getDomainId());
        r.setImageName(d.getImageName());
        r.setImage(d.getImage());
        r.setImageFile(d.getImageFile());
        r.setDescription(d.getDescription());
        r.setLinkUrl(d.getLinkUrl());
        r.setNoticeBegin(d.getNoticeBegin());
        r.setNoticeEnd(d.getNoticeEnd());
        r.setReflectYn(d.getReflectYn());
        r.setSortOrder(d.getSortOrder());
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
