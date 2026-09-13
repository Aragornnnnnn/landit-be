# 구·신 AI 라우터를 실행하며 외부 LLM만 결정적인 테스트 응답으로 바꾼다.
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
from test_conversation_api import FakeOpenAI, good_message_feedback

current_client = contextvars.ContextVar('compatibility_llm')
patcher = patch('app.core.openai_client.OpenAI', side_effect=lambda **_: current_client.get())
patcher.start()
app = create_app(Settings(_env_file=None, app_env='local', openrouter_api_key='local-test-only',
                          openrouter_model='local-test', sentry_dsn=''))


@app.post('/__test__/clear-cache')
def clear_cache():
    clear_message_feedback_cache()
    return {'cleared': True}


@app.middleware('http')
async def external_llm_fixture(request, call_next):
    if not request.url.path.startswith('/api/v1/conversation/'):
        return await call_next(request)
    payload = await request.json()
    if request.url.path.endswith('/message-feedback'):
        fake = FakeOpenAI(message_feedback=good_message_feedback(payload['messageId']))
    else:
        fake = FakeOpenAI(content=json.dumps({
            'sessionId': payload['sessionId'],
            'highlightMessage': '의도를 잘 전달했어요.',
            'summaryMessage': '질문에 맞춰 대답했어요.',
        }))
    token = current_client.set(fake)
    try:
        return await call_next(request)
    finally:
        current_client.reset(token)
