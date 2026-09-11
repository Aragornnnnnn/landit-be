// 기존 FE API와 훅을 실제 BE·AI에 연결해 정상 경로와 FE 변경이 필요한 오류를 구분한다.
import { createElement, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, cleanup, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useAuthStore } from '@/shared/auth/auth-store';
import { api } from '@/shared/api/client';
import { getMySubscription } from '@/features/subscription/api/subscription';
import { usePaywallGate } from '@/features/subscription/model/usePaywallGate';
import { usePurchase } from '@/features/subscription/model/usePurchase';
import { subscriptionKeys } from '@/features/subscription/model/keys';
import { startScenarioTalkSession, submitScenarioTalkMessage } from '@/app/(protected)/conversation/scenario/[scenarioId]/_api/scenario-session';
import { createSessionFeedback } from '@/features/feedback/api/session-feedback';
import { getExpressions } from '@/features/expression/api/list';
import { scenarioExpressionBranchPath } from '@/shared/lib/routes';
import { endSession } from '@/features/conversation/api/session';
import { getExpressionLearning } from '@/features/expression/api/learning';
import { getExpressionPractice } from '@/features/expression/api/practice';
import { finishExpression } from '@/features/expression/api/finish';

const mocks = vi.hoisted(() => ({ push: vi.fn(), replace: vi.fn(), toast: vi.fn(), purchase: vi.fn(), restore: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push: mocks.push, replace: mocks.replace }) }));
vi.mock('@/shared/analytics', () => ({ track: vi.fn() }));
vi.mock('@/shared/ui/toast', () => ({ showToast: mocks.toast }));
vi.mock('@/features/subscription/model/shell-purchases', async (original) => ({
  ...await original<object>(), purchaseViaBridge: mocks.purchase, restoreViaBridge: mocks.restore,
}));

const actualFetch = globalThis.fetch;
const base = process.env.LAN474_BE_URL!;
const scenarioId = 900001;
const otherScenario = 900002;
let account: any;
let query: QueryClient;
let failSubscription = false;
let subscriptionCalls = 0;
const shell = { platform: 'ios', appVersion: '1.3.0', buildNumber: '8', bridgeVersion: 5 };
const wrapper = ({ children }: { children: ReactNode }) => createElement(QueryClientProvider, { client: query }, children);

async function login() {
  const nonce = crypto.randomUUID();
  const result = await api.post<any>('/api/v1/auth/social-login', {
    provider: 'GOOGLE', nonce, idToken: `${nonce}|${nonce}@example.com|Payment Test|${nonce}`,
  });
  useAuthStore.getState().setAuth(result.accessToken, result.refreshToken, result.user);
  await api.put('/api/v1/me/learning-level', { learningLevel: 5 });
  return result;
}
async function webhook(type = 'INITIAL_PURCHASE', target = account.user.userId, expiration = Date.now() + 86400000) {
  const response = await actualFetch(`${base}/webhooks/revenuecat`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'local-payment-test' },
    body: JSON.stringify({ api_version: '1.0', event: {
      id: crypto.randomUUID(), type, app_user_id: String(target), original_app_user_id: String(target),
      product_id: 'landit_monthly', period_type: 'NORMAL', environment: 'PRODUCTION', store: 'APP_STORE',
      purchased_at_ms: Date.now() - 1000, expiration_at_ms: expiration, event_timestamp_ms: Date.now(),
      price_in_purchased_currency: 9900, currency: 'KRW',
    } }),
  });
  expect(response.status, await response.text()).toBe(200);
}
async function gate() {
  query.setQueryData(subscriptionKeys.mine(account.user.userId), await getMySubscription());
  const hook = renderHook(() => usePaywallGate(), { wrapper });
  await act(async () => {});
  return hook;
}
async function finishConversation() {
  const session = await startScenarioTalkSession(scenarioId);
  const turn = await submitScenarioTalkMessage(session.sessionId, 'I like pizza because it is spicy.', 'TEXT');
  expect(turn.progress.completed).toBe(false);
  const end = await submitScenarioTalkMessage(session.sessionId, 'I ate pasta yesterday.', 'TEXT');
  expect(end.progress.completed).toBe(true);
  return session.sessionId;
}

beforeEach(async () => {
  vi.clearAllMocks();
  failSubscription = false;
  subscriptionCalls = 0;
  globalThis.fetch = ((input: string, options: RequestInit) => {
    if (typeof input === 'string' && input.startsWith('/api/')) {
      if (input === '/api/v1/me/subscription') {
        subscriptionCalls++;
        if (failSubscription) return Promise.reject(new TypeError('injected subscription network failure'));
      }
      return actualFetch(base + input, options);
    }
    return actualFetch(input, options);
  }) as typeof fetch;
  window.__LANDIT_NATIVE__ = { ...shell };
  useAuthStore.getState().clearAuth();
  query = new QueryClient({ defaultOptions: { queries: { retryDelay: 0 } } });
  account = await login();
});
afterEach(() => { cleanup(); query?.clear(); globalThis.fetch = actualFetch; });

describe('결제 ON — 정상 흐름', () => {
  it('첫 무료 대화 → 실제 AI 피드백·수준 평가 → 페이월 → 웹훅 결제 → 표현 완료', async () => {
    const firstGate = await gate();
    const go = vi.fn();
    act(() => firstGate.result.current.guard(go, { entry: 'scenario', door: 'today_scenario' }));
    expect(go).toHaveBeenCalledOnce();
    const sessionId = await finishConversation();
    await actualFetch(`${process.env.LAN474_AI_URL}/__test__/clear-cache`, { method: 'POST' });
    const feedback = await createSessionFeedback(sessionId);
    expect(feedback.messageFeedbacks).toHaveLength(2);
    expect(feedback.nativeScore).toBeGreaterThan(0);
    await waitFor(async () => {
      const level: any = await api.get(`/api/v1/sessions/${sessionId}/level-assessment`);
      expect(level.processingStatus).toBe('COMPLETED');
      expect(level.levelAssessment.source).toBe('MODEL');
    }, { timeout: 10000 });
    firstGate.unmount();
    const afterGate = await gate();
    act(() => afterGate.result.current.guard(go, { entry: 'expression', returnTo: scenarioExpressionBranchPath(scenarioId) }));
    expect(mocks.push).toHaveBeenCalledWith(expect.stringContaining('/paywall'));
    expect(go).toHaveBeenCalledOnce();
    await expect(getExpressionLearning(scenarioId)).rejects.toMatchObject({ status: 403, code: 'PREMIUM_REQUIRED' });
    mocks.purchase.mockImplementation(async () => { await webhook(); return { status: 'success', type: 'PURCHASE_RESULT' }; });
    const unlocked = vi.fn();
    const purchase = renderHook(() => usePurchase({ pricing: {}, onUnlocked: unlocked }), { wrapper });
    await act(() => purchase.result.current.purchase('monthly'));
    expect(unlocked).toHaveBeenCalledOnce();
    expect((await getMySubscription()).premium).toBe(true);
    expect((await getExpressionLearning(scenarioId)).expressionId).toBe(scenarioId);
    await getExpressionPractice(scenarioId);
    await finishExpression(scenarioId);
  });

  it('대화·표현을 시작한 뒤 구독 만료되어도 기존 FE 요청으로 완료한다', async () => {
    await webhook();
    const session = await startScenarioTalkSession(scenarioId);
    await getExpressionLearning(scenarioId);
    await webhook('EXPIRATION', account.user.userId, Date.now() - 1000);
    expect((await getMySubscription()).premium).toBe(false);
    await submitScenarioTalkMessage(session.sessionId, 'I like pizza because it is spicy.', 'TEXT');
    expect((await submitScenarioTalkMessage(session.sessionId, 'I ate pasta yesterday.', 'TEXT')).progress.completed).toBe(true);
    expect((await createSessionFeedback(session.sessionId)).messageFeedbacks).toHaveLength(2);
    await getExpressionPractice(scenarioId);
    await finishExpression(scenarioId);
    await expect(startScenarioTalkSession(otherScenario)).rejects.toMatchObject({ code: 'PREMIUM_REQUIRED' });
  });

  it('AI가 한 번 실패해도 clientMessageId 없는 FE가 새로 녹음한 내용으로 재시도한다', async () => {
    const session = await startScenarioTalkSession(scenarioId);
    await actualFetch(`${process.env.LAN474_AI_URL}/__test__/fail-next`, { method: 'POST' });
    await expect(submitScenarioTalkMessage(session.sessionId, 'Pizza.', 'TEXT')).rejects.toBeDefined();
    const retried = await submitScenarioTalkMessage(session.sessionId, 'I like pizza because it is spicy.', 'TEXT');
    expect(retried.progress.currentTurnNumber).toBe(2);
    expect((await submitScenarioTalkMessage(session.sessionId, 'I ate pasta yesterday.', 'TEXT')).progress.completed).toBe(true);
  });

  it('중도 종료한 첫 무료 대화는 같은 세션을 반환하고 다른 대화는 차단한다', async () => {
    const session = await startScenarioTalkSession(scenarioId);
    await submitScenarioTalkMessage(session.sessionId, 'I like pizza because it is spicy.', 'TEXT');
    await endSession(session.sessionId);
    const resumed = await startScenarioTalkSession(scenarioId);
    expect(resumed.sessionId).toBe(session.sessionId);
    expect(resumed.progress.currentTurnNumber).toBe(2);
    await expect(startScenarioTalkSession(otherScenario)).rejects.toMatchObject({ code: 'PREMIUM_REQUIRED' });
    expect((await submitScenarioTalkMessage(session.sessionId, 'I ate pasta yesterday.', 'TEXT')).progress.completed).toBe(true);
  });

  it('구매 취소는 이동하지 않고, 활성 구독 복원은 실제 서버 조회 후 열린다', async () => {
    const unlocked = vi.fn();
    mocks.purchase.mockResolvedValue({ status: 'cancelled' });
    mocks.restore.mockResolvedValue({ status: 'success' });
    const purchase = renderHook(() => usePurchase({ pricing: {}, onUnlocked: unlocked }), { wrapper });
    await act(() => purchase.result.current.purchase('monthly'));
    expect(unlocked).not.toHaveBeenCalled();
    await webhook();
    await act(() => purchase.result.current.restore());
    expect(unlocked).toHaveBeenCalledOnce();
  });
});

describe('결제 ON — 기존 FE의 경계 동작과 오류 재현', () => {
  it('웹훅 지연: 표현 목록 복귀는 성공하고 학습 재진입은 다시 페이월로 막힌다', async () => {
    const before = await gate();
    before.unmount();
    mocks.purchase.mockResolvedValue({ status: 'success' });
    const unlocked = vi.fn();
    const purchase = renderHook(() => usePurchase({ pricing: {}, onUnlocked: unlocked }), { wrapper });
    subscriptionCalls = 0;
    await act(() => purchase.result.current.purchase('monthly'));
    expect(subscriptionCalls).toBe(5);
    expect(unlocked).toHaveBeenCalledOnce();
    expect((await getMySubscription()).premium).toBe(false);
    // ExpressionBranch가 실제 복귀 지점이다. 공개 목록을 읽은 뒤 guard를 다시 거친다.
    expect(await getExpressions(scenarioId)).toHaveLength(1);
    const after = await gate();
    const enterLearning = vi.fn();
    act(() => after.result.current.guard(enterLearning, {
      entry: 'expression', returnTo: scenarioExpressionBranchPath(scenarioId),
    }));
    expect(enterLearning).not.toHaveBeenCalled();
    expect(mocks.push).toHaveBeenCalledWith(expect.stringContaining('/paywall'));
    await webhook();
    after.unmount();
    const confirmed = await gate();
    act(() => confirmed.result.current.guard(enterLearning, { entry: 'expression' }));
    expect(enterLearning).toHaveBeenCalledOnce();
    expect((await getExpressionLearning(scenarioId)).expressionId).toBe(scenarioId);
  });

  it('구독 조회 실패: FE가 진입을 허용하지만 서버는 403으로 차단한다', async () => {
    failSubscription = true;
    const hook = renderHook(() => usePaywallGate(), { wrapper });
    await waitFor(() => expect(query.getQueryState(subscriptionKeys.mine(account.user.userId))?.status).toBe('error'));
    const go = vi.fn();
    act(() => hook.result.current.guard(go, { entry: 'expression' }));
    expect(go).toHaveBeenCalledOnce();
    expect(mocks.push).not.toHaveBeenCalled();
    await expect(getExpressionLearning(scenarioId)).rejects.toMatchObject({ status: 403, code: 'PREMIUM_REQUIRED' });
  });

  it.each([null, { ...shell, appVersion: '1.2.9' }])('브라우저·구 앱은 FE 게이트를 통과하지만 서버는 표현 진입을 차단한다: %j', async context => {
    window.__LANDIT_NATIVE__ = context;
    const hook = renderHook(() => usePaywallGate(), { wrapper });
    const go = vi.fn();
    act(() => hook.result.current.guard(go, { entry: 'expression' }));
    expect(go).toHaveBeenCalledOnce();
    await expect(getExpressionLearning(scenarioId)).rejects.toMatchObject({ status: 403, code: 'PREMIUM_REQUIRED' });
  });
});
