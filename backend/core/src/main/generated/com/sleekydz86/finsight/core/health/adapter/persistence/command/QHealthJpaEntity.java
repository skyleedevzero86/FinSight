package com.sleekydz86.finsight.core.health.adapter.persistence.command;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QHealthJpaEntity is a Querydsl query type for HealthJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QHealthJpaEntity extends EntityPathBase<HealthJpaEntity> {

    private static final long serialVersionUID = 1486923576L;

    public static final QHealthJpaEntity healthJpaEntity = new QHealthJpaEntity("healthJpaEntity");

    public final DateTimePath<java.time.LocalDateTime> checkedAt = createDateTime("checkedAt", java.time.LocalDateTime.class);

    public final StringPath componentStatusesJson = createString("componentStatusesJson");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath id = createString("id");

    public final StringPath jvmMetricsJson = createString("jvmMetricsJson");

    public final StringPath message = createString("message");

    public final StringPath status = createString("status");

    public final StringPath systemMetricsJson = createString("systemMetricsJson");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QHealthJpaEntity(String variable) {
        super(HealthJpaEntity.class, forVariable(variable));
    }

    public QHealthJpaEntity(Path<? extends HealthJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QHealthJpaEntity(PathMetadata metadata) {
        super(HealthJpaEntity.class, metadata);
    }

}

