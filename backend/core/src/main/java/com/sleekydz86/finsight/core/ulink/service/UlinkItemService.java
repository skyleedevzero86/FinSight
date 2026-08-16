package com.sleekydz86.finsight.core.ulink.service;

import com.sleekydz86.finsight.core.global.dto.PaginationResponse;
import com.sleekydz86.finsight.core.global.exception.UlinkItemNotFoundException;
import com.sleekydz86.finsight.core.ulink.domain.UlinkItem;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemCreateRequest;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemResponse;
import com.sleekydz86.finsight.core.ulink.domain.port.in.dto.UlinkItemUpdateRequest;
import com.sleekydz86.finsight.core.ulink.domain.port.out.UlinkItemPersistencePort;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UlinkItemService {

    private final UlinkItemPersistencePort persistencePort;

    public UlinkItemService(UlinkItemPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public PaginationResponse<UlinkItemResponse> list(
            String domainId, String sectionCode, int page, int size) {
        var pg = persistencePort.findPage(blankToNull(domainId), blankToNull(sectionCode), PageRequest.of(page, size));
        List<UlinkItemResponse> content = pg.getContent().stream()
                .map(UlinkItemResponse::from)
                .collect(Collectors.toList());
        return PaginationResponse.<UlinkItemResponse>builder()
                .content(content)
                .page(pg.getNumber())
                .size(pg.getSize())
                .totalElements(pg.getTotalElements())
                .build();
    }

    public UlinkItemResponse get(String id) {
        return persistencePort.findById(id)
                .map(UlinkItemResponse::from)
                .orElseThrow(() -> new UlinkItemNotFoundException(id));
    }

    @Transactional
    public UlinkItemResponse create(UlinkItemCreateRequest req) {
        UlinkItem d = new UlinkItem();
        d.setId("ULK" + System.currentTimeMillis());
        apply(d, req.getDomainId(), req.getSectionCode(), req.getLinkGroup(),
                req.getLinkName(), req.getLinkUrl(), req.getLinkTarget(), req.getDescription());
        return UlinkItemResponse.from(persistencePort.save(d));
    }

    @Transactional
    public UlinkItemResponse update(String id, UlinkItemUpdateRequest req) {
        UlinkItem existing = persistencePort.findById(id)
                .orElseThrow(() -> new UlinkItemNotFoundException(id));
        apply(existing, req.getDomainId(), req.getSectionCode(), req.getLinkGroup(),
                req.getLinkName(), req.getLinkUrl(), req.getLinkTarget(), req.getDescription());
        return UlinkItemResponse.from(persistencePort.save(existing));
    }

    @Transactional
    public void delete(String id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new UlinkItemNotFoundException(id);
        }
        persistencePort.deleteById(id);
    }

    private static void apply(
            UlinkItem d,
            String domainId,
            String sectionCode,
            String linkGroup,
            String linkName,
            String linkUrl,
            String linkTarget,
            String description) {
        d.setDomainId(blankToNull(domainId));
        d.setSectionCode(blankToNull(sectionCode));
        d.setLinkGroup(blankToNull(linkGroup));
        d.setLinkName(linkName.trim());
        d.setLinkUrl(linkUrl.trim());
        d.setLinkTarget(blankToNull(linkTarget));
        d.setDescription(description);
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }
}
