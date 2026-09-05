package com.sleekydz86.finsight.core.mainimg.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.MainimgItemNotFoundException;
import com.sleekydz86.finsight.core.mainimg.domain.MainimgItem;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemCreateRequest;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemResponse;
import com.sleekydz86.finsight.core.mainimg.domain.port.in.dto.MainimgItemUpdateRequest;
import com.sleekydz86.finsight.core.mainimg.domain.port.out.MainimgItemPersistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MainimgItemService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MainimgItemPersistencePort persistencePort;

    public MainimgItemService(MainimgItemPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public PaginationResponse<MainimgItemResponse> list(
            String domainId, boolean reflectOnly, int page, int size) {
        log.info("메인이미지 목록 서비스 - domainId={}, reflectOnly={}, page={}, size={}",
                domainId, reflectOnly, page, size);
        String today = LocalDate.now().format(DAY);
        var pg = persistencePort.findPage(
                blankToNull(domainId), reflectOnly, today, PageRequest.of(page, size));
        List<MainimgItemResponse> content = pg.getContent().stream()
                .map(MainimgItemResponse::from)
                .collect(Collectors.toList());
        return PaginationResponse.<MainimgItemResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .build();
    }

    public MainimgItemResponse get(String id) {
        log.info("메인이미지 상세 서비스 - id={}", id);
        return persistencePort.findById(id)
                .map(MainimgItemResponse::from)
                .orElseThrow(() -> new MainimgItemNotFoundException(id));
    }

    @Transactional
    public MainimgItemResponse create(MainimgItemCreateRequest req) {
        MainimgItem d = new MainimgItem();
        d.setId("IMG" + System.currentTimeMillis());
        applyCreate(d, req);
        MainimgItem saved = persistencePort.save(d);
        log.info("메인이미지 등록 완료 - id={}, sortOrder={}", saved.getId(), saved.getSortOrder());
        return MainimgItemResponse.from(saved);
    }

    @Transactional
    public MainimgItemResponse update(String id, MainimgItemUpdateRequest req) {
        MainimgItem existing = persistencePort.findById(id)
                .orElseThrow(() -> new MainimgItemNotFoundException(id));
        applyUpdate(existing, req);
        MainimgItem saved = persistencePort.save(existing);
        log.info("메인이미지 수정 완료 - id={}, sortOrder={}", id, saved.getSortOrder());
        return MainimgItemResponse.from(saved);
    }

    @Transactional
    public void delete(String id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new MainimgItemNotFoundException(id);
        }
        persistencePort.deleteById(id);
        log.info("메인이미지 삭제 완료 - id={}", id);
    }

    private void applyCreate(MainimgItem d, MainimgItemCreateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setImageName(req.getImageName().trim());
        d.setImage(blankToNull(req.getImage()));
        d.setImageFile(blankToNull(req.getImageFile()));
        d.setDescription(blankToNull(req.getDescription()));
        d.setLinkUrl(blankToNull(req.getLinkUrl()));
        d.setNoticeBegin(blankToNull(req.getNoticeBegin()));
        d.setNoticeEnd(blankToNull(req.getNoticeEnd()));
        d.setReflectYn(yn(req.getReflectYn(), "Y"));
        d.setSortOrder(resolveSortOrder(req.getSortOrder(), true));
    }

    private void applyUpdate(MainimgItem d, MainimgItemUpdateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setImageName(req.getImageName().trim());
        d.setImage(blankToNull(req.getImage()));
        d.setImageFile(blankToNull(req.getImageFile()));
        d.setDescription(blankToNull(req.getDescription()));
        d.setLinkUrl(blankToNull(req.getLinkUrl()));
        d.setNoticeBegin(blankToNull(req.getNoticeBegin()));
        d.setNoticeEnd(blankToNull(req.getNoticeEnd()));
        d.setReflectYn(yn(req.getReflectYn(), "Y"));
        if (req.getSortOrder() != null) {
            d.setSortOrder(Math.max(1, req.getSortOrder()));
        } else if (d.getSortOrder() == null || d.getSortOrder() < 1) {
            d.setSortOrder(resolveSortOrder(null, true));
        }
    }

    private int resolveSortOrder(Integer requested, boolean allocateNext) {
        if (requested != null && requested >= 1) {
            return requested;
        }
        if (!allocateNext) {
            return 1;
        }
        return persistencePort.findMaxSortOrder() + 1;
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
        return v.trim().equalsIgnoreCase("N") ? "N" : "Y";
    }
}
