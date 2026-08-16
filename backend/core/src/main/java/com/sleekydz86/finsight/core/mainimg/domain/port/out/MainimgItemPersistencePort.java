package com.sleekydz86.finsight.core.mainimg.domain.port.out;

import com.sleekydz86.finsight.core.mainimg.domain.MainimgItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MainimgItemPersistencePort {

    Page<MainimgItem> findPage(String domainId, boolean reflectOnly, Pageable pageable);

    Optional<MainimgItem> findById(String id);

    MainimgItem save(MainimgItem item);

    void deleteById(String id);
}
