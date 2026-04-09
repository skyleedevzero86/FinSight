package com.sleekydz86.finsight.core.media.youtube.domain.port.out;

import com.sleekydz86.finsight.core.media.youtube.domain.YoutubeImportSource;

import java.util.List;
import java.util.Optional;

public interface YoutubeImportSourcePersistencePort {
    YoutubeImportSource save(YoutubeImportSource source);

    Optional<YoutubeImportSource> findById(Long sourceId);

    List<YoutubeImportSource> findAll();

    List<YoutubeImportSource> findActiveSources();
}
