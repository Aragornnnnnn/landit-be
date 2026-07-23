// 세션 히스토리의 외부 저장 artifact 경로를 저장한다.

package com.landit.landitbe.feature.session.domain;

import com.landit.landitbe.shared.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 세션 히스토리의 외부 저장 artifact 경로를 저장한다. */
@Entity
@Table(name = "session_history_artifact")
public class SessionHistoryArtifact extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_history_id", nullable = false)
  private Long sessionHistoryId;

  @Column(name = "session_history_message_id")
  private Long sessionHistoryMessageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "artifact_type", nullable = false, length = 30)
  private ArtifactType artifactType;

  @Enumerated(EnumType.STRING)
  @Column(name = "storage_provider", nullable = false, length = 20)
  private StorageProvider storageProvider;

  @Column(name = "data_path", nullable = false, length = 500)
  private String dataPath;

  @Column(name = "byte_size")
  private Long byteSize;

  /** 동작을 수행한다. */
  protected SessionHistoryArtifact() {}
}
