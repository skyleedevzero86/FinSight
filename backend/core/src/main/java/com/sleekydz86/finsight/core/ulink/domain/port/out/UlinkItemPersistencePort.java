package com.sleekydz86.finsight.core.ulink.domain.port.out;

import com.sleekydz86.finsight.core.ulink.domain.UlinkItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UlinkItemPersistencePort {

    Page<UlinkItem> findPage(String domainId, String sectionCode, boolean openOnly, Pageable pageable);

    Optional<UlinkItem> findById(String id);

    UlinkItem save(UlinkItem item);

    void deleteById(String id);

    int findMaxSortOrder();
}
