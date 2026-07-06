// Sentry SDK를 사용해 예외 이벤트를 전송하는 기본 reporter다.
package com.landit.landitbe.common.observability;

import io.sentry.Sentry;
import org.springframework.stereotype.Component;

@Component
public class DefaultSentryEventReporter implements SentryEventReporter {

    /** 전달받은 예외를 Sentry 이벤트로 전송한다. */
    @Override
    public void captureException(Throwable exception) {
        Sentry.captureException(exception);
    }
}
