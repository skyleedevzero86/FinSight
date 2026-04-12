package com.sleekydz86.finsight.core.popup.adapter.persistence;

import com.sleekydz86.finsight.core.popup.domain.PopupItem;
import com.sleekydz86.finsight.core.popup.domain.port.out.PopupItemPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PopupItemRepositoryImpl implements PopupItemPersistencePort {

    private final PopupItemJpaRepository jpaRepository;

    public PopupItemRepositoryImpl(PopupItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Page<PopupItem> findPage(String domainId, boolean activeOnly, Pageable pageable) {
        Page<PopupItemJpaEntity> page =
                activeOnly ? jpaRepository.searchActiveOnly(domainId, pageable) : jpaRepository.searchAll(domainId, pageable);
        return page.map(PopupItemRepositoryImpl::toDomain);
    }

    @Override
    public Optional<PopupItem> findById(String id) {
        return jpaRepository.findById(id).map(PopupItemRepositoryImpl::toDomain);
    }

    @Override
    public PopupItem save(PopupItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    static PopupItem toDomain(PopupItemJpaEntity e) {
        PopupItem d = new PopupItem();
        d.setId(e.getId());
        d.setDomainId(e.getDomainId());
        d.setTitle(e.getTitle());
        d.setFileUrl(e.getFileUrl());
        d.setLinkTarget(e.getLinkTarget());
        d.setImgPath(e.getImgPath());
        d.setFileName(e.getFileName());
        d.setVerticalPos(e.getVerticalPos());
        d.setWidthPos(e.getWidthPos());
        d.setVerticalSize(e.getVerticalSize());
        d.setWidthSize(e.getWidthSize());
        d.setNoticeBegin(e.getNoticeBegin());
        d.setNoticeEnd(e.getNoticeEnd());
        d.setStopTodayHide(e.getStopTodayHide());
        d.setNoticeActive(e.getNoticeActive());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    static PopupItemJpaEntity toEntity(PopupItem d) {
        PopupItemJpaEntity e = new PopupItemJpaEntity();
        e.setId(d.getId());
        e.setDomainId(d.getDomainId());
        e.setTitle(d.getTitle());
        e.setFileUrl(d.getFileUrl());
        e.setLinkTarget(d.getLinkTarget());
        e.setImgPath(d.getImgPath());
        e.setFileName(d.getFileName());
        e.setVerticalPos(d.getVerticalPos());
        e.setWidthPos(d.getWidthPos());
        e.setVerticalSize(d.getVerticalSize());
        e.setWidthSize(d.getWidthSize());
        e.setNoticeBegin(d.getNoticeBegin());
        e.setNoticeEnd(d.getNoticeEnd());
        e.setStopTodayHide(d.getStopTodayHide() != null ? d.getStopTodayHide() : "N");
        e.setNoticeActive(d.getNoticeActive() != null ? d.getNoticeActive() : "Y");
        return e;
    }
}
