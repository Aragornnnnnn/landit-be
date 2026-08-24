// 편지함 어드민 피드백 검색과 일괄 처리를 지원한다.

package com.landit.landitbe.feature.mailbox.repository;

import com.landit.landitbe.feature.mailbox.domain.MailboxFeedback;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackStatus;
import com.landit.landitbe.feature.mailbox.domain.UserFeedbackType;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 편지함 어드민 피드백 검색과 일괄 처리를 지원한다. */
public interface AdminMailboxFeedbackRepository extends JpaRepository<MailboxFeedback, Long> {

  /**
   * 사용자 정보와 함께 어드민 피드백을 검색한다.
   *
   * @param keyword 본문 검색어
   * @param type 피드백 유형
   * @param status 처리 상태
   * @param createdFrom 검색 시작 시각
   * @param createdTo 검색 종료 시각
   * @param pageable 페이지와 정렬 조건
   * @return 조건에 맞는 피드백 페이지
   */
  @Query(
      """
      select feedback.id as feedbackId,
             feedback.userProfileId as userProfileId,
             profile.email as email,
             profile.nickname as nickname,
             feedback.feedbackType as type,
             feedback.contentText as content,
             feedback.processingStatus as status,
             feedback.resolvedByFeedbackId as resolvedByFeedbackId,
             feedback.createdAt as createdAt,
             feedback.updatedAt as updatedAt
      from MailboxFeedback feedback, UserProfile profile
      where profile.id = feedback.userProfileId
        and (:keyword is null
          or lower(feedback.contentText) like lower(concat('%', cast(:keyword as string), '%')) escape '!')
        and (:type is null or feedback.feedbackType = :type)
        and (:status is null or feedback.processingStatus = :status)
        and (:createdFrom is null or feedback.createdAt >= :createdFrom)
        and (:createdTo is null or feedback.createdAt < :createdTo)
      """)
  Page<AdminMailboxFeedbackSummary> search(
      @Param("keyword") String keyword,
      @Param("type") UserFeedbackType type,
      @Param("status") UserFeedbackStatus status,
      @Param("createdFrom") LocalDateTime createdFrom,
      @Param("createdTo") LocalDateTime createdTo,
      Pageable pageable);

  /**
   * 일괄 답장 대상 피드백을 ID 순서로 잠금 조회한다.
   *
   * @param ids 잠글 피드백 ID
   * @return ID 순서의 피드백 목록
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<MailboxFeedback> findAllByIdInOrderByIdAsc(Collection<Long> ids);

  /** 어드민 피드백 검색 결과 Projection이다. */
  interface AdminMailboxFeedbackSummary {

    /**
     * 피드백 식별자를 반환한다.
     *
     * @return 피드백 ID
     */
    Long getFeedbackId();

    /**
     * 사용자 식별자를 반환한다.
     *
     * @return 사용자 ID
     */
    Long getUserProfileId();

    /**
     * 사용자 이메일을 반환한다.
     *
     * @return 사용자 이메일
     */
    String getEmail();

    /**
     * 사용자 닉네임을 반환한다.
     *
     * @return 사용자 닉네임
     */
    String getNickname();

    /**
     * 피드백 유형을 반환한다.
     *
     * @return 피드백 유형
     */
    UserFeedbackType getType();

    /**
     * 피드백 본문을 반환한다.
     *
     * @return 피드백 본문
     */
    String getContent();

    /**
     * 처리 상태를 반환한다.
     *
     * @return 처리 상태
     */
    UserFeedbackStatus getStatus();

    /**
     * 대표 피드백 식별자를 반환한다.
     *
     * @return 대표 피드백 ID
     */
    Long getResolvedByFeedbackId();

    /**
     * 생성 시각을 반환한다.
     *
     * @return 생성 시각
     */
    LocalDateTime getCreatedAt();

    /**
     * 수정 시각을 반환한다.
     *
     * @return 수정 시각
     */
    LocalDateTime getUpdatedAt();
  }
}
