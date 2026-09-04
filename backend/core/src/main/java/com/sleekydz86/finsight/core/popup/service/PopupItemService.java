package com.sleekydz86.finsight.core.popup.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.PopupItemNotFoundException;
import com.sleekydz86.finsight.core.popup.domain.PopupItem;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemCreateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemUpdateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.out.PopupItemPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class PopupItemService {

    private final PopupItemPersistencePort persistencePort;

    public PopupItemService(PopupItemPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public PaginationResponse<PopupItemResponse> list(
            String domainId, boolean activeOnly, int page, int size) {
        log.info("팝업 목록 서비스 - domainId={}, activeOnly={}, page={}, size={}",
                domainId, activeOnly, page, size);
        var pg = persistencePort.findPage(blankToNull(domainId), activeOnly, PageRequest.of(page, size));
        List<PopupItemResponse> content = pg.getContent().stream()
                .map(PopupItemResponse::from)
                .collect(Collectors.toList());
        return PaginationResponse.<PopupItemResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .build();
    }

    public PopupItemResponse get(String id) {
        log.info("팝업 상세 서비스 - id={}", id);
        return persistencePort.findById(id)
                .map(PopupItemResponse::from)
                .orElseThrow(() -> new PopupItemNotFoundException(id));
    }

    @Transactional
    public PopupItemResponse create(PopupItemCreateRequest req) {
        PopupItem d = new PopupItem();
        d.setId("POP" + System.currentTimeMillis());
        apply(d, req);
        PopupItem saved = persistencePort.save(d);
        log.info("팝업 등록 완료 - id={}", saved.getId());
        return PopupItemResponse.from(saved);
    }

    @Transactional
    public PopupItemResponse update(String id, PopupItemUpdateRequest req) {
        PopupItem existing = persistencePort.findById(id)
                .orElseThrow(() -> new PopupItemNotFoundException(id));
        apply(existing, req);
        PopupItem saved = persistencePort.save(existing);
        log.info("팝업 수정 완료 - id={}", id);
        return PopupItemResponse.from(saved);
    }

    @Transactional
    public void delete(String id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new PopupItemNotFoundException(id);
        }
        persistencePort.deleteById(id);
        log.info("팝업 삭제 완료 - id={}", id);
    }

    private static void apply(PopupItem d, PopupItemCreateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setTitle(req.getTitle().trim());
        d.setFileUrl(req.getFileUrl());
        d.setLinkTarget(req.getLinkTarget());
        d.setImgPath(req.getImgPath());
        d.setFileName(req.getFileName());
        d.setVerticalPos(req.getVerticalPos());
        d.setWidthPos(req.getWidthPos());
        d.setVerticalSize(req.getVerticalSize());
        d.setWidthSize(req.getWidthSize());
        d.setNoticeBegin(req.getNoticeBegin());
        d.setNoticeEnd(req.getNoticeEnd());
        d.setStopTodayHide(yn(req.getStopTodayHide(), "N"));
        d.setNoticeActive(yn(req.getNoticeActive(), "Y"));
    }

    private static void apply(PopupItem d, PopupItemUpdateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setTitle(req.getTitle().trim());
        d.setFileUrl(req.getFileUrl());
        d.setLinkTarget(req.getLinkTarget());
        d.setImgPath(req.getImgPath());
        d.setFileName(req.getFileName());
        d.setVerticalPos(req.getVerticalPos());
        d.setWidthPos(req.getWidthPos());
        d.setVerticalSize(req.getVerticalSize());
        d.setWidthSize(req.getWidthSize());
        d.setNoticeBegin(req.getNoticeBegin());
        d.setNoticeEnd(req.getNoticeEnd());
        d.setStopTodayHide(yn(req.getStopTodayHide(), "N"));
        d.setNoticeActive(yn(req.getNoticeActive(), "Y"));
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String yn(String v, String def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        return v.trim().equalsIgnoreCase("Y") ? "Y" : "N";
    }
}
