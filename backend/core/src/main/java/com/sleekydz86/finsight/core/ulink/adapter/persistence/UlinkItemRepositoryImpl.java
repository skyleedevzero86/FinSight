package com.sleekydz86.finsight.core.ulink.adapter.persistence;

import com.sleekydz86.finsight.core.ulink.domain.UlinkItem;
import com.sleekydz86.finsight.core.ulink.domain.port.out.UlinkItemPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UlinkItemRepositoryImpl implements UlinkItemPersistencePort {

    private final UlinkItemJpaRepository jpaRepository;

    public UlinkItemRepositoryImpl(UlinkItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Page<UlinkItem> findPage(String domainId, String sectionCode, boolean openOnly, Pageable pageable) {
        return jpaRepository.search(domainId, sectionCode, openOnly, pageable)
                .map(UlinkItemRepositoryImpl::toDomain);
    }

    @Override
    public Optional<UlinkItem> findById(String id) {
        return jpaRepository.findById(id).map(UlinkItemRepositoryImpl::toDomain);
    }

    @Override
    public UlinkItem save(UlinkItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int findMaxSortOrder() {
        return jpaRepository.findMaxSortOrder();
    }

    static UlinkItem toDomain(UlinkItemJpaEntity e) {
        UlinkItem d = new UlinkItem();
        d.setId(e.getId());
        d.setDomainId(e.getDomainId());
        d.setSectionCode(e.getSectionCode());
        d.setLinkGroup(e.getLinkGroup());
        d.setLinkName(e.getLinkName());
        d.setLinkUrl(e.getLinkUrl());
        d.setLinkTarget(e.getLinkTarget());
        d.setDescription(e.getDescription());
        d.setImgPath(e.getImgPath());
        d.setSortOrder(e.getSortOrder() != null ? e.getSortOrder() : 0);
        d.setOpenYn(e.getOpenYn() != null ? e.getOpenYn() : "Y");
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    static UlinkItemJpaEntity toEntity(UlinkItem d) {
        UlinkItemJpaEntity e = new UlinkItemJpaEntity();
        e.setId(d.getId());
        e.setDomainId(d.getDomainId());
        e.setSectionCode(d.getSectionCode());
        e.setLinkGroup(d.getLinkGroup());
        e.setLinkName(d.getLinkName());
        e.setLinkUrl(d.getLinkUrl());
        e.setLinkTarget(d.getLinkTarget());
        e.setDescription(d.getDescription());
        e.setImgPath(d.getImgPath());
        e.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : 0);
        e.setOpenYn(d.getOpenYn() != null ? d.getOpenYn() : "Y");
        return e;
    }
}
