package com.sleekydz86.finsight.core.editor.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EditorAssetJpaRepository extends JpaRepository<EditorAssetJpaEntity, String> {
}
