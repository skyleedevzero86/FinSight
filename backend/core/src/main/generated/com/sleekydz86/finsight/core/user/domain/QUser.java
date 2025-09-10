package com.sleekydz86.finsight.core.user.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 93238475L;

    public static final QUser user = new QUser("user");

    public final com.sleekydz86.finsight.core.global.QBaseTimeEntity _super = new com.sleekydz86.finsight.core.global.QBaseTimeEntity(this);

    public final DateTimePath<java.time.LocalDateTime> accountLockedAt = createDateTime("accountLockedAt", java.time.LocalDateTime.class);

    public final StringPath apiKey = createString("apiKey");

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> approvedBy = createNumber("approvedBy", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDate = _super.createDate;

    public final StringPath deviceToken = createString("deviceToken");

    public final DateTimePath<java.time.LocalDateTime> deviceTokenUpdatedAt = createDateTime("deviceTokenUpdatedAt", java.time.LocalDateTime.class);

    public final StringPath deviceType = createString("deviceType");

    public final StringPath email = createString("email");

    public final BooleanPath emailNotificationEnabled = createBoolean("emailNotificationEnabled");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath language = createString("language");

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> lastPasswordChangeDate = createDate("lastPasswordChangeDate", java.time.LocalDate.class);

    public final NumberPath<Integer> loginFailCount = createNumber("loginFailCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDate = _super.modifyDate;

    public final StringPath nickname = createString("nickname");

    public final ListPath<NotificationType, EnumPath<NotificationType>> notificationPreferences = this.<NotificationType, EnumPath<NotificationType>>createList("notificationPreferences", NotificationType.class, EnumPath.class, PathInits.DIRECT2);

    public final BooleanPath otpEnabled = createBoolean("otpEnabled");

    public final StringPath otpSecret = createString("otpSecret");

    public final BooleanPath otpVerified = createBoolean("otpVerified");

    public final StringPath password = createString("password");

    public final NumberPath<Integer> passwordChangeCount = createNumber("passwordChangeCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> passwordChangedAt = createDateTime("passwordChangedAt", java.time.LocalDateTime.class);

    public final StringPath phoneNumber = createString("phoneNumber");

    public final StringPath profileImageUrl = createString("profileImageUrl");

    public final BooleanPath pushNotificationEnabled = createBoolean("pushNotificationEnabled");

    public final EnumPath<UserRole> role = createEnum("role", UserRole.class);

    public final BooleanPath smsNotificationEnabled = createBoolean("smsNotificationEnabled");

    public final EnumPath<UserStatus> status = createEnum("status", UserStatus.class);

    public final StringPath timezone = createString("timezone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath username = createString("username");

    public final ListPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>> watchlist = this.<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>>createList("watchlist", com.sleekydz86.finsight.core.news.domain.vo.TargetCategory.class, EnumPath.class, PathInits.DIRECT2);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

