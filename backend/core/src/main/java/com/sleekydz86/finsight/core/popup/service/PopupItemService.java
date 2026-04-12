package com.sleekydz86.finsight.core.popup.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.PopupItemNotFoundException;
import com.sleekydz86.finsight.core.popup.domain.PopupItem;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemCreateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemResponse;
import com.sleekydz86.finsight.core.popup.domain.port.in.dto.PopupItemUpdateRequest;
import com.sleekydz86.finsight.core.popup.domain.port.out.PopupItemPersistencePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PopupItemService {

    private final PopupItemPersistencePort persistencePort;

    public PopupItemService(PopupItemPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public PaginationResponse<PopupItemResponse> list(
            String domainId, boolean activeOnly, int page, int size) {
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
        return persistencePort.findById(id)
                .map(PopupItemResponse::from)
                .orElseThrow(() -> new PopupItemNotFoundException(id));
    }

    @Transactional
    public PopupItemResponse create(PopupItemCreateRequest req) {
        PopupItem d = new PopupItem();
        d.setId("POP" + System.currentTimeMillis());
        apply(d, req);
        return PopupItemResponse.from(persistencePort.save(d));
    }

    @Transactional
    public PopupItemResponse update(String id, PopupItemUpdateRequest req) {
        PopupItem existing = persistencePort.findById(id)
                .orElseThrow(() -> new PopupItemNotFoundException(id));
        apply(existing, req);
        return PopupItemResponse.from(persistencePort.save(existing));
    }

    @Transactional
    public void delete(String id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new PopupItemNotFoundException(id);
        }
        persistencePort.deleteById(id);
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
