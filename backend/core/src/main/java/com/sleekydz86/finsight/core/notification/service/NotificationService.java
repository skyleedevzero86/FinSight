package com.sleekydz86.finsight.core.notification.service;

import com.sleekydz86.finsight.core.inbox.domain.InboxCategory;
import com.sleekydz86.finsight.core.inbox.service.InboxService;
import com.sleekydz86.finsight.core.news.domain.News;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import com.sleekydz86.finsight.core.notification.domain.port.out.NotificationSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserPersistencePort userPersistencePort;
    private final NotificationSenderPort notificationSenderPort;
    private final InboxService inboxService;

    public NotificationService(UserPersistencePort userPersistencePort,
                               NotificationSenderPort notificationSenderPort,
                               InboxService inboxService) {
        this.userPersistencePort = userPersistencePort;
        this.notificationSenderPort = notificationSenderPort;
        this.inboxService = inboxService;
    }

    public void notifyUsersAboutNews(News news) {
        if (news.getAiOverView() == null) {
            return;
        }
        List<TargetCategory> newsCategories = news.getAiOverView().getTargetCategories();
        if (newsCategories == null || newsCategories.isEmpty()) {
            return;
        }
        List<User> interestedUsers = userPersistencePort.findByWatchlistCategories(newsCategories);

        String categoriesLabel = newsCategories == null || newsCategories.isEmpty()
                ? ""
                : newsCategories.stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("");
        String newsTitle = news.getOriginalContent() != null && news.getOriginalContent().getTitle() != null
                ? news.getOriginalContent().getTitle()
                : "관심 종목 뉴스";
        String title = "[관심종목] " + newsTitle + (categoriesLabel.isBlank() ? "" : " (" + categoriesLabel + ")");
        String link = news.getId() != null ? "/news/" + news.getId() : "/news";

        for (User user : interestedUsers) {
            List<NotificationType> prefs = user.getNotificationPreferences();
            if (prefs != null && prefs.contains(NotificationType.EMAIL)) {
                notificationSenderPort.sendEmailNotification(user, news);
            }
            if (prefs != null && prefs.contains(NotificationType.PUSH)) {
                notificationSenderPort.sendPushNotification(user, news);
            }
        }

        try {
            Long newsId = news.getId();
            inboxService.createForUsers(
                    interestedUsers,
                    InboxCategory.WATCHLIST,
                    null,
                    "FinSight 뉴스",
                    null,
                    title,
                    categoriesLabel.isBlank() ? null : "관심 종목: " + categoriesLabel,
                    link,
                    "NEWS",
                    newsId);
        } catch (Exception e) {
            log.warn("뉴스 인앱 알림 생성 실패 - error={}", e.getMessage());
        }
    }
}
