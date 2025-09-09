package com.sleekydz86.finsight.core.user.adapter.persistence.command;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserJpaEntity is a Querydsl query type for UserJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserJpaEntity extends EntityPathBase<UserJpaEntity> {

    private static final long serialVersionUID = 1565415704L;

    public static final QUserJpaEntity userJpaEntity = new QUserJpaEntity("userJpaEntity");

    public final com.sleekydz86.finsight.core.global.QBaseTimeEntity _super = new com.sleekydz86.finsight.core.global.QBaseTimeEntity(this);

    public final DateTimePath<java.time.LocalDateTime> accountLockedAt = createDateTime("accountLockedAt", java.time.LocalDateTime.class);

    public final StringPath apiKey = createString("apiKey");

    public final DateTimePath<java.time.LocalDateTime> approvedAt = createDateTime("approvedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> approvedBy = createNumber("approvedBy", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDate = _super.createDate;

    public final StringPath email = createString("email");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> lastPasswordChangeDate = createDate("lastPasswordChangeDate", java.time.LocalDate.class);

    public final NumberPath<Integer> loginFailCount = createNumber("loginFailCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifyDate = _super.modifyDate;

    public final StringPath nickname = createString("nickname");

    public final ListPath<com.sleekydz86.finsight.core.user.domain.NotificationType, EnumPath<com.sleekydz86.finsight.core.user.domain.NotificationType>> notificationPreferences = this.<com.sleekydz86.finsight.core.user.domain.NotificationType, EnumPath<com.sleekydz86.finsight.core.user.domain.NotificationType>>createList("notificationPreferences", com.sleekydz86.finsight.core.user.domain.NotificationType.class, EnumPath.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final NumberPath<Integer> passwordChangeCount = createNumber("passwordChangeCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> passwordChangedAt = createDateTime("passwordChangedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.sleekydz86.finsight.core.user.domain.UserRole> role = createEnum("role", com.sleekydz86.finsight.core.user.domain.UserRole.class);

    public final EnumPath<com.sleekydz86.finsight.core.user.domain.UserStatus> status = createEnum("status", com.sleekydz86.finsight.core.user.domain.UserStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath username = createString("username");

    public final ListPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>> watchlist = this.<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>>createList("watchlist", com.sleekydz86.finsight.core.news.domain.vo.TargetCategory.class, EnumPath.class, PathInits.DIRECT2);

    public QUserJpaEntity(String variable) {
        super(UserJpaEntity.class, forVariable(variable));
    }

    public QUserJpaEntity(Path<? extends UserJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserJpaEntity(PathMetadata metadata) {
        super(UserJpaEntity.class, metadata);
    }

}

