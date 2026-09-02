package com.sleekydz86.finsight.core.media.livevod.adapter.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LiveVodCommentJpaRepository extends JpaRepository<LiveVodCommentJpaEntity, Long> {
    /**
 * Retrieves all comments for a video in ascending creation order.
 *
 * @param videoId the video identifier
 * @return the video's comments ordered from oldest to newest
 */
List<LiveVodCommentJpaEntity> findByVideoIdOrderByCreatedAtAsc(String videoId);

    /**
 * Retrieves top-level comments for a video, ordered from newest to oldest.
 *
 * @param videoId  the video identifier
 * @param pageable the pagination and page size configuration
 * @return        a page of top-level comments
 */
Page<LiveVodCommentJpaEntity> findByVideoIdAndParentIsNullOrderByCreatedAtDesc(String videoId, Pageable pageable);

    /**
 * Retrieves replies for a parent comment in ascending creation order.
 *
 * @param parentId the identifier of the parent comment
 * @param pageable the pagination and sorting configuration
 * @return a page of replies for the specified parent comment
 */
Page<LiveVodCommentJpaEntity> findByParent_IdOrderByCreatedAtAsc(Long parentId, Pageable pageable);

    /**
 * Counts the comments associated with a video.
 *
 * @param videoId the identifier of the video
 * @return the number of comments associated with the video
 */
long countByVideoId(String videoId);

    /**
 * Counts the top-level comments for a video.
 *
 * @param videoId the identifier of the video
 * @return the number of top-level comments
 */
long countByVideoIdAndParentIsNull(String videoId);

    /**
 * Counts the replies for a parent comment.
 *
 * @param parentId the parent comment identifier
 * @return the number of replies for the parent comment
 */
long countByParent_Id(Long parentId);

    /**
     * Groups comment counts by video ID.
     *
     * @param videoIds the video IDs to count comments for
     * @return rows containing each video ID and its comment count
     */
    @Query("SELECT c.videoId, COUNT(c) FROM LiveVodCommentJpaEntity c WHERE c.videoId IN :videoIds GROUP BY c.videoId")
    List<Object[]> countByVideoIds(@Param("videoIds") Collection<String> videoIds);
}
