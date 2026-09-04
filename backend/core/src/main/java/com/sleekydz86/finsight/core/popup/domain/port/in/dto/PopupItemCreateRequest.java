package com.sleekydz86.finsight.core.popup.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "팝업 등록 요청")
public class PopupItemCreateRequest {

    @Schema(description = "사이트·테넌트 구분", example = "finsight")
    @Size(max = 32)
    private String domainId;

    @Schema(description = "팝업 제목", example = "서비스 점검 안내", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 200)
    private String title;

    @Schema(description = "클릭 시 이동 URL", example = "https://example.com")
    @Size(max = 500)
    private String fileUrl;

    @Schema(description = "링크 타겟", example = "_blank", allowableValues = {"_blank", "_self"})
    @Size(max = 32)
    private String linkTarget;

    @Schema(description = "팝업 이미지 URL/경로", example = "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=600")
    @Size(max = 500)
    private String imgPath;

    @Schema(description = "표시용 파일명")
    @Size(max = 200)
    private String fileName;

    @Schema(description = "세로 위치(px, 선택)")
    private Integer verticalPos;

    @Schema(description = "가로 위치(px, 선택)")
    private Integer widthPos;

    @Schema(description = "세로 크기(px, 선택)", example = "400")
    private Integer verticalSize;

    @Schema(description = "가로 크기(px, 선택)", example = "480")
    private Integer widthSize;

    @Schema(description = "노출 시작일 (yyyy-MM-dd)", example = "2026-09-01")
    @Size(max = 20)
    private String noticeBegin;

    @Schema(description = "노출 종료일 (yyyy-MM-dd)", example = "2026-12-31")
    @Size(max = 20)
    private String noticeEnd;

    @Schema(description = "오늘 하루 보지 않기 버튼 노출 Y/N", example = "Y", allowableValues = {"Y", "N"})
    @Size(max = 1)
    private String stopTodayHide = "N";

    @Schema(description = "게시(노출) 활성 Y/N", example = "Y", allowableValues = {"Y", "N"})
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
