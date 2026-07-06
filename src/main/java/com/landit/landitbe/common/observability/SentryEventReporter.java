// Sentry로 예외 이벤트를 전송하는 관측성 경계를 정의한다.
package com.landit.landitbe.common.observability;

public interface SentryEventReporter {

    /** 운영자가 확인해야 할 예외 이벤트를 Sentry로 전송한다. */
    void captureException(Throwable exception);
}
