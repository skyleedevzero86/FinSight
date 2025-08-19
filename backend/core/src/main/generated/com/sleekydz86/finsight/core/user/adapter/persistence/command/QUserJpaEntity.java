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

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> lastLoginAt = createDateTime("lastLoginAt", java.time.LocalDateTime.class);

    public final ListPath<com.sleekydz86.finsight.core.user.domain.NotificationType, EnumPath<com.sleekydz86.finsight.core.user.domain.NotificationType>> notificationPreferences = this.<com.sleekydz86.finsight.core.user.domain.NotificationType, EnumPath<com.sleekydz86.finsight.core.user.domain.NotificationType>>createList("notificationPreferences", com.sleekydz86.finsight.core.user.domain.NotificationType.class, EnumPath.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final EnumPath<com.sleekydz86.finsight.core.user.domain.UserRole> role = createEnum("role", com.sleekydz86.finsight.core.user.domain.UserRole.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

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

