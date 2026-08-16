package com.sleekydz86.finsight.core.popup.domain.port.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PopupItemCreateRequest {

    @Size(max = 32)
    private String domainId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String fileUrl;

    @Size(max = 32)
    private String linkTarget;

    @Size(max = 500)
    private String imgPath;

    @Size(max = 200)
    private String fileName;

    private Integer verticalPos;
    private Integer widthPos;
    private Integer verticalSize;
    private Integer widthSize;

    @Size(max = 20)
    private String noticeBegin;

    @Size(max = 20)
    private String noticeEnd;

    @Size(max = 1)
    private String stopTodayHide = "N";

    @Size(max = 1)
    private String noticeActive = "Y";

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
}
