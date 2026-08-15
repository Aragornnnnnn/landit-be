// 편지함 어드민 공지·업데이트 수정 요청을 정의한다.

package com.landit.landitbe.feature.mailbox.dto;

import com.landit.landitbe.feature.mailbox.domain.MailboxLetterType;
import com.landit.landitbe.feature.mailbox.domain.MailboxPublicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 편지함 어드민 공지·업데이트 부분 수정 요청이다.
 *
 * @param type 편지 유형
 * @param title 편지 제목
 * @param contentBlocks 구조화된 본문 블록
 * @param preview 목록 미리보기
 * @param publicationStatus 게시 상태
 * @param pinned 상단 고정 여부
 */
@Schema(description = "편지함 어드민 공지·업데이트 수정 요청")
public record AdminMailboxLetterPatchRequest(
    @Schema(description = "편지 유형") MailboxLetterType type,
    @Size(max = 200) @Schema(description = "편지 제목") String title,
    @Schema(description = "구조화된 본문 블록") List<Object> contentBlocks,
    @Schema(description = "목록 미리보기") String preview,
    @Schema(description = "게시 상태") MailboxPublicationStatus publicationStatus,
    @Schema(description = "상단 고정 여부") Boolean pinned) {

  /**
   * 하나 이상의 수정 항목이 포함됐는지 확인한다.
   *
   * @return 수정할 항목이 있으면 {@code true}
   */
  public boolean hasChanges() {
    return type != null
        || title != null
        || contentBlocks != null
        || preview != null
        || publicationStatus != null
        || pinned != null;
  }
}
