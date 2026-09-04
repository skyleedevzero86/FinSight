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

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class MainimgItemService {

    private final MainimgItemPersistencePort persistencePort;

    public MainimgItemService(MainimgItemPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public PaginationResponse<MainimgItemResponse> list(
            String domainId, boolean reflectOnly, int page, int size) {
        log.info("메인이미지 목록 서비스 - domainId={}, reflectOnly={}, page={}, size={}",
                domainId, reflectOnly, page, size);
        var pg = persistencePort.findPage(blankToNull(domainId), reflectOnly, PageRequest.of(page, size));
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
        apply(d, req);
        MainimgItem saved = persistencePort.save(d);
        log.info("메인이미지 등록 완료 - id={}", saved.getId());
        return MainimgItemResponse.from(saved);
    }

    @Transactional
    public MainimgItemResponse update(String id, MainimgItemUpdateRequest req) {
        MainimgItem existing = persistencePort.findById(id)
                .orElseThrow(() -> new MainimgItemNotFoundException(id));
        apply(existing, req);
        MainimgItem saved = persistencePort.save(existing);
        log.info("메인이미지 수정 완료 - id={}", id);
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

    private static void apply(MainimgItem d, MainimgItemCreateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setImageName(req.getImageName().trim());
        d.setImage(req.getImage());
        d.setImageFile(req.getImageFile());
        d.setDescription(req.getDescription());
        d.setReflectYn(yn(req.getReflectYn(), "Y"));
    }

    private static void apply(MainimgItem d, MainimgItemUpdateRequest req) {
        d.setDomainId(blankToNull(req.getDomainId()));
        d.setImageName(req.getImageName().trim());
        d.setImage(req.getImage());
        d.setImageFile(req.getImageFile());
        d.setDescription(req.getDescription());
        d.setReflectYn(yn(req.getReflectYn(), "Y"));
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
