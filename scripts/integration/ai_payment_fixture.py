# 실제 AI HTTP 경로를 실행하면서 외부 LLM 호출만 결정적인 응답으로 대체한다.
import contextvars
import json
import os
import sys
from unittest.mock import patch

sys.path.insert(0, os.environ['LAN474_AI_ROOT'])
sys.path.insert(0, os.path.join(os.environ['LAN474_AI_ROOT'], 'tests'))
from app.main import create_app
from app.core.config import Settings
from app.conversation.application.next_message_service import clear_message_feedback_cache
from test_conversation_api import FakeOpenAI, good_message_feedback, valid_level_assessment

client = contextvars.ContextVar('payment_test_llm')
patcher = patch('app.core.openai_client.OpenAI', side_effect=lambda **_: client.get())
patcher.start()
app = create_app(Settings(_env_file=None, app_env='local', openrouter_api_key='local-test-only', openrouter_model='local-test', sentry_dsn=''))
fail_next = False

@app.post('/__test__/clear-cache')
def clear_cache():
    clear_message_feedback_cache()
    return {'cleared': True}

@app.post('/__test__/fail-next')
def fail_once():
    global fail_next
    fail_next = True
    return {'armed': True}

@app.middleware('http')
async def external_llm_fixture(request, call_next):
    global fail_next
    path = request.url.path
    if not path.startswith('/api/v1/conversation/'):
        return await call_next(request)
    payload = await request.json()
    if path.endswith('/session-level-assessment'):
        from app.models.conversation import SessionLevelAssessmentRequest
        from pydantic import ValidationError
        try:
            SessionLevelAssessmentRequest.model_validate(payload)
        except ValidationError as exc:
            print('LEVEL_REQUEST_ERRORS', [(e['loc'], e['type']) for e in exc.errors()], flush=True)
    response = {
        'acknowledgement': 'Sounds tasty.',
        'translatedAcknowledgement': '맛있겠다.',
        'goalCompletionStatus': 'PARTIAL',
        'innerThought': '의도를 분명하게 전달했네.',
        'innerThoughtType': 'GOOD',
        'answerCoverage': 'COMPLETE', 'relationshipTone': 'WARM', 'directedAttack': False,
        'aiMessage': 'Sounds good. See you soon.',
        'translatedMessage': '좋아. 다음에 봐.',
        'sessionId': payload['sessionId'],
        'highlightMessage': '의도를 잘 전달했어요.',
        'summaryMessage': '질문에 맞춰 대답했어요.',
    }
    if path.endswith('/session-level-assessment'):
        assessment = valid_level_assessment()
        template = assessment['core']['messages'][0]
        assessment['core']['messages'] = []
        for message in payload['assessmentMessages']:
            item = json.loads(json.dumps(template))
            item['messageId'] = message['messageId']
            for domain in item['domains'].values():
                domain['evidenceExcerpt'] = message['userMessage']
            assessment['core']['messages'].append(item)
        response['levelAssessment'] = assessment
    fields = {
        'next-message': ['acknowledgement', 'translatedAcknowledgement', 'goalCompletionStatus'],
        'inner-thought': ['answerCoverage', 'relationshipTone', 'directedAttack', 'innerThought', 'innerThoughtType'],
        'closing-message': ['aiMessage', 'translatedMessage', 'innerThought', 'innerThoughtType'],
        'session-feedback': ['sessionId', 'highlightMessage', 'summaryMessage'],
        'session-level-assessment': ['sessionId', 'levelAssessment'],
    }.get(path.rsplit('/', 1)[-1], [])
    fake = FakeOpenAI(content=json.dumps({key: response[key] for key in fields}))
    if path.endswith('/message-feedback'):
        fake = FakeOpenAI(message_feedback=good_message_feedback(payload['messageId']))
    if fail_next and path.endswith('/next-message'):
        fail_next = False
        fake = FakeOpenAI(error=RuntimeError('injected local LLM failure'))
    token = client.set(fake)
    try:
        return await call_next(request)
    finally:
        client.reset(token)
