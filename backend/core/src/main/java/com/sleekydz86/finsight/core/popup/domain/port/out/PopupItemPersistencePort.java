package com.sleekydz86.finsight.core.popup.domain.port.out;

import com.sleekydz86.finsight.core.popup.domain.PopupItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PopupItemPersistencePort {

    Page<PopupItem> findPage(String domainId, boolean activeOnly, Pageable pageable);

    Optional<PopupItem> findById(String id);

    PopupItem save(PopupItem item);

    void deleteById(String id);
}
