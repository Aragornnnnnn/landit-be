-- LAN-584에서 승인된 예외 12건의 질문·본문·번역과 연결된 퀴즈 배열을 교정한다.
-- V139 영어 칩 교정 이후 상태를 확인하고 그 밖의 필드와 예문은 보존한다.
LOCK TABLE writing_expression IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE lan584_text_updates (
    expression_id BIGINT PRIMARY KEY,
    example_number INTEGER NOT NULL CHECK (example_number IN (1, 2)),
    expression_source TEXT NOT NULL,
    expected_example JSONB NOT NULL,
    changes JSONB NOT NULL
) ON COMMIT DROP;

INSERT INTO lan584_text_updates
SELECT (value ->> 'expressionId')::BIGINT, (value ->> 'exampleNumber')::INTEGER,
       value ->> 'expressionSource', value -> 'expected', value -> 'changes'
FROM jsonb_array_elements($lan584_text$
[
  {
    "expressionId": 435,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 마르코 → 발언권을.",
    "expected": {
      "sentenceText": "Without further ado, I'll hand it over to our first speaker.",
      "sentenceWords": [
        "Without",
        "further",
        "ado",
        "I'll",
        "hand",
        "it",
        "over",
        "to",
        "our",
        "first",
        "speaker"
      ],
      "highlightingPart": "Without further ado",
      "practiceQuestion": "Everyone's seated, so shall we begin?",
      "sentenceTranslation": "지체 없이, 첫 발표자께 마르코 넘기겠습니다.",
      "sentenceWordChoices": [
        "to",
        "our",
        "hand",
        "hands",
        "Without",
        "speaker",
        "I'll",
        "ado",
        "adieu",
        "it",
        "farther",
        "over",
        "first",
        "further"
      ],
      "sentenceTranslateWords": [
        "지체",
        "없이",
        "첫",
        "발표자께",
        "마르코",
        "넘기겠습니다"
      ],
      "practiceQuestionTranslation": "다들 앉으셨는데, 시작할까요?",
      "sentenceTranslateWordChoices": [
        "없이",
        "첫",
        "발표자께",
        "받겠습니다",
        "마르코",
        "넘기겠습니다",
        "천천히",
        "지체",
        "마지막"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "지체",
          "없이",
          "첫",
          "발표자께",
          "마르코",
          "넘기겠습니다"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "지체 없이, 첫 발표자께 발언권을 넘기겠습니다.",
      "sentenceTranslateWords": [
        "지체",
        "없이",
        "첫",
        "발표자께",
        "발언권을",
        "넘기겠습니다"
      ],
      "sentenceTranslateWordChoices": [
        "없이",
        "첫",
        "발표자께",
        "받겠습니다",
        "발언권을",
        "넘기겠습니다",
        "천천히",
        "지체",
        "마지막"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "지체",
          "없이",
          "첫",
          "발표자께",
          "발언권을",
          "넘기겠습니다"
        ]
      ]
    }
  },
  {
    "expressionId": 910,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 마르코 → 마이크.",
    "expected": {
      "sentenceText": "My professor called it a mic drop ending.",
      "sentenceWords": [
        "My",
        "professor",
        "called",
        "it",
        "a",
        "mic",
        "drop",
        "ending"
      ],
      "highlightingPart": "mic drop",
      "practiceQuestion": "How was the last line of your essay?",
      "sentenceTranslation": "교수님이 마르코 드롭급 마무리라고 하셨어.",
      "sentenceWordChoices": [
        "My",
        "it",
        "professor",
        "call",
        "ending",
        "called",
        "drop",
        "drops",
        "a",
        "mic",
        "mics"
      ],
      "sentenceTranslateWords": [
        "교수님이",
        "마르코",
        "드롭급",
        "마무리라고",
        "하셨어"
      ],
      "practiceQuestionTranslation": "네 에세이 마지막 문장 어땠어?",
      "sentenceTranslateWordChoices": [
        "마르코",
        "들었어",
        "친구가",
        "하셨어",
        "마무리라고",
        "시작이라고",
        "드롭급",
        "교수님이"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "교수님이",
          "마르코",
          "드롭급",
          "마무리라고",
          "하셨어"
        ],
        [
          "마르코",
          "드롭급",
          "마무리라고",
          "교수님이",
          "하셨어"
        ],
        [
          "교수님이",
          "하셨어",
          "마르코",
          "드롭급",
          "마무리라고"
        ],
        [
          "마르코",
          "드롭급",
          "마무리라고",
          "하셨어",
          "교수님이"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "교수님이 마이크 드롭급 마무리라고 하셨어.",
      "sentenceTranslateWords": [
        "교수님이",
        "마이크",
        "드롭급",
        "마무리라고",
        "하셨어"
      ],
      "sentenceTranslateWordChoices": [
        "마이크",
        "들었어",
        "친구가",
        "하셨어",
        "마무리라고",
        "시작이라고",
        "드롭급",
        "교수님이"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "교수님이",
          "마이크",
          "드롭급",
          "마무리라고",
          "하셨어"
        ],
        [
          "마이크",
          "드롭급",
          "마무리라고",
          "교수님이",
          "하셨어"
        ],
        [
          "교수님이",
          "하셨어",
          "마이크",
          "드롭급",
          "마무리라고"
        ],
        [
          "마이크",
          "드롭급",
          "마무리라고",
          "하셨어",
          "교수님이"
        ]
      ]
    }
  },
  {
    "expressionId": 1254,
    "exampleNumber": 1,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 말을 → 노래를.",
    "expected": {
      "sentenceText": "He broke into a smile when he heard it.",
      "sentenceWords": [
        "He",
        "broke",
        "into",
        "a",
        "smile",
        "when",
        "he",
        "heard",
        "it"
      ],
      "highlightingPart": "broke into",
      "practiceQuestion": "What happened when he heard the old song?",
      "sentenceTranslation": "그 말을 듣자 그는 갑자기 미소를 지었어.",
      "sentenceWordChoices": [
        "He",
        "broke",
        "a",
        "smile",
        "she",
        "into",
        "from",
        "an",
        "heard",
        "he",
        "it",
        "when"
      ],
      "sentenceTranslateWords": [
        "그",
        "말을",
        "듣자",
        "그는",
        "갑자기",
        "미소를",
        "지었어"
      ],
      "practiceQuestionTranslation": "그가 옛 노래를 들었을 때 어떻게 됐어?",
      "sentenceTranslateWordChoices": [
        "갑자기",
        "그는",
        "그",
        "감췄어",
        "미소를",
        "보자",
        "눈물을",
        "듣자",
        "지었어",
        "말을"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "그",
          "말을",
          "듣자",
          "그는",
          "갑자기",
          "미소를",
          "지었어"
        ],
        [
          "그",
          "말을",
          "듣자",
          "갑자기",
          "그는",
          "미소를",
          "지었어"
        ],
        [
          "그",
          "말을",
          "듣자",
          "그는",
          "미소를",
          "갑자기",
          "지었어"
        ],
        [
          "그",
          "말을",
          "듣자",
          "갑자기",
          "미소를",
          "그는",
          "지었어"
        ],
        [
          "그",
          "말을",
          "듣자",
          "그는",
          "갑자기",
          "지었어",
          "미소를"
        ],
        [
          "그",
          "말을",
          "듣자",
          "그는",
          "미소를",
          "지었어",
          "갑자기"
        ],
        [
          "그",
          "말을",
          "듣자",
          "갑자기",
          "그는",
          "지었어",
          "미소를"
        ],
        [
          "그",
          "말을",
          "듣자",
          "갑자기",
          "미소를",
          "지었어",
          "그는"
        ],
        [
          "그는",
          "갑자기",
          "미소를",
          "지었어",
          "그",
          "말을",
          "듣자"
        ],
        [
          "갑자기",
          "그는",
          "미소를",
          "지었어",
          "그",
          "말을",
          "듣자"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "그 노래를 듣자 그는 갑자기 미소를 지었어.",
      "sentenceTranslateWords": [
        "그",
        "노래를",
        "듣자",
        "그는",
        "갑자기",
        "미소를",
        "지었어"
      ],
      "sentenceTranslateWordChoices": [
        "갑자기",
        "그는",
        "그",
        "감췄어",
        "미소를",
        "보자",
        "눈물을",
        "듣자",
        "지었어",
        "노래를"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "그",
          "노래를",
          "듣자",
          "그는",
          "갑자기",
          "미소를",
          "지었어"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "갑자기",
          "그는",
          "미소를",
          "지었어"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "그는",
          "미소를",
          "갑자기",
          "지었어"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "갑자기",
          "미소를",
          "그는",
          "지었어"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "그는",
          "갑자기",
          "지었어",
          "미소를"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "그는",
          "미소를",
          "지었어",
          "갑자기"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "갑자기",
          "그는",
          "지었어",
          "미소를"
        ],
        [
          "그",
          "노래를",
          "듣자",
          "갑자기",
          "미소를",
          "지었어",
          "그는"
        ],
        [
          "그는",
          "갑자기",
          "미소를",
          "지었어",
          "그",
          "노래를",
          "듣자"
        ],
        [
          "갑자기",
          "그는",
          "미소를",
          "지었어",
          "그",
          "노래를",
          "듣자"
        ]
      ]
    }
  },
  {
    "expressionId": 1269,
    "exampleNumber": 1,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 마르코을 → 마르코를.",
    "expected": {
      "sentenceText": "The teacher is about to call on Marco.",
      "sentenceWords": [
        "The",
        "teacher",
        "is",
        "about",
        "to",
        "call",
        "on",
        "Marco"
      ],
      "highlightingPart": "call on",
      "practiceQuestion": "Why is the teacher looking at Marco?",
      "sentenceTranslation": "선생님이 곧 마르코을 지목하려고 해.",
      "sentenceWordChoices": [
        "is",
        "about",
        "on",
        "abouts",
        "The",
        "are",
        "call",
        "Marco",
        "in",
        "teacher",
        "to"
      ],
      "sentenceTranslateWords": [
        "선생님이",
        "곧",
        "마르코을",
        "지목하려고",
        "해"
      ],
      "practiceQuestionTranslation": "선생님이 왜 마르코을 보고 있어?",
      "sentenceTranslateWordChoices": [
        "학생이",
        "해",
        "이미",
        "칭찬하려고",
        "곧",
        "지목하려고",
        "선생님이",
        "마르코을"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "선생님이",
          "곧",
          "마르코을",
          "지목하려고",
          "해"
        ],
        [
          "곧",
          "선생님이",
          "마르코을",
          "지목하려고",
          "해"
        ],
        [
          "선생님이",
          "마르코을",
          "곧",
          "지목하려고",
          "해"
        ],
        [
          "곧",
          "마르코을",
          "선생님이",
          "지목하려고",
          "해"
        ],
        [
          "마르코을",
          "선생님이",
          "곧",
          "지목하려고",
          "해"
        ],
        [
          "마르코을",
          "곧",
          "선생님이",
          "지목하려고",
          "해"
        ],
        [
          "선생님이",
          "곧",
          "지목하려고",
          "해",
          "마르코을"
        ],
        [
          "선생님이",
          "마르코을",
          "지목하려고",
          "해",
          "곧"
        ],
        [
          "곧",
          "선생님이",
          "지목하려고",
          "해",
          "마르코을"
        ],
        [
          "곧",
          "마르코을",
          "지목하려고",
          "해",
          "선생님이"
        ],
        [
          "마르코을",
          "선생님이",
          "지목하려고",
          "해",
          "곧"
        ],
        [
          "마르코을",
          "곧",
          "지목하려고",
          "해",
          "선생님이"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "선생님이 곧 마르코를 지목하려고 해.",
      "sentenceTranslateWords": [
        "선생님이",
        "곧",
        "마르코를",
        "지목하려고",
        "해"
      ],
      "practiceQuestionTranslation": "선생님이 왜 마르코를 보고 있어?",
      "sentenceTranslateWordChoices": [
        "학생이",
        "해",
        "이미",
        "칭찬하려고",
        "곧",
        "지목하려고",
        "선생님이",
        "마르코를"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "선생님이",
          "곧",
          "마르코를",
          "지목하려고",
          "해"
        ],
        [
          "곧",
          "선생님이",
          "마르코를",
          "지목하려고",
          "해"
        ],
        [
          "선생님이",
          "마르코를",
          "곧",
          "지목하려고",
          "해"
        ],
        [
          "곧",
          "마르코를",
          "선생님이",
          "지목하려고",
          "해"
        ],
        [
          "마르코를",
          "선생님이",
          "곧",
          "지목하려고",
          "해"
        ],
        [
          "마르코를",
          "곧",
          "선생님이",
          "지목하려고",
          "해"
        ],
        [
          "선생님이",
          "곧",
          "지목하려고",
          "해",
          "마르코를"
        ],
        [
          "선생님이",
          "마르코를",
          "지목하려고",
          "해",
          "곧"
        ],
        [
          "곧",
          "선생님이",
          "지목하려고",
          "해",
          "마르코를"
        ],
        [
          "곧",
          "마르코를",
          "지목하려고",
          "해",
          "선생님이"
        ],
        [
          "마르코를",
          "선생님이",
          "지목하려고",
          "해",
          "곧"
        ],
        [
          "마르코를",
          "곧",
          "지목하려고",
          "해",
          "선생님이"
        ]
      ]
    }
  },
  {
    "expressionId": 1462,
    "exampleNumber": 1,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 마르코이 → 마르코가.",
    "expected": {
      "sentenceText": "Yes, Marco washed up after lunch.",
      "sentenceWords": [
        "Yes",
        "Marco",
        "washed",
        "up",
        "after",
        "lunch"
      ],
      "highlightingPart": "washed up",
      "practiceQuestion": "Is the kitchen clean now?",
      "sentenceTranslation": "응, 마르코이 점심 뒤에 설거지했어.",
      "sentenceWordChoices": [
        "Marco",
        "before",
        "ye",
        "up",
        "after",
        "washed",
        "Yes",
        "down",
        "lunch"
      ],
      "sentenceTranslateWords": [
        "응",
        "마르코이",
        "점심",
        "뒤에",
        "설거지했어"
      ],
      "practiceQuestionTranslation": "이제 주방 깨끗해?",
      "sentenceTranslateWordChoices": [
        "응",
        "뒤에",
        "전에",
        "클로이가",
        "요리했어",
        "설거지했어",
        "점심",
        "마르코이"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "응",
          "마르코이",
          "점심",
          "뒤에",
          "설거지했어"
        ],
        [
          "응",
          "점심",
          "뒤에",
          "마르코이",
          "설거지했어"
        ],
        [
          "응",
          "점심",
          "뒤에",
          "설거지했어",
          "마르코이"
        ],
        [
          "응",
          "마르코이",
          "설거지했어",
          "점심",
          "뒤에"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "응, 마르코가 점심 뒤에 설거지했어.",
      "sentenceTranslateWords": [
        "응",
        "마르코가",
        "점심",
        "뒤에",
        "설거지했어"
      ],
      "sentenceTranslateWordChoices": [
        "응",
        "뒤에",
        "전에",
        "클로이가",
        "요리했어",
        "설거지했어",
        "점심",
        "마르코가"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "응",
          "마르코가",
          "점심",
          "뒤에",
          "설거지했어"
        ],
        [
          "응",
          "점심",
          "뒤에",
          "마르코가",
          "설거지했어"
        ],
        [
          "응",
          "점심",
          "뒤에",
          "설거지했어",
          "마르코가"
        ],
        [
          "응",
          "마르코가",
          "설거지했어",
          "점심",
          "뒤에"
        ]
      ]
    }
  },
  {
    "expressionId": 1467,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 마르코이 → 마르코가.",
    "expected": {
      "sentenceText": "Marco cleared everything away after lunch.",
      "sentenceWords": [
        "Marco",
        "cleared",
        "everything",
        "away",
        "after",
        "lunch"
      ],
      "highlightingPart": "cleared everything away",
      "practiceQuestion": "Why is the table already empty?",
      "sentenceTranslation": "마르코이 점심 뒤에 전부 치웠어.",
      "sentenceWordChoices": [
        "lunch",
        "cleared",
        "clear",
        "Marco",
        "everything",
        "after",
        "away",
        "before"
      ],
      "sentenceTranslateWords": [
        "마르코이",
        "점심",
        "뒤에",
        "전부",
        "치웠어"
      ],
      "practiceQuestionTranslation": "왜 벌써 식탁이 비었어?",
      "sentenceTranslateWordChoices": [
        "뒤에",
        "치웠어",
        "클로이가",
        "전에",
        "조금",
        "마르코이",
        "전부",
        "점심"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "마르코이",
          "점심",
          "뒤에",
          "전부",
          "치웠어"
        ],
        [
          "점심",
          "뒤에",
          "마르코이",
          "전부",
          "치웠어"
        ],
        [
          "마르코이",
          "전부",
          "점심",
          "뒤에",
          "치웠어"
        ],
        [
          "점심",
          "뒤에",
          "전부",
          "마르코이",
          "치웠어"
        ],
        [
          "전부",
          "마르코이",
          "점심",
          "뒤에",
          "치웠어"
        ],
        [
          "전부",
          "점심",
          "뒤에",
          "마르코이",
          "치웠어"
        ],
        [
          "마르코이",
          "점심",
          "뒤에",
          "치웠어",
          "전부"
        ],
        [
          "마르코이",
          "전부",
          "치웠어",
          "점심",
          "뒤에"
        ],
        [
          "점심",
          "뒤에",
          "마르코이",
          "치웠어",
          "전부"
        ],
        [
          "점심",
          "뒤에",
          "전부",
          "치웠어",
          "마르코이"
        ],
        [
          "전부",
          "마르코이",
          "치웠어",
          "점심",
          "뒤에"
        ],
        [
          "전부",
          "점심",
          "뒤에",
          "치웠어",
          "마르코이"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "마르코가 점심 뒤에 전부 치웠어.",
      "sentenceTranslateWords": [
        "마르코가",
        "점심",
        "뒤에",
        "전부",
        "치웠어"
      ],
      "sentenceTranslateWordChoices": [
        "뒤에",
        "치웠어",
        "클로이가",
        "전에",
        "조금",
        "마르코가",
        "전부",
        "점심"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "마르코가",
          "점심",
          "뒤에",
          "전부",
          "치웠어"
        ],
        [
          "점심",
          "뒤에",
          "마르코가",
          "전부",
          "치웠어"
        ],
        [
          "마르코가",
          "전부",
          "점심",
          "뒤에",
          "치웠어"
        ],
        [
          "점심",
          "뒤에",
          "전부",
          "마르코가",
          "치웠어"
        ],
        [
          "전부",
          "마르코가",
          "점심",
          "뒤에",
          "치웠어"
        ],
        [
          "전부",
          "점심",
          "뒤에",
          "마르코가",
          "치웠어"
        ],
        [
          "마르코가",
          "점심",
          "뒤에",
          "치웠어",
          "전부"
        ],
        [
          "마르코가",
          "전부",
          "치웠어",
          "점심",
          "뒤에"
        ],
        [
          "점심",
          "뒤에",
          "마르코가",
          "치웠어",
          "전부"
        ],
        [
          "점심",
          "뒤에",
          "전부",
          "치웠어",
          "마르코가"
        ],
        [
          "전부",
          "마르코가",
          "치웠어",
          "점심",
          "뒤에"
        ],
        [
          "전부",
          "점심",
          "뒤에",
          "치웠어",
          "마르코가"
        ]
      ]
    }
  },
  {
    "expressionId": 1497,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "상대방이 재제출 허락 여부를 묻는 질문으로 화자 관계를 맞춘다.",
    "expected": {
      "sentenceText": "The teacher agreed to give me a second chance.",
      "sentenceWords": [
        "The",
        "teacher",
        "agreed",
        "to",
        "give",
        "me",
        "a",
        "second",
        "chance"
      ],
      "highlightingPart": "give me a second chance",
      "practiceQuestion": "Can I redo the assignment?",
      "sentenceTranslation": "선생님이 내게 다시 기회를 주셨어.",
      "sentenceWordChoices": [
        "an",
        "teacher",
        "agreed",
        "give",
        "me",
        "The",
        "second",
        "to",
        "chance",
        "agre",
        "a"
      ],
      "sentenceTranslateWords": [
        "선생님이",
        "내게",
        "다시",
        "기회를",
        "주셨어"
      ],
      "practiceQuestionTranslation": "과제를 다시 해도 될까요?",
      "sentenceTranslateWordChoices": [
        "기회를",
        "네게",
        "선생님이",
        "내게",
        "거절하셨어",
        "주셨어",
        "친구가",
        "다시"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "선생님이",
          "내게",
          "다시",
          "기회를",
          "주셨어"
        ],
        [
          "내게",
          "선생님이",
          "다시",
          "기회를",
          "주셨어"
        ],
        [
          "선생님이",
          "다시",
          "내게",
          "기회를",
          "주셨어"
        ],
        [
          "선생님이",
          "내게",
          "기회를",
          "다시",
          "주셨어"
        ],
        [
          "내게",
          "다시",
          "선생님이",
          "기회를",
          "주셨어"
        ],
        [
          "내게",
          "선생님이",
          "기회를",
          "다시",
          "주셨어"
        ],
        [
          "내게",
          "다시",
          "기회를",
          "선생님이",
          "주셨어"
        ],
        [
          "선생님이",
          "기회를",
          "내게",
          "다시",
          "주셨어"
        ],
        [
          "선생님이",
          "기회를",
          "다시",
          "내게",
          "주셨어"
        ],
        [
          "다시",
          "선생님이",
          "내게",
          "기회를",
          "주셨어"
        ],
        [
          "다시",
          "내게",
          "선생님이",
          "기회를",
          "주셨어"
        ],
        [
          "선생님이",
          "내게",
          "다시",
          "주셨어",
          "기회를"
        ],
        [
          "선생님이",
          "내게",
          "기회를",
          "주셨어",
          "다시"
        ],
        [
          "선생님이",
          "다시",
          "내게",
          "주셨어",
          "기회를"
        ],
        [
          "선생님이",
          "다시",
          "기회를",
          "주셨어",
          "내게"
        ],
        [
          "선생님이",
          "기회를",
          "내게",
          "주셨어",
          "다시"
        ],
        [
          "선생님이",
          "기회를",
          "다시",
          "주셨어",
          "내게"
        ],
        [
          "내게",
          "다시",
          "기회를",
          "주셨어",
          "선생님이"
        ],
        [
          "내게",
          "기회를",
          "다시",
          "주셨어",
          "선생님이"
        ]
      ]
    },
    "changes": {
      "practiceQuestion": "What did the teacher say about redoing the assignment?",
      "practiceQuestionTranslation": "과제를 다시 하는 것에 대해 선생님이 뭐라고 하셨어?"
    }
  },
  {
    "expressionId": 1508,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 리엄이 → 마르코가.",
    "expected": {
      "sentenceText": "Marco went out of his way to locate a copy.",
      "sentenceWords": [
        "Marco",
        "went",
        "out",
        "of",
        "his",
        "way",
        "to",
        "locate",
        "a",
        "copy"
      ],
      "highlightingPart": "went out of his way",
      "practiceQuestion": "How did you find that rare book?",
      "sentenceTranslation": "리엄이 사본을 찾으려고 애써 줬어.",
      "sentenceWordChoices": [
        "went",
        "way",
        "of",
        "to",
        "a",
        "her",
        "his",
        "an",
        "copy",
        "Marco",
        "locate",
        "out",
        "outed"
      ],
      "sentenceTranslateWords": [
        "리엄이",
        "사본을",
        "찾으려고",
        "애써",
        "줬어"
      ],
      "practiceQuestionTranslation": "그 희귀한 책을 어떻게 찾았어?",
      "sentenceTranslateWordChoices": [
        "줬어",
        "찾으려고",
        "원본을",
        "사본을",
        "리엄이",
        "버리려고",
        "애써",
        "클로이가"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "리엄이",
          "사본을",
          "찾으려고",
          "애써",
          "줬어"
        ],
        [
          "사본을",
          "찾으려고",
          "리엄이",
          "애써",
          "줬어"
        ],
        [
          "리엄이",
          "애써",
          "줬어",
          "사본을",
          "찾으려고"
        ],
        [
          "사본을",
          "찾으려고",
          "애써",
          "줬어",
          "리엄이"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "마르코가 사본을 찾으려고 애써 줬어.",
      "sentenceTranslateWords": [
        "마르코가",
        "사본을",
        "찾으려고",
        "애써",
        "줬어"
      ],
      "sentenceTranslateWordChoices": [
        "줬어",
        "찾으려고",
        "원본을",
        "사본을",
        "마르코가",
        "버리려고",
        "애써",
        "클로이가"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "마르코가",
          "사본을",
          "찾으려고",
          "애써",
          "줬어"
        ],
        [
          "사본을",
          "찾으려고",
          "마르코가",
          "애써",
          "줬어"
        ],
        [
          "마르코가",
          "애써",
          "줬어",
          "사본을",
          "찾으려고"
        ],
        [
          "사본을",
          "찾으려고",
          "애써",
          "줬어",
          "마르코가"
        ]
      ]
    }
  },
  {
    "expressionId": 1712,
    "exampleNumber": 2,
    "expressionSource": "FREE_TALK",
    "reason": "한국어 오역 또는 이름과 조사 교정: 에이버 → 클로이.",
    "expected": {
      "sentenceText": "There's more to Chloe than meets the eye.",
      "sentenceWords": [
        "There's",
        "more",
        "to",
        "Chloe",
        "than",
        "meets",
        "the",
        "eye"
      ],
      "highlightingPart": "There's more to Chloe than meets the eye",
      "practiceQuestion": "Is Chloe simply quiet?",
      "sentenceTranslation": "에이버에게는 겉보기보다 더 많은 면이 있어.",
      "sentenceWordChoices": [
        "eye",
        "meet",
        "less",
        "There's",
        "meets",
        "than",
        "Chloe",
        "to",
        "more",
        "the",
        "eyed"
      ],
      "sentenceTranslateWords": [
        "에이버에게는",
        "겉보기보다",
        "더",
        "많은",
        "면이",
        "있어"
      ],
      "practiceQuestionTranslation": "에이버는 그냥 조용한 사람이야?",
      "sentenceTranslateWordChoices": [
        "돈이",
        "면이",
        "없어",
        "적은",
        "겉보기보다",
        "있어",
        "에이버에게는",
        "더",
        "많은"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "에이버에게는",
          "겉보기보다",
          "더",
          "많은",
          "면이",
          "있어"
        ],
        [
          "겉보기보다",
          "에이버에게는",
          "더",
          "많은",
          "면이",
          "있어"
        ],
        [
          "에이버에게는",
          "더",
          "많은",
          "면이",
          "겉보기보다",
          "있어"
        ],
        [
          "에이버에게는",
          "겉보기보다",
          "있어",
          "더",
          "많은",
          "면이"
        ],
        [
          "에이버에게는",
          "더",
          "많은",
          "면이",
          "있어",
          "겉보기보다"
        ],
        [
          "겉보기보다",
          "에이버에게는",
          "있어",
          "더",
          "많은",
          "면이"
        ],
        [
          "겉보기보다",
          "더",
          "많은",
          "면이",
          "있어",
          "에이버에게는"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "클로이에게는 겉보기보다 더 많은 면이 있어.",
      "sentenceTranslateWords": [
        "클로이에게는",
        "겉보기보다",
        "더",
        "많은",
        "면이",
        "있어"
      ],
      "practiceQuestionTranslation": "클로이는 그냥 조용한 사람이야?",
      "sentenceTranslateWordChoices": [
        "돈이",
        "면이",
        "없어",
        "적은",
        "겉보기보다",
        "있어",
        "클로이에게는",
        "더",
        "많은"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "클로이에게는",
          "겉보기보다",
          "더",
          "많은",
          "면이",
          "있어"
        ],
        [
          "겉보기보다",
          "클로이에게는",
          "더",
          "많은",
          "면이",
          "있어"
        ],
        [
          "클로이에게는",
          "더",
          "많은",
          "면이",
          "겉보기보다",
          "있어"
        ],
        [
          "클로이에게는",
          "겉보기보다",
          "있어",
          "더",
          "많은",
          "면이"
        ],
        [
          "클로이에게는",
          "더",
          "많은",
          "면이",
          "있어",
          "겉보기보다"
        ],
        [
          "겉보기보다",
          "클로이에게는",
          "있어",
          "더",
          "많은",
          "면이"
        ],
        [
          "겉보기보다",
          "더",
          "많은",
          "면이",
          "있어",
          "클로이에게는"
        ]
      ]
    }
  },
  {
    "expressionId": 2026,
    "exampleNumber": 2,
    "expressionSource": "SCENARIO",
    "reason": "Maybe의 추측 의미를 번역과 한국어 퀴즈에 명시한다.",
    "expected": {
      "sentenceText": "Maybe someday.",
      "sentenceWords": [
        "Maybe",
        "someday"
      ],
      "highlightingPart": "someday",
      "practiceQuestion": "Will you live abroad?",
      "sentenceTranslation": "언젠가는.",
      "sentenceWordChoices": [
        "Maybe",
        "someday",
        "never",
        "Maybes",
        "sometimes"
      ],
      "sentenceTranslateWords": [
        "언젠가는"
      ],
      "practiceQuestionTranslation": "외국에서 살 거야?",
      "sentenceTranslateWordChoices": [
        "살았어",
        "절대",
        "언젠가는",
        "지금"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "언젠가는"
        ]
      ]
    },
    "changes": {
      "sentenceTranslation": "아마 언젠가는.",
      "sentenceTranslateWords": [
        "아마",
        "언젠가는"
      ],
      "sentenceTranslateWordChoices": [
        "살았어",
        "절대",
        "아마",
        "언젠가는",
        "지금"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "아마",
          "언젠가는"
        ],
        [
          "언젠가는",
          "아마"
        ]
      ]
    }
  },
  {
    "expressionId": 2115,
    "exampleNumber": 1,
    "expressionSource": "SCENARIO",
    "reason": "아침부터 계속되는 통증을 현재완료진행으로 교정하고 시간구 이동을 칩 결합으로 제한한다.",
    "expected": {
      "sentenceText": "My head hurts since this morning.",
      "sentenceWords": [
        "My",
        "head",
        "hurts",
        "since",
        "this",
        "morning"
      ],
      "highlightingPart": "My head hurts",
      "practiceQuestion": "Where does it hurt?",
      "sentenceTranslation": "오늘 아침부터 머리가 아파요.",
      "sentenceWordChoices": [
        "evening",
        "hurt",
        "this",
        "head",
        "since",
        "heads",
        "My",
        "hurts",
        "morning"
      ],
      "sentenceTranslateWords": [
        "오늘",
        "아침부터",
        "머리가",
        "아파요"
      ],
      "practiceQuestionTranslation": "어디가 아프세요?",
      "sentenceTranslateWordChoices": [
        "저녁부터",
        "머리가",
        "아파요",
        "다리가",
        "어제",
        "아침부터",
        "오늘"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "오늘",
          "아침부터",
          "머리가",
          "아파요"
        ],
        [
          "머리가",
          "오늘",
          "아침부터",
          "아파요"
        ],
        [
          "오늘",
          "아침부터",
          "아파요",
          "머리가"
        ],
        [
          "머리가",
          "아파요",
          "오늘",
          "아침부터"
        ]
      ]
    },
    "changes": {
      "sentenceText": "My head has been hurting since this morning.",
      "sentenceWords": [
        "My",
        "head",
        "has",
        "been",
        "hurting since this morning"
      ],
      "highlightingPart": "My head has been hurting",
      "sentenceWordChoices": [
        "evening",
        "hurt",
        "head",
        "has",
        "heads",
        "My",
        "been",
        "hurting since this morning"
      ]
    }
  },
  {
    "expressionId": 2410,
    "exampleNumber": 1,
    "expressionSource": "FREE_TALK",
    "reason": "연필을 잊었는지 묻는 질문과 자연스러운 동의 응답으로 수정한다.",
    "expected": {
      "sentenceText": "Sure, I forgot my pencil too.",
      "sentenceWords": [
        "Sure",
        "I",
        "forgot",
        "my",
        "pencil",
        "too"
      ],
      "highlightingPart": "I forgot my pencil",
      "practiceQuestion": "Can I borrow a pencil?",
      "sentenceTranslation": "그래, 나도 연필 깜빡했어.",
      "sentenceWordChoices": [
        "forget",
        "Sure",
        "penciled",
        "too",
        "my",
        "forgot",
        "I",
        "pencil",
        "mine"
      ],
      "sentenceTranslateWords": [
        "그래",
        "나도",
        "연필",
        "깜빡했어"
      ],
      "practiceQuestionTranslation": "연필 좀 빌려줄 수 있어?",
      "sentenceTranslateWordChoices": [
        "가져왔어",
        "깜빡했어",
        "연필",
        "깜빡해",
        "나도",
        "지우개",
        "그래"
      ],
      "sentenceTranslateAcceptedAnswers": [
        [
          "그래",
          "나도",
          "연필",
          "깜빡했어"
        ],
        [
          "그래",
          "연필",
          "나도",
          "깜빡했어"
        ],
        [
          "그래",
          "나도",
          "깜빡했어",
          "연필"
        ],
        [
          "그래",
          "연필",
          "깜빡했어",
          "나도"
        ]
      ]
    },
    "changes": {
      "sentenceText": "Yeah, I forgot my pencil too.",
      "sentenceWords": [
        "Yeah",
        "I",
        "forgot",
        "my",
        "pencil",
        "too"
      ],
      "practiceQuestion": "Did you forget your pencil too?",
      "sentenceWordChoices": [
        "forget",
        "Yeah",
        "penciled",
        "too",
        "my",
        "forgot",
        "I",
        "pencil",
        "mine"
      ],
      "practiceQuestionTranslation": "너도 연필 깜빡했어?"
    }
  }
]
$lan584_text$::JSONB);

DO $$
BEGIN
    IF (SELECT COUNT(*) FROM lan584_text_updates) <> 12 THEN
        RAISE EXCEPTION 'LAN-584 text manifest count mismatch';
    END IF;
    IF EXISTS (
        SELECT 1 FROM lan584_text_updates mapping
        LEFT JOIN writing_expression expression ON expression.id = mapping.expression_id
        WHERE expression.id IS NULL
           OR expression.expression_source IS DISTINCT FROM mapping.expression_source
           OR expression.target_locale IS DISTINCT FROM 'EN'
           OR expression.base_locale IS DISTINCT FROM 'KR'
           OR CASE WHEN jsonb_typeof(expression.practice_examples_payload) = 'array'
                   THEN jsonb_array_length(expression.practice_examples_payload) ELSE 0 END <> 4
           OR EXISTS (
               SELECT 1 FROM jsonb_each(mapping.expected_example) field
               WHERE expression.practice_examples_payload -> (mapping.example_number - 1) -> field.key
                     IS DISTINCT FROM field.value
           )
    ) THEN
        RAISE EXCEPTION 'LAN-584 text source precondition failed';
    END IF;
END $$;

CREATE TEMP TABLE lan584_text_originals ON COMMIT DROP AS
SELECT expression.id, to_jsonb(expression) AS original_record,
       expression.practice_examples_payload AS payload
FROM writing_expression expression
WHERE expression.id IN (SELECT expression_id FROM lan584_text_updates);

CREATE TEMP TABLE lan584_text_patched ON COMMIT DROP AS
SELECT original.id,
       jsonb_agg(
           CASE WHEN mapping.example_number = example.ordinality
                THEN example.value || mapping.changes ELSE example.value END
           ORDER BY example.ordinality
       ) AS payload
FROM lan584_text_originals original
JOIN lan584_text_updates mapping ON mapping.expression_id = original.id
CROSS JOIN LATERAL jsonb_array_elements(original.payload)
    WITH ORDINALITY AS example(value, ordinality)
GROUP BY original.id;

UPDATE writing_expression expression
SET practice_examples_payload = patched.payload
FROM lan584_text_patched patched
WHERE expression.id = patched.id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM lan584_text_originals original
        JOIN lan584_text_patched patched ON patched.id = original.id
        LEFT JOIN writing_expression expression ON expression.id = original.id
        WHERE expression.id IS NULL
           OR to_jsonb(expression) IS DISTINCT FROM jsonb_set(
               original.original_record, '{practice_examples_payload}', patched.payload, false)
    ) THEN
        RAISE EXCEPTION 'LAN-584 text postcondition failed';
    END IF;
END $$;
