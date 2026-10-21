package com.sleekydz86.finsight.core.news.crawl;

import java.util.List;
import java.util.Optional;

public interface NewsCrawlIncidentStore {

    NewsCrawlIncident insert(NewsCrawlIncident incident);

    Optional<NewsCrawlIncident> findById(long id);

    List<NewsCrawlIncident> findRecent(int limit);

    Optional<NewsCrawlIncident> updateIfStatus(
            long id,
            NewsCrawlIncidentStatus expected,
            NewsCrawlIncident replacement);

    NewsCrawlAttempt insertAttempt(NewsCrawlAttempt attempt);

    List<NewsCrawlAttempt> findAttempts(long incidentId);
}
