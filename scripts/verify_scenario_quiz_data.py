# LAN-391 시나리오 예문 1·2의 칩 구성, 변경 범위 및 대체 정답 회귀를 검증한다.
import argparse
from collections import Counter
from functools import lru_cache
import json
from pathlib import Path
import re
import subprocess
import unicodedata

ROOT = Path(__file__).resolve().parents[1]
MIGRATION = 'src/main/resources/db/postgresql/V136__insert_scenario_41_70_writing_expressions.sql'
PAYLOAD = re.compile(r"^   '(\[.*\])'::jsonb,", re.M)
BASELINE = 'e7df116dcfd95b174e3ea57f036a9336507ddf52'


def read_payloads(sql):
    ids = [int(v) for v in re.findall(r'^  \((\d+),', sql, re.M)]
    payloads = [json.loads(m[1].replace("''", "'")) for m in PAYLOAD.finditer(sql)]
    assert ids == list(range(4001, 4329)) and len(payloads) == 328
    return dict(zip(ids, payloads))


def tokens(text):
    text = unicodedata.normalize('NFKC', text).replace('’', "'").replace('‘', "'")
    return re.findall(r"[A-Za-z0-9]+(?:['-][A-Za-z0-9]+)*(?:')?", text)


def buildable(sentence, bank):
    target = tuple(w.lower() for w in tokens(sentence))
    items = tuple(Counter(w.lower() for w in bank).items())
    chunks = tuple(tuple(w.split()) for w, _ in items)

    @lru_cache(None)
    def solve(index, counts):
        if index == len(target):
            return True
        for position, chunk in enumerate(chunks):
            if counts[position] and target[index:index + len(chunk)] == chunk:
                remaining = list(counts)
                remaining[position] -= 1
                if solve(index + len(chunk), tuple(remaining)):
                    return True
        return False

    return solve(0, tuple(count for _, count in items))


def verify_sentence(key, sentence):
    for words_key, choices_key in [('sentenceWords', 'sentenceWordChoices'),
                                   ('sentenceTranslateWords', 'sentenceTranslateWordChoices')]:
        words, choices = Counter(sentence[words_key]), Counter(sentence[choices_key])
        assert not words - choices, (key, words_key, 'missing correct chips')
        assert sum((choices - words).values()) == 3, (key, choices_key, 'distractor count')
    assert tokens(sentence['sentenceText']) == tokens(' '.join(sentence['sentenceWords'])), key
    assert sentence['highlightingPart'] in sentence['sentenceText'], key
    answers = sentence['sentenceTranslateAcceptedAnswers']
    assert answers and answers[0] == sentence['sentenceTranslateWords'], key
    assert len({tuple(v) for v in answers}) == len(answers), key
    for answer in answers:
        assert Counter(answer) == Counter(sentence['sentenceTranslateWords']), key
    assert buildable(sentence['sentenceText'], sentence['sentenceWordChoices']), key


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--baseline-ref', default=BASELINE)
    args = parser.parse_args()
    sql = (ROOT / MIGRATION).read_text()
    baseline_sql = subprocess.check_output(
        ['git', 'show', f'{args.baseline_ref}:{MIGRATION}'], cwd=ROOT, text=True)
    assert PAYLOAD.sub('<payload>', sql) == PAYLOAD.sub('<payload>', baseline_sql), 'metadata changed'
    rows, baseline = read_payloads(sql), read_payloads(baseline_sql)
    common = {'sentenceWords', 'sentenceWordChoices', 'sentenceTranslateAcceptedAnswers'}
    exceptions = {
        (4061, 1): {'sentenceText'},
        (4120, 2): {'sentenceTranslation', 'sentenceTranslateWords', 'sentenceTranslateWordChoices'},
        (4141, 1): {'sentenceTranslateWordChoices'},
        (4249, 1): {'practiceQuestion', 'practiceQuestionTranslation'},
        (4259, 1): {'practiceQuestion', 'practiceQuestionTranslation'},
    }
    for rid, examples in rows.items():
        assert len(examples) == 4 and examples[2:] == baseline[rid][2:], rid
        for number, sentence in enumerate(examples[:2], 1):
            key = (rid, number)
            before = baseline[rid][number - 1]
            changed = {k for k in before.keys() | sentence.keys() if before.get(k) != sentence.get(k)}
            assert not changed - common - exceptions.get(key, set()), (key, changed)
            verify_sentence(key, sentence)
    cases = json.loads((ROOT / 'docs/tasks/LAN-391/quiz-regressions.json').read_text())
    for case in cases:
        rid, number = map(int, case['key'].split('.'))
        assert buildable(case['alternative'], baseline[rid][number - 1]['sentenceWordChoices']), case
        assert not buildable(case['alternative'], rows[rid][number - 1]['sentenceWordChoices']), case
    print(f'PASS: 328 expressions, 656 sentences, {len(cases)} blocked alternatives; scope preserved.')


if __name__ == '__main__':
    main()
