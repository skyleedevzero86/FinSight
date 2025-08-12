package com.sleekydz86.finsight.core.news.adapter.persistence.command;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNewsJpaEntity is a Querydsl query type for NewsJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNewsJpaEntity extends EntityPathBase<NewsJpaEntity> {

    private static final long serialVersionUID = 1891404023L;

    public static final QNewsJpaEntity newsJpaEntity = new QNewsJpaEntity("newsJpaEntity");

    public final com.sleekydz86.finsight.core.global.QBaseEntity _super = new com.sleekydz86.finsight.core.global.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.sleekydz86.finsight.core.global.NewsProvider> newsProvider = createEnum("newsProvider", com.sleekydz86.finsight.core.global.NewsProvider.class);

    public final StringPath originalContent = createString("originalContent");

    public final StringPath originalTitle = createString("originalTitle");

    public final StringPath overview = createString("overview");

    public final DateTimePath<java.time.LocalDateTime> scrapedTime = createDateTime("scrapedTime", java.time.LocalDateTime.class);

    public final NumberPath<Double> sentimentScore = createNumber("sentimentScore", Double.class);

    public final EnumPath<com.sleekydz86.finsight.core.news.domain.vo.SentimentType> sentimentType = createEnum("sentimentType", com.sleekydz86.finsight.core.news.domain.vo.SentimentType.class);

    public final ListPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>> targetCategories = this.<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory, EnumPath<com.sleekydz86.finsight.core.news.domain.vo.TargetCategory>>createList("targetCategories", com.sleekydz86.finsight.core.news.domain.vo.TargetCategory.class, EnumPath.class, PathInits.DIRECT2);

    public final StringPath translatedContent = createString("translatedContent");

    public final StringPath translatedTitle = createString("translatedTitle");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNewsJpaEntity(String variable) {
        super(NewsJpaEntity.class, forVariable(variable));
    }

    public QNewsJpaEntity(Path<? extends NewsJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNewsJpaEntity(PathMetadata metadata) {
        super(NewsJpaEntity.class, metadata);
    }

}

