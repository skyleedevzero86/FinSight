package com.sleekydz86.finsight.core.popup.adapter.persistence;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "popup_item")
@EntityListeners(AuditingEntityListener.class)
public class PopupItemJpaEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "domain_id", length = 32)
    private String domainId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "link_target", length = 32)
    private String linkTarget;

    @Column(name = "img_path", length = 500)
    private String imgPath;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "vertical_pos")
    private Integer verticalPos;

    @Column(name = "width_pos")
    private Integer widthPos;

    @Column(name = "vertical_size")
    private Integer verticalSize;

    @Column(name = "width_size")
    private Integer widthSize;

    @Column(name = "notice_begin", length = 20)
    private String noticeBegin;

    @Column(name = "notice_end", length = 20)
    private String noticeEnd;

    @Column(name = "stop_today_hide", nullable = false, length = 1)
    private String stopTodayHide = "N";

    @Column(name = "notice_active", nullable = false, length = 1)
    private String noticeActive = "Y";

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getLinkTarget() {
        return linkTarget;
    }

    public void setLinkTarget(String linkTarget) {
        this.linkTarget = linkTarget;
    }

    public String getImgPath() {
        return imgPath;
    }

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getVerticalPos() {
        return verticalPos;
    }

    public void setVerticalPos(Integer verticalPos) {
        this.verticalPos = verticalPos;
    }

    public Integer getWidthPos() {
        return widthPos;
    }

    public void setWidthPos(Integer widthPos) {
        this.widthPos = widthPos;
    }

    public Integer getVerticalSize() {
        return verticalSize;
    }

    public void setVerticalSize(Integer verticalSize) {
        this.verticalSize = verticalSize;
    }

    public Integer getWidthSize() {
        return widthSize;
    }

    public void setWidthSize(Integer widthSize) {
        this.widthSize = widthSize;
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

    public String getStopTodayHide() {
        return stopTodayHide;
    }

    public void setStopTodayHide(String stopTodayHide) {
        this.stopTodayHide = stopTodayHide;
    }

    public String getNoticeActive() {
        return noticeActive;
    }

    public void setNoticeActive(String noticeActive) {
        this.noticeActive = noticeActive;
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
