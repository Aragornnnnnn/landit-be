// 어드민 피드백 날짜 없는 검색 쿼리의 PostgreSQL 계약을 검증한다.

package com.landit.landitbe.feature.mailbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.landit.landitbe.feature.mailbox.repository.AdminMailboxFeedbackRepository;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/** 어드민 피드백 날짜 없는 검색 쿼리의 PostgreSQL 계약을 검증한다. */
class AdminMailboxFeedbackRepositoryContractTest {

  /** 날짜가 없을 때 PostgreSQL이 타입을 추론할 날짜 파라미터 자체가 쿼리에 없어야 한다. */
  @Test
  void exposesSearchQueryWithoutCreatedDatePredicates() {
    Method searchMethod =
        Arrays.stream(AdminMailboxFeedbackRepository.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("searchWithoutCreatedRange"))
            .findFirst()
            .orElse(null);

    assertThat(searchMethod).as("날짜 없는 피드백 검색 메서드").isNotNull();
    Query query = searchMethod.getAnnotation(Query.class);
    assertThat(query).as("날짜 없는 피드백 검색 쿼리").isNotNull();
    assertThat(query.value()).doesNotContain(":createdFrom").doesNotContain(":createdTo");
  }
}
