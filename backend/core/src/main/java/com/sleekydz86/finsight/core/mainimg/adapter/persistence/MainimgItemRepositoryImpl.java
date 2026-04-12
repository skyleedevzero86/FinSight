package com.sleekydz86.finsight.core.mainimg.adapter.persistence;

import com.sleekydz86.finsight.core.mainimg.domain.MainimgItem;
import com.sleekydz86.finsight.core.mainimg.domain.port.out.MainimgItemPersistencePort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MainimgItemRepositoryImpl implements MainimgItemPersistencePort {

    private final MainimgItemJpaRepository jpaRepository;

    public MainimgItemRepositoryImpl(MainimgItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Page<MainimgItem> findPage(String domainId, boolean reflectOnly, Pageable pageable) {
        Page<MainimgItemJpaEntity> page =
                reflectOnly ? jpaRepository.searchReflectOnly(domainId, pageable) : jpaRepository.searchAll(domainId, pageable);
        return page.map(MainimgItemRepositoryImpl::toDomain);
    }

    @Override
    public Optional<MainimgItem> findById(String id) {
        return jpaRepository.findById(id).map(MainimgItemRepositoryImpl::toDomain);
    }

    @Override
    public MainimgItem save(MainimgItem item) {
        return toDomain(jpaRepository.save(toEntity(item)));
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

    static MainimgItem toDomain(MainimgItemJpaEntity e) {
        MainimgItem d = new MainimgItem();
        d.setId(e.getId());
        d.setDomainId(e.getDomainId());
        d.setImageName(e.getImageName());
        d.setImage(e.getImage());
        d.setImageFile(e.getImageFile());
        d.setDescription(e.getDescription());
        d.setReflectYn(e.getReflectYn());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    static MainimgItemJpaEntity toEntity(MainimgItem d) {
        MainimgItemJpaEntity e = new MainimgItemJpaEntity();
        e.setId(d.getId());
        e.setDomainId(d.getDomainId());
        e.setImageName(d.getImageName());
        e.setImage(d.getImage());
        e.setImageFile(d.getImageFile());
        e.setDescription(d.getDescription());
        e.setReflectYn(d.getReflectYn() != null ? d.getReflectYn() : "Y");
        return e;
    }
}
