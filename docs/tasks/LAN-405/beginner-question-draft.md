<!-- LAN-405 초보용 시나리오 질문의 사용자 승인 초안 -->
# LAN-405 초보용 시나리오 질문 초안

## 배치 계약

| 항목 | 값 |
| --- | --- |
| 기준 데이터 | 2026-08-31 develop DB 활성 콘텐츠 |
| 활성 시나리오 | 40개 |
| 카테고리 | 기숙사 12, 여행 13, 수업 8, 쇼핑 7 |
| 기존 난이도 | EASY 16, NORMAL 16, HARD 8 |
| 시작 화자 | AI 30, USER 10 |
| 신규 질문 그룹 | `LEVEL_1`, `LEVEL_2_TO_3` |
| 그룹별 질문 | 시나리오당 3개 |
| 전체 신규 질문 | 240개 |
| 변경 금지 | 기존 `LEVEL_4_TO_5` 질문, 시나리오 메타데이터, Writing 표현 |

영어 질문은 모두 25단어 이하로 작성한다. AI First 시나리오의 각 그룹 첫 질문에는 `GOOD` 속마음을 둔다. 질문 ID, 음원 URL, SQL은 텍스트 승인 후 확정한다.

## 기숙사

### 시나리오 1. 입주 첫날, 룸메이트 Marco와 첫 만남

- 시작 화자. `AI`.
- AI 역할. 스페인에서 온 교환학생 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 먼저 이름을 물으면 편하게 대화를 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi! I'm Marco. What's your name?**
   - 안녕! 난 Marco야. 이름이 뭐야?
2. **What do you like to do for fun?**
   - 뭐 하면서 노는 걸 좋아해?
3. **I want to visit Korea. What place do you like there?**
   - 나 한국에 가보고 싶어. 넌 한국에서 어떤 곳을 좋아해?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 간단한 자기소개를 부탁하면 서로를 자연스럽게 알아갈 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Nice to meet you! I'm Marco. Could you tell me a little about yourself?**
   - 만나서 반가워! 난 Marco야. 네 소개를 간단히 해줄래?
2. **What hobby do you enjoy, and what do you like about it?**
   - 어떤 취미를 즐겨? 그 취미의 어떤 점이 좋아?
3. **If I visit Korea, what place should I see first?**
   - 내가 한국에 가면 어디를 먼저 가보면 좋을까?

### 시나리오 3. 카페 수다 — 주말 약속 잡기

- 시작 화자. `AI`.
- AI 역할. 주말에 같이 놀고 싶은 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 토요일과 일요일 중 하나를 고르게 하면 약속을 쉽게 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Let's meet this weekend! Is Saturday or Sunday better?**
   - 이번 주말에 만나자! 토요일이 좋아, 일요일이 좋아?
2. **Do you want to go to a park or a cafe?**
   - 공원에 갈래, 카페에 갈래?
3. **Great! What time should we meet?**
   - 좋아! 몇 시에 만날까?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 가능한 요일부터 정하면 계획을 차근차근 세울 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **We should hang out this weekend. Which day works better for you?**
   - 이번 주말에 같이 놀자. 어느 날이 더 괜찮아?
2. **Would you rather relax at a cafe or do something outside?**
   - 카페에서 쉴래, 아니면 밖에서 뭔가 할래?
3. **Tell me what time and place would be easy for you.**
   - 너한테 편한 시간과 장소를 말해줘.

### 시나리오 6. 인터내셔널 파티 — 처음 만난 Chloe

- 시작 화자. `AI`.
- AI 역할. 파티에서 처음 만난 유학생.

#### `LEVEL_1`

- 첫 질문 속마음. 이름부터 물으면 처음 만난 사람도 쉽게 답할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, I'm Chloe. What's your name?**
   - 안녕, 난 Chloe야. 이름이 뭐야?
2. **What food do you like at this party?**
   - 이 파티에서 어떤 음식이 좋아?
3. **There's another party next week. Do you want to come?**
   - 다음 주에도 파티가 있어. 올래?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 가벼운 소개부터 시작하면 파티에서 자연스럽게 친해질 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, I'm Chloe. How are you enjoying the party?**
   - 안녕, 난 Chloe야. 파티는 재미있어?
2. **What kind of music or food do you like at parties?**
   - 파티에서 어떤 음악이나 음식을 좋아해?
3. **My friend has a party next week. Would you like to join us?**
   - 내 친구가 다음 주에 파티를 해. 우리랑 같이 갈래?

### 시나리오 21. 방 키를 잃어버려 임시 출입 요청하기

- 시작 화자. `USER`.
- AI 역할. 기숙사 생활지원실 직원.
- 사용자 시작 안내. 방 키를 잃어버렸다고 설명하고 방에 들어갈 방법을 묻는다.

#### `LEVEL_1`

1. **I can give you a spare key or open the door for you. Which do you want?**
   - 여분의 키를 드리거나 문을 열어드릴 수 있어요. 어느 쪽이 좋으세요?
2. **Your new key can be ready tomorrow. Is morning okay?**
   - 새 키는 내일 준비돼요. 오전에 괜찮으세요?
3. **Should we call you or send a text?**
   - 전화드릴까요, 문자를 보내드릴까요?

#### `LEVEL_2_TO_3`

1. **We can lend you a temporary key or have a staff member open your door. Which would help more?**
   - 임시 키를 빌려드리거나 직원이 문을 열어드릴 수 있어요. 어느 쪽이 더 좋으세요?
2. **A replacement key can be ready tomorrow morning or afternoon. When can you pick it up?**
   - 새 키는 내일 오전이나 오후에 준비돼요. 언제 찾으러 오실 수 있나요?
3. **Would you like a text or a phone call when it's ready?**
   - 준비되면 문자와 전화 중 어떤 방법으로 알려드릴까요?

### 시나리오 23. 세탁물이 뒤섞인 문제 해결하기

- 시작 화자. `AI`.
- AI 역할. 세탁실에서 옷을 찾는 기숙사 이웃.

#### `LEVEL_1`

- 첫 질문 속마음. 서로의 옷 색을 알려주면 쉽게 찾아볼 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **I have your white shirt. Do you see my blue shirt?**
   - 네 흰 셔츠가 나한테 있어. 내 파란 셔츠 보여?
2. **Do you want to use the dryer together?**
   - 건조기를 같이 쓸래?
3. **Next time, should we send a text when clothes get mixed up?**
   - 다음에 옷이 섞이면 문자할까?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 찾는 옷을 분명히 말하면 서로 바로 확인할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Our laundry got mixed up. I found your white shirt. Could you look for my blue one?**
   - 우리 빨래가 섞였어. 네 흰 셔츠는 찾았어. 내 파란 셔츠도 찾아줄래?
2. **The dryer is cheaper if we share it. Would you like to use it together or separately?**
   - 건조기는 같이 쓰면 더 저렴해. 같이 쓸래, 따로 쓸래?
3. **If this happens again, should we text each other or leave the clothes by the door?**
   - 다음에 또 이러면 서로 문자할까, 아니면 방문 앞에 옷을 둘까?

### 시나리오 5. 소음, 손님, 경계 정하기

- 시작 화자. `AI`.
- AI 역할. 생활 규칙을 정하고 싶은 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 손님 방문에 괜찮은 시간을 먼저 물으면 쉽게 규칙을 정할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Can my friends visit our room on Friday evening?**
   - 금요일 저녁에 내 친구들이 우리 방에 와도 돼?
2. **Should I ask before I use your things?**
   - 내가 네 물건을 쓰기 전에 물어봐야 할까?
3. **Do you want the room quiet at night?**
   - 밤에는 방이 조용하면 좋겠어?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 손님 초대부터 확인하면 서로의 기준을 편하게 말할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **I'd like to invite two friends on Friday evening. Is that okay, or should we choose another time?**
   - 금요일 저녁에 친구 두 명을 부르고 싶어. 괜찮아, 아니면 다른 시간으로 할까?
2. **What things can we share, and what should we always ask before using?**
   - 어떤 물건은 같이 써도 되고, 어떤 건 꼭 물어봐야 할까?
3. **How quiet should we keep the room on weeknights?**
   - 평일 밤에는 방을 어느 정도 조용히 해야 할까?

### 시나리오 22. 공용 냉장고 공간 정리하기

- 시작 화자. `AI`.
- AI 역할. 냉장고 정리 규칙을 정하려는 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 음식에 이름을 쓰는 간단한 규칙부터 제안해야겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Let's write our names on our food. Is that okay?**
   - 음식에 우리 이름을 쓰자. 괜찮아?
2. **Should we throw away old food every Friday?**
   - 매주 금요일에 오래된 음식을 버릴까?
3. **Which shelf do you want to use?**
   - 넌 어느 칸을 쓰고 싶어?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 이름과 공유 표시를 나누면 냉장고를 쉽게 정리할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **We could write names on personal food and use a green sticker for shared food. What do you think?**
   - 개인 음식에는 이름을 쓰고 같이 먹는 음식에는 초록 스티커를 붙이면 어때?
2. **For food with no name, should we ask in the group chat before throwing it away?**
   - 이름 없는 음식은 버리기 전에 단체 채팅방에 물어볼까?
3. **Would Friday or Sunday be better for cleaning the fridge?**
   - 냉장고 정리는 금요일과 일요일 중 언제가 더 좋아?

### 시나리오 2. 집안일과 생활 규칙 정하기

- 시작 화자. `AI`.
- AI 역할. 생활 규칙을 정하고 싶은 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 좋아하는 집안일부터 물으면 역할을 쉽게 나눌 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you like washing dishes or cleaning the floor?**
   - 설거지와 바닥 청소 중 뭐가 더 좋아?
2. **When should we clean our room?**
   - 우리 방은 언제 청소할까?
3. **Do you sleep early or late?**
   - 너는 일찍 자, 늦게 자?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 각자 편한 집안일을 말하면 공평하게 나눌 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Which chores do you prefer? I can wash dishes or clean the floor.**
   - 어떤 집안일이 더 편해? 나는 설거지나 바닥 청소를 할 수 있어.
2. **How often should we clean the room, and which day works best?**
   - 방을 얼마나 자주 청소하고, 어느 날 하는 게 좋을까?
3. **I'm often awake late. What quiet-time rule would work for both of us?**
   - 나는 자주 늦게까지 깨어 있어. 우리 둘에게 맞는 조용한 시간 규칙은 뭘까?

### 시나리오 24. 세면대 누수 수리 요청하기

- 시작 화자. `USER`.
- AI 역할. 기숙사 시설 관리 접수 직원.
- 사용자 시작 안내. 세면대에서 물이 샌다고 설명하고 수리를 요청한다.

#### `LEVEL_1`

1. **Please stop using the sink. Do you need a towel or a bucket?**
   - 세면대 사용을 멈춰주세요. 수건이나 양동이가 필요하세요?
2. **We can come today at six or tomorrow at nine. Which time is good?**
   - 오늘 6시나 내일 9시에 갈 수 있어요. 어느 시간이 좋으세요?
3. **Should the worker call you before coming?**
   - 직원이 가기 전에 전화드릴까요?

#### `LEVEL_2_TO_3`

1. **Please don't use the sink for now. Would you like a bucket, or help turning off the water?**
   - 지금은 세면대를 쓰지 마세요. 양동이를 드릴까요, 아니면 물 잠그는 걸 도와드릴까요?
2. **A worker can visit at six today or nine tomorrow. Which appointment works for you?**
   - 직원이 오늘 6시나 내일 9시에 방문할 수 있어요. 어느 시간이 괜찮으세요?
3. **Should the worker call first, and may they enter if you are away?**
   - 직원이 먼저 전화드릴까요? 안 계시면 들어가도 될까요?

### 시나리오 4. 기숙사 에어컨 요금 문제 — 프론트에 전화하기

- 시작 화자. `USER`.
- AI 역할. 기숙사 프론트 데스크 직원.
- 사용자 시작 안내. 방을 비운 7월에 에어컨 요금이 청구됐다고 설명한다.

#### `LEVEL_1`

1. **I see the $100 charge. Were you away for all of July?**
   - 100달러 요금이 보이네요. 7월 내내 방을 비우셨나요?
2. **Can you send us your plane ticket or hotel booking?**
   - 항공권이나 호텔 예약서를 보내주실 수 있나요?
3. **We can return the money in two weeks. Is that okay?**
   - 2주 안에 돈을 돌려드릴 수 있어요. 괜찮으세요?

#### `LEVEL_2_TO_3`

1. **I found the $100 July charge. Please tell me when you left and returned.**
   - 7월 요금 100달러를 확인했어요. 언제 떠났고 돌아왔는지 말씀해 주세요.
2. **We need proof that you were away. Could you send a plane ticket or booking confirmation?**
   - 방을 비웠다는 증빙이 필요해요. 항공권이나 예약 확인서를 보내주시겠어요?
3. **After we check it, the refund will take about two weeks. Would that work for you?**
   - 확인 후 환불까지 약 2주 걸려요. 괜찮으시겠어요?

### 시나리오 7. 서로 더 알아가는 밤

- 시작 화자. `AI`.
- AI 역할. 더 친해지고 싶은 룸메이트.

#### `LEVEL_1`

- 첫 질문 속마음. 좋아하는 가족 활동을 물으면 부담 없이 가족 이야기를 할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What do you like to do with your family?**
   - 가족과 무엇을 하는 걸 좋아해?
2. **What job do you want in the future?**
   - 나중에 어떤 일을 하고 싶어?
3. **What makes you happy these days?**
   - 요즘 무엇이 너를 행복하게 해?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 가족과 보내는 시간을 물으면 자연스럽게 서로를 더 알 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What do you enjoy doing with your family or close friends?**
   - 가족이나 친한 친구와 무엇을 하는 걸 좋아해?
2. **What kind of work would you like to do someday?**
   - 언젠가 어떤 일을 하고 싶어?
3. **Is there anything simple that has made you happy lately?**
   - 최근에 너를 기분 좋게 한 소소한 일이 있어?

### 시나리오 25. 퇴실 점검과 보증금 확인하기

- 시작 화자. `AI`.
- AI 역할. 기숙사 퇴실 점검 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 청소 목록과 함께 점검 준비를 쉽게 알려줘야겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Please clean the room and remove your things. Do you want a checklist?**
   - 방을 청소하고 짐을 빼주세요. 확인 목록을 드릴까요?
2. **We can check the room today at three or tomorrow at ten. Which time is good?**
   - 오늘 3시나 내일 10시에 방을 확인할 수 있어요. 어느 시간이 좋으세요?
3. **Please take photos of any old damage. Can you do that?**
   - 원래 있던 파손은 사진을 찍어주세요. 그렇게 할 수 있나요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 필요한 준비를 짧게 안내하고 확인 목록이 필요한지 물어봐야겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Before the inspection, remove your things and clean the room. Would a checklist help you?**
   - 점검 전에 짐을 빼고 방을 청소해 주세요. 확인 목록이 필요하세요?
2. **Inspection times are three today and ten tomorrow. Which one should I reserve?**
   - 점검 시간은 오늘 3시와 내일 10시예요. 언제로 예약해 드릴까요?
3. **Damage may reduce your deposit. Please prepare move-in photos if the damage was already there.**
   - 파손이 있으면 보증금이 줄 수 있어요. 원래 있던 파손이라면 입주 때 사진을 준비해 주세요.

## 여행

### 시나리오 17. 카페에서 주문하기

- 시작 화자. `AI`.
- AI 역할. 카페 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 메뉴를 바로 물으면 손님이 쉽게 주문할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi! What drink would you like?**
   - 안녕하세요! 어떤 음료를 드릴까요?
2. **Do you want it hot or cold?**
   - 따뜻하게 드릴까요, 차갑게 드릴까요?
3. **Will you drink it here or take it with you?**
   - 여기서 드실 건가요, 가지고 가실 건가요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 원하는 음료부터 확인하면 주문을 자연스럽게 이어갈 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Welcome! What would you like to drink today?**
   - 어서 오세요! 오늘 어떤 음료를 드릴까요?
2. **Would you like it hot or iced, and what size would you like?**
   - 따뜻하게 드릴까요, 아이스로 드릴까요? 크기는 어떻게 하시겠어요?
3. **Is that for here or to go? We also have fresh cookies today.**
   - 드시고 가세요, 포장이세요? 오늘 갓 나온 쿠키도 있어요.

### 시나리오 16. 마음에 안 드는 사람 정중히 거절하기

- 시작 화자. `AI`.
- AI 역할. 길에서 데이트를 제안하는 낯선 사람.

#### `LEVEL_1`

- 첫 질문 속마음. 커피 제안을 분명하게 하면 상대가 편하게 답할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi. Would you like to get coffee with me?**
   - 안녕하세요. 저와 커피 마실래요?
2. **I understand. Do you want me to leave now?**
   - 알겠어요. 제가 이제 가면 될까요?
3. **Okay. Have a nice day!**
   - 알겠어요. 좋은 하루 보내세요!

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 부담 없이 제안하되 거절할 수 있는 여지를 줘야겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, sorry to stop you. Would you be interested in getting coffee sometime?**
   - 안녕하세요, 붙잡아서 죄송해요. 언제 커피 한잔하실래요?
2. **No problem. Would you prefer that I leave you alone?**
   - 괜찮아요. 제가 더 이상 말을 걸지 않는 게 좋을까요?
3. **I understand. Thanks for being clear, and have a good day.**
   - 알겠어요. 분명히 말씀해 주셔서 감사하고, 좋은 하루 보내세요.

### 시나리오 20. 친구와 여행 수다 떨기

- 시작 화자. `AI`.
- AI 역할. 여행 이야기를 좋아하는 친구.

#### `LEVEL_1`

- 첫 질문 속마음. 좋아하는 여행 장소를 물으면 쉽게 이야기를 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What place do you like to visit?**
   - 어떤 곳으로 여행 가는 걸 좋아해?
2. **Do you like mountains or beaches?**
   - 산이 좋아, 바다가 좋아?
3. **Where do you want to go next?**
   - 다음에는 어디에 가고 싶어?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 좋아하는 여행지를 물으면 부담 없이 경험과 취향을 나눌 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What kind of place do you enjoy visiting, and what do you like about it?**
   - 어떤 곳으로 여행 가는 걸 좋아하고, 그곳의 어떤 점이 좋아?
2. **Do you prefer a busy city trip or a quiet nature trip?**
   - 바쁜 도시 여행과 조용한 자연 여행 중 어떤 걸 더 좋아해?
3. **Choose one place you want to visit next and tell me why.**
   - 다음에 가고 싶은 곳 하나와 그 이유를 말해줘.

### 시나리오 26. 일정에 맞는 기차표 구매하기

- 시작 화자. `USER`.
- AI 역할. 기차역 매표소 직원.
- 사용자 시작 안내. 당일 기차표를 찾고 있으며 직행과 환승 열차를 비교해 달라고 요청한다.

#### `LEVEL_1`

1. **The direct train is $45, and the slower train is $30. Which ticket do you want?**
   - 직행 열차는 45달러이고, 느린 열차는 30달러예요. 어떤 표를 원하세요?
2. **Do you want a paper ticket or a phone ticket?**
   - 종이 표와 휴대폰 표 중 어떤 걸 원하세요?
3. **Would you like a window seat or an aisle seat?**
   - 창가 좌석과 통로 좌석 중 어디가 좋으세요?

#### `LEVEL_2_TO_3`

1. **The direct train arrives at four for $45. The transfer arrives at five for $30. Which one would you like?**
   - 직행은 4시 도착에 45달러예요. 환승은 5시 도착에 30달러예요. 어떤 걸로 드릴까요?
2. **Would you like the ticket printed or sent to your phone?**
   - 표를 출력해 드릴까요, 휴대폰으로 보내드릴까요?
3. **Window and aisle seats are available. Tell me your seat preference.**
   - 창가와 통로 좌석이 남아 있어요. 원하는 좌석을 말씀해 주세요.

### 시나리오 13. 비행기 옆자리 승객과의 대화

- 시작 화자. `AI`.
- AI 역할. 비행기 옆자리에 앉은 여행자.

#### `LEVEL_1`

- 첫 질문 속마음. 목적지를 물으면 옆자리 사람과 쉽게 대화를 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi! Do you like quiet flights or talking with the person next to you?**
   - 안녕하세요! 조용히 가는 것과 옆 사람과 이야기하는 것 중 어느 쪽을 좋아하세요?
2. **Do you like window seats or aisle seats?**
   - 창가 좌석과 통로 좌석 중 어디를 좋아하세요?
3. **What do you like to do on a trip?**
   - 여행에서 무엇을 하는 걸 좋아하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 여행 목적을 가볍게 물으면 자연스럽게 대화를 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, I'm sitting next to you. Do you prefer a quiet flight or some conversation?**
   - 안녕하세요, 옆자리에 앉게 됐네요. 조용히 가는 것과 이야기하는 것 중 어느 쪽이 좋아요?
2. **Do you prefer window or aisle seats, and what do you like about them?**
   - 창가와 통로 좌석 중 어디를 더 좋아하고, 어떤 점이 좋아요?
3. **What is one thing you enjoy doing when you travel?**
   - 여행할 때 즐겨 하는 일 한 가지가 뭐예요?

### 시나리오 15. 호텔 체크인 하기

- 시작 화자. `AI`.
- AI 역할. 호텔 프론트 데스크 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 예약자 이름부터 확인하면 체크인을 쉽게 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Welcome! What name is the reservation under?**
   - 어서 오세요! 어떤 이름으로 예약하셨나요?
2. **Your room will be ready in one hour. Is that okay?**
   - 객실은 한 시간 뒤에 준비돼요. 괜찮으세요?
3. **Do you want a high floor or a low floor?**
   - 높은 층과 낮은 층 중 어디를 원하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 예약 정보를 먼저 확인하면 체크인 절차를 차분히 진행할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Welcome to the hotel. Could I have the name on your reservation?**
   - 호텔에 오신 것을 환영합니다. 예약자 성함을 알려주시겠어요?
2. **Your room needs one more hour. Would you like to wait here or leave your bags with us?**
   - 객실 준비에 한 시간 더 필요해요. 여기서 기다리실래요, 짐을 맡기실래요?
3. **Would you prefer a higher floor or a room away from the elevator?**
   - 높은 층과 엘리베이터에서 먼 방 중 어느 쪽을 원하세요?

### 시나리오 28. 여행 일정에 맞는 박물관 패스 고르기

- 시작 화자. `AI`.
- AI 역할. 관광 안내소 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 두 패스의 차이를 짧게 알려주면 쉽게 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The museum pass has two museums. The city pass also has buses. Which do you want?**
   - 박물관 패스는 박물관 두 곳에 갈 수 있어요. 도시 패스는 버스도 탈 수 있어요. 어떤 걸 원하세요?
2. **Lunch costs $15 more. Do you want to add it?**
   - 점심은 15달러가 더 들어요. 추가하시겠어요?
3. **Do you want a paper pass or a phone pass?**
   - 종이 패스와 휴대폰 패스 중 어떤 걸 원하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 포함 항목을 비교해서 알려주면 일정에 맞는 패스를 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The museum pass covers two museums. The city pass includes museums and public transit. Which fits your plan?**
   - 박물관 패스는 박물관 두 곳, 도시 패스는 박물관과 대중교통이 포함돼요. 어느 쪽이 일정에 맞나요?
2. **You can add lunch for $15. Would you like to include it?**
   - 15달러를 추가하면 점심도 포함할 수 있어요. 추가하시겠어요?
3. **Would you like a paper pass or a digital pass on your phone?**
   - 종이 패스와 휴대폰 디지털 패스 중 어떤 걸 드릴까요?

### 시나리오 27. 식당에서 메뉴 비교하고 주문하기

- 시작 화자. `AI`.
- AI 역할. 현지 식당 서버.

#### `LEVEL_1`

- 첫 질문 속마음. 부드러운 음식과 매운 음식을 알려주면 쉽게 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The pasta is mild. The curry is spicy. Which one do you want?**
   - 파스타는 부드럽고 커리는 매워요. 어떤 걸 원하세요?
2. **Would you like a regular size or a large size?**
   - 보통 크기와 큰 크기 중 어떤 걸 원하세요?
3. **Would you like water or juice?**
   - 물과 주스 중 어떤 걸 드릴까요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 두 메뉴의 맛을 먼저 설명하면 손님이 편하게 선택할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The pasta is mild and creamy, while the curry is hot and spicy. Which sounds better today?**
   - 파스타는 부드럽고 크리미하고, 커리는 맵고 진해요. 오늘은 어떤 게 더 당기세요?
2. **Both dishes come in regular and large sizes. Which size would you like?**
   - 두 메뉴 모두 보통 크기와 큰 크기가 있어요. 어떤 크기로 드릴까요?
3. **Would you like water, juice, or another drink with your meal?**
   - 식사와 함께 물, 주스, 다른 음료 중 무엇을 드릴까요?

### 시나리오 19. 길 잃고 현지인에게 길 묻기

- 시작 화자. `USER`.
- AI 역할. 길을 안내하는 현지인.
- 사용자 시작 안내. Tower Bridge로 가려면 어느 출구로 나가야 하는지 묻는다.

#### `LEVEL_1`

1. **Use the east exit. Do you see the blue sign?**
   - 동쪽 출구로 가세요. 파란 표지판이 보이나요?
2. **Walk straight for five minutes, then turn left. Is that clear?**
   - 5분 동안 곧장 간 뒤 왼쪽으로 도세요. 이해되셨나요?
3. **Do you want me to show you the way on a map?**
   - 지도에서 길을 보여드릴까요?

#### `LEVEL_2_TO_3`

1. **Take the east exit by the blue sign. Can you see it from here?**
   - 파란 표지판 옆 동쪽 출구로 나가세요. 여기서 보이시나요?
2. **Walk straight for five minutes and turn left at the bank. Would you like me to repeat that?**
   - 5분쯤 곧장 가서 은행에서 왼쪽으로 도세요. 다시 말씀드릴까요?
3. **I can mark the route on your map if that would help.**
   - 도움이 된다면 지도에 길을 표시해 드릴게요.

### 시나리오 29. 날씨 때문에 야외 투어 일정 변경하기

- 시작 화자. `USER`.
- AI 역할. 여행사 일정 변경 직원.
- 사용자 시작 안내. 폭우 때문에 야외 투어 일정을 바꿀 수 있는지 묻는다.

#### `LEVEL_1`

1. **The morning tour is the same price. The evening tour costs $10 more. Which do you want?**
   - 오전 투어는 같은 가격이고, 저녁 투어는 10달러가 더 들어요. 어느 쪽이 좋으세요?
2. **Should we send your new ticket by email or text?**
   - 새 표를 이메일과 문자 중 어디로 보내드릴까요?
3. **If the weather changes again, should we call you or send a text?**
   - 날씨가 또 바뀌면 전화드릴까요, 문자를 보내드릴까요?

#### `LEVEL_2_TO_3`

1. **Morning has a smaller group. Evening includes city lights and costs $10 more. Which time would you prefer?**
   - 오전은 인원이 적어요. 저녁은 야경이 포함되고 10달러가 더 들어요. 어느 시간이 더 좋으세요?
2. **Would you like the new booking confirmation by email or text?**
   - 새 예약 확인서를 이메일과 문자 중 어디로 보내드릴까요?
3. **If the weather changes again, would you prefer a phone call or a text update?**
   - 날씨가 또 바뀌면 전화와 문자 중 어떤 방법으로 알려드릴까요?

### 시나리오 18. 약국에서 증상 설명하고 약 사기

- 시작 화자. `AI`.
- AI 역할. 약사.

#### `LEVEL_1`

- 첫 질문 속마음. 머리가 아픈지 먼저 확인하면 필요한 도움을 쉽게 줄 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hello. Does your head hurt?**
   - 안녕하세요. 머리가 아프세요?
2. **We have pills or liquid medicine. Which is easier for you?**
   - 알약과 물약이 있어요. 어느 쪽이 더 편하세요?
3. **Take one pill after food. Can you follow that?**
   - 식후에 한 알 드세요. 그렇게 드실 수 있나요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 현재 증상을 간단히 확인하면 알맞은 약을 안내할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hello. Please tell me where it hurts and how you feel right now.**
   - 안녕하세요. 지금 어디가 아프고 어떤 느낌인지 말씀해 주세요.
2. **We have tablets and liquid medicine for headaches. Which type would be easier for you?**
   - 두통약은 알약과 물약이 있어요. 어느 쪽이 더 편하세요?
3. **Take one pill after each meal, up to three times a day. Is that clear?**
   - 식후에 한 알씩, 하루 최대 세 번 드세요. 이해되셨나요?

### 시나리오 30. 공항에서 더 편한 좌석으로 변경하기

- 시작 화자. `AI`.
- AI 역할. 공항 탑승구 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 넓은 좌석의 가격을 바로 알려주면 쉽게 결정할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **A wider seat costs $50. A free exit seat means you must help if there is danger. Which do you want?**
   - 더 넓은 좌석은 50달러예요. 무료 비상구 좌석은 위험할 때 도와야 해요. 어느 쪽이 좋으세요?
2. **We can check your bag for free. Would you like that?**
   - 가방을 무료로 부쳐드릴 수 있어요. 원하세요?
3. **Do you want your new boarding pass on paper or on your phone?**
   - 새 탑승권을 종이와 휴대폰 중 어디로 받으시겠어요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 추가 공간과 가격을 함께 알려주면 좌석을 판단하기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **More legroom costs $50. A free exit-row seat requires emergency help. Which option would you prefer?**
   - 다리 공간이 넓은 좌석은 50달러예요. 무료 비상구 좌석은 비상시에 도와야 해요. 어느 쪽을 원하세요?
2. **Overhead space is limited, so we can check your carry-on for free. Would you like that?**
   - 기내 짐칸이 부족해서 가방을 무료로 부쳐드릴 수 있어요. 원하시나요?
3. **Would you like the updated boarding pass printed or sent to your phone?**
   - 새 탑승권을 출력해 드릴까요, 휴대폰으로 보내드릴까요?

### 시나리오 14. 수하물 파손 — 카운터에 항의하기

- 시작 화자. `USER`.
- AI 역할. 항공사 카운터 직원.
- 사용자 시작 안내. 캐리어가 파손됐다고 설명하고 보상 방법을 묻는다.

#### `LEVEL_1`

1. **I'm sorry. Can you show me the broken part?**
   - 죄송합니다. 파손된 부분을 보여주시겠어요?
2. **We can give you money or a travel coupon. Which do you want?**
   - 현금이나 여행 쿠폰으로 보상해 드릴 수 있어요. 어느 쪽이 좋으세요?
3. **Should we send it by email or text?**
   - 이메일과 문자 중 어디로 보내드릴까요?

#### `LEVEL_2_TO_3`

1. **I'm sorry about the damage. Please show me the broken part and explain the problem.**
   - 파손되어 죄송합니다. 부서진 부분을 보여주시고 문제를 설명해 주세요.
2. **We can offer cash, miles, or a travel voucher. Which kind of compensation would you prefer?**
   - 현금, 마일리지, 여행 바우처로 보상할 수 있어요. 어떤 보상을 원하세요?
3. **Would you like the confirmation sent by email or text message?**
   - 처리 확인서를 이메일과 문자 중 어디로 보내드릴까요?

## 수업

### 시나리오 8. 첫 수업, 옆자리 Marco

- 시작 화자. `AI`.
- AI 역할. 같은 수업을 듣는 학생.

#### `LEVEL_1`

- 첫 질문 속마음. 먼저 앉아도 되는지 물으면 자연스럽게 인사할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, can I sit here? I'm Marco. What's your name?**
   - 안녕, 여기 앉아도 돼? 난 Marco야. 이름이 뭐야?
2. **Do you like this class?**
   - 이 수업이 좋아?
3. **What is your favorite class at school?**
   - 학교에서 어떤 수업을 가장 좋아해?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 자리를 확인하고 소개하면 첫 수업에서도 편하게 대화할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Hi, is this seat free? I'm Marco. What should I call you?**
   - 안녕, 이 자리 비었어? 난 Marco야. 넌 뭐라고 부르면 돼?
2. **What made you choose this class?**
   - 이 수업을 고른 이유가 뭐야?
3. **What is one thing you like about school in Korea?**
   - 한국 학교생활에서 좋아하는 점 한 가지가 뭐야?

### 시나리오 11. 시험 공부 수다

- 시작 화자. `AI`.
- AI 역할. 같이 시험을 준비하는 친구.

#### `LEVEL_1`

- 첫 질문 속마음. 공부하는 시간을 물으면 쉬운 이야기부터 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you study in the morning or at night?**
   - 아침에 공부해, 밤에 공부해?
2. **Can we study chapter five together?**
   - 우리 5단원을 같이 공부할래?
3. **Should we meet at the library after class?**
   - 수업 후에 도서관에서 만날까?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 평소 공부 시간을 물으면 서로 맞는 계획을 찾기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **When do you study best, and where do you usually study?**
   - 언제 공부가 가장 잘되고, 보통 어디서 공부해?
2. **Chapter five is difficult for me. Could we review it together after class?**
   - 나는 5단원이 어려워. 수업 후에 같이 복습할 수 있을까?
3. **Would the library at four or the cafe at five work better?**
   - 4시에 도서관과 5시에 카페 중 어디가 더 좋아?

### 시나리오 32. 결석한 수업 노트 부탁하기

- 시작 화자. `AI`.
- AI 역할. 필기를 보여주려는 동급생.

#### `LEVEL_1`

- 첫 질문 속마음. 필요한 필기를 두 가지로 나누어 알려주면 쉽게 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **I have notes about the homework and examples. Which do you need first?**
   - 과제와 예제 필기가 있어. 뭐부터 필요해?
2. **Should I send photos or lend you my notebook?**
   - 사진을 보내줄까, 노트를 빌려줄까?
3. **Can you buy me a snack later?**
   - 나중에 간식 하나 사줄래?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 놓친 내용을 나누어 알려주면 필요한 필기를 바로 정할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **I took notes on the example problems and homework changes. Which part do you need first?**
   - 예제 문제와 과제 변경 내용을 필기했어. 어떤 부분이 먼저 필요해?
2. **I can send photos now or lend you the notebook later. Which would be easier?**
   - 지금 사진을 보내거나 나중에 노트를 빌려줄 수 있어. 뭐가 더 편해?
3. **I helped you today, so how about buying me a snack later?**
   - 오늘 내가 도와줬으니까 나중에 간식 하나 사주는 건 어때?

### 시나리오 9. 조별 발표 준비하기

- 시작 화자. `AI`.
- AI 역할. 조별 발표를 준비하는 팀원.

#### `LEVEL_1`

- 첫 질문 속마음. 발표와 자료 만들기 중 편한 일을 물으면 역할을 쉽게 나눌 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you want to speak or make the slides?**
   - 발표할래, 자료를 만들래?
2. **Can you practice with me after class?**
   - 수업 후에 나와 같이 연습할 수 있어?
3. **Should we meet on Monday or Tuesday?**
   - 월요일과 화요일 중 언제 만날까?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 가능한 역할을 먼저 나누면 발표 준비를 쉽게 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Would you rather present, make the slides, or find information for our project?**
   - 발표, 자료 만들기, 정보 찾기 중 어떤 역할을 하고 싶어?
2. **I get nervous when speaking. Could we practice the presentation together?**
   - 나는 말할 때 긴장해. 우리 발표를 같이 연습할 수 있을까?
3. **Monday afternoon and Tuesday evening both work for me. When should we meet?**
   - 나는 월요일 오후와 화요일 저녁에 가능해. 언제 만날까?

### 시나리오 33. 과제 제출 방식 선택하고 준비하기

- 시작 화자. `AI`.
- AI 역할. 과제 선택지를 설명하는 수업 조교.

#### `LEVEL_1`

- 첫 질문 속마음. 보고서와 발표를 짧게 비교하면 쉽게 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **You can write three pages or give a five-minute talk. Which do you want?**
   - 세 쪽을 쓰거나 5분 발표를 할 수 있어요. 어느 쪽이 좋아요?
2. **Would an example or a checklist help you more?**
   - 예시와 확인 목록 중 어떤 게 더 도움이 될까요?
3. **The work is due Friday. Can you finish by then?**
   - 과제는 금요일까지예요. 그때까지 끝낼 수 있나요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 두 과제 형식과 분량을 알려주면 자신에게 맞는 방식을 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **You can write a three-page report or give a five-minute presentation. Which format suits you better?**
   - 세 쪽 보고서나 5분 발표 중 하나를 할 수 있어요. 어느 형식이 더 잘 맞나요?
2. **What makes that choice easier for you: writing, speaking, or preparing quickly?**
   - 글쓰기, 말하기, 빠른 준비 중 어떤 점 때문에 그 선택이 더 편한가요?
3. **The deadline is Friday at five. Tell me how you will make sure it is ready.**
   - 마감은 금요일 5시예요. 제때 준비하려면 어떻게 할지 말해 주세요.

### 시나리오 31. 수강 과목을 지도교수와 상담하기

- 시작 화자. `AI`.
- AI 역할. 수강 과목을 상담하는 지도교수.

#### `LEVEL_1`

- 첫 질문 속마음. 프로젝트와 시험 중 편한 방식을 물으면 과목을 쉽게 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you like doing projects each week or studying for big tests?**
   - 매주 프로젝트 하는 것과 큰 시험을 준비하는 것 중 어느 쪽이 좋아요?
2. **The project class needs weekly work. The test class has two exams. Which class do you choose?**
   - 프로젝트 수업은 매주 과제가 있고, 시험 수업은 시험이 두 번 있어요. 어느 수업을 고르겠어요?
3. **Can you study Tuesday morning or Thursday afternoon?**
   - 화요일 오전과 목요일 오후 중 언제 수업을 들을 수 있나요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 평가 방식을 비교해서 알려주면 자신에게 맞는 과목을 판단하기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you prefer steady weekly projects or studying for a few large exams?**
   - 매주 꾸준히 프로젝트를 하는 것과 몇 번의 큰 시험을 준비하는 것 중 어느 쪽이 좋아요?
2. **One course has weekly projects, and the other has two exams. Which course would you choose?**
   - 한 과목은 매주 프로젝트가 있고, 다른 과목은 시험이 두 번 있어요. 어느 과목을 고르겠어요?
3. **The class meets Tuesday morning or Thursday afternoon. Which time works with your schedule?**
   - 수업은 화요일 오전이나 목요일 오후예요. 어느 시간이 시간표에 맞나요?

### 시나리오 12. 토론 수업 — 돈과 행복

- 시작 화자. `AI`.
- AI 역할. 토론 수업의 조원.

#### `LEVEL_1`

- 첫 질문 속마음. 행복하게 하는 쉬운 일을 물으면 주제에 부담 없이 들어갈 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What makes you happy: good food, fun games, or time with friends?**
   - 맛있는 음식, 재미있는 게임, 친구와 보내는 시간 중 무엇이 너를 행복하게 해?
2. **Can money buy some of those things?**
   - 돈으로 그중 몇 가지를 살 수 있을까?
3. **What is one happy thing that costs no money?**
   - 돈이 들지 않는 행복한 일 한 가지는 뭐야?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 돈으로 살 수 있는 즐거움부터 생각하면 토론 주제를 쉽게 이해할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Money can buy food, games, and trips. Which of these can make people happy?**
   - 돈으로 음식, 게임, 여행을 살 수 있어. 그중 무엇이 사람을 행복하게 할 수 있을까?
2. **Do you think money makes life easier? Give one simple example.**
   - 돈이 삶을 더 편하게 만든다고 생각해? 쉬운 예를 하나 들어줘.
3. **What free activity can make a person happy? Give one example.**
   - 돈이 들지 않으면서 사람을 행복하게 하는 활동은 뭐가 있을까? 예를 하나 들어줘.

### 시나리오 10. 교수님 오피스아워 방문

- 시작 화자. `USER`.
- AI 역할. 과제 성적을 상담하는 교수.
- 사용자 시작 안내. 낮은 과제 점수의 이유와 개선 방법을 정중하게 묻는다.

#### `LEVEL_1`

1. **Please tell me which part of your grade you want to ask about.**
   - 성적의 어떤 부분을 묻고 싶은지 말해 주세요.
2. **Your ideas were good, but the examples were short. Can you add more examples next time?**
   - 생각은 좋았지만 예시가 짧았어요. 다음에는 예시를 더 넣을 수 있나요?
3. **Do you want my feedback by email or in person tomorrow?**
   - 피드백을 이메일로 받을래요, 내일 직접 들을래요?

#### `LEVEL_2_TO_3`

1. **Tell me which part of the grade surprised you most, and I will explain it.**
   - 성적에서 가장 뜻밖이었던 부분을 말해 주세요. 제가 설명해 드릴게요.
2. **Your main idea was clear, but you needed stronger examples. How could you improve that next time?**
   - 중심 생각은 분명했지만 더 좋은 예시가 필요했어요. 다음에는 어떻게 보완할 수 있을까요?
3. **I can send detailed feedback by email or meet tomorrow. Which would help you more?**
   - 자세한 피드백을 이메일로 보내거나 내일 만날 수 있어요. 어느 쪽이 더 도움이 될까요?

## 쇼핑

### 시나리오 40. 유심과 요금제 구매하기

- 시작 화자. `USER`.
- AI 역할. 통신사 매장 직원.
- 사용자 시작 안내. 유심을 사러 왔다고 말하고 요금제를 묻는다.

#### `LEVEL_1`

1. **Do you use your phone mostly for messages or for videos?**
   - 휴대폰을 주로 메시지에 쓰세요, 영상에 쓰세요?
2. **Ten gigabytes costs $30. Unlimited data costs $45. Which do you want?**
   - 10기가는 30달러, 무제한은 45달러예요. 어떤 걸 원하세요?
3. **Do you want to pay now or pay automatically each month?**
   - 지금 결제할까요, 매달 자동으로 결제할까요?

#### `LEVEL_2_TO_3`

1. **Do you mainly use mobile data for messages, maps, or watching videos?**
   - 휴대폰 데이터를 주로 메시지, 지도, 영상 보기 중 어디에 쓰시나요?
2. **The 10-gigabyte plan is $30, and unlimited data is $45. Which plan fits you better?**
   - 10기가 요금제는 30달러, 무제한은 45달러예요. 어느 요금제가 더 잘 맞나요?
3. **Would you like automatic monthly payment, or would you rather add money yourself?**
   - 매달 자동 결제로 할까요, 아니면 직접 충전하시겠어요?

### 시나리오 34. 식료품점에서 대체 재료 찾기

- 시작 화자. `AI`.
- AI 역할. 식료품점 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 만들 요리를 물으면 쉬운 대체 재료를 추천할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **We have no fresh basil. What are you cooking?**
   - 생바질이 없어요. 어떤 요리를 만드시나요?
2. **You can use dried basil or parsley. Which do you want?**
   - 말린 바질이나 파슬리를 쓸 수 있어요. 어떤 걸 원하세요?
3. **Tomatoes are on sale. Do you want some?**
   - 토마토가 할인 중이에요. 좀 필요하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 요리를 확인하면 맛에 맞는 대체 재료를 안내할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **We're out of fresh basil today. What dish are you making so I can suggest another ingredient?**
   - 오늘 생바질이 다 나갔어요. 다른 재료를 추천할 수 있게 어떤 요리인지 알려주시겠어요?
2. **Dried basil has a similar flavor, while parsley tastes fresher. Which would you prefer?**
   - 말린 바질은 맛이 비슷하고, 파슬리는 더 산뜻해요. 어느 쪽을 원하세요?
3. **Tomatoes are 20 percent off with either one. Would you like to add them?**
   - 둘 중 하나와 같이 사면 토마토가 20% 할인돼요. 추가하시겠어요?

### 시나리오 35. 상황에 맞는 옷 추천받기

- 시작 화자. `AI`.
- AI 역할. 의류 매장 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 편한 옷과 격식 있는 옷 중 필요한 것을 물으면 쉽게 추천할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The casual jacket cannot be returned. The formal blazer can. Which do you want?**
   - 편한 재킷은 반품할 수 없고, 격식 있는 블레이저는 가능해요. 어떤 걸 원하세요?
2. **Would you like navy or beige?**
   - 네이비와 베이지 중 어떤 색이 좋으세요?
3. **Would you like a small, medium, or large size?**
   - 작은, 중간, 큰 사이즈 중 어떤 걸 원하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 행사의 분위기를 물으면 맞는 옷을 추천하기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The casual jacket cannot be returned, while the formal blazer can. Which suits your event better?**
   - 편한 재킷은 반품할 수 없고, 격식 있는 블레이저는 가능해요. 행사에는 어느 쪽이 더 잘 맞나요?
2. **Both styles come in navy and beige. Which color would you like to try first?**
   - 두 옷 모두 네이비와 베이지가 있어요. 어떤 색부터 입어보시겠어요?
3. **We have small, medium, and large sizes. Which size should I bring?**
   - 작은, 중간, 큰 사이즈가 있어요. 어떤 사이즈로 가져다드릴까요?

### 시나리오 37. 기능과 가격을 비교해 헤드폰 고르기

- 시작 화자. `AI`.
- AI 역할. 전자제품 매장 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 가격과 무게를 간단히 알려주면 쉽게 비교할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Is a low price, light weight, or better sound most important to you?**
   - 낮은 가격, 가벼운 무게, 더 좋은 소리 중 무엇이 가장 중요하세요?
2. **Model A has better sound for $120. Model B is lighter for $80. Which do you want?**
   - A 모델은 소리가 더 좋고 120달러예요. B 모델은 더 가볍고 80달러예요. 어떤 걸 원하세요?
3. **Do you want black, white, or gray?**
   - 검정, 흰색, 회색 중 어떤 색을 원하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 두 제품의 핵심 차이를 먼저 알려주면 필요한 모델을 고르기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **What matters most to you: stronger noise canceling, lighter weight, or a lower price?**
   - 더 강한 소음 차단, 가벼운 무게, 낮은 가격 중 무엇이 가장 중요하세요?
2. **Model A has better noise canceling for $120. Model B is lighter for $80. Which fits your needs?**
   - A 모델은 소음 차단이 더 좋고 120달러예요. B 모델은 더 가볍고 80달러예요. 어느 쪽이 필요에 맞나요?
3. **The model comes in black, white, and gray. Which color should I bring?**
   - 검정, 흰색, 회색이 있어요. 어떤 색으로 가져다드릴까요?

### 시나리오 39. 불량 상품 교환 또는 환불 요청하기

- 시작 화자. `USER`.
- AI 역할. 매장 반품 창구 직원.
- 사용자 시작 안내. 전기주전자가 자꾸 꺼진다고 설명하고 교환이나 환불을 묻는다.

#### `LEVEL_1`

1. **I'm sorry. Can we test the kettle now?**
   - 죄송합니다. 지금 주전자를 확인해 봐도 될까요?
2. **We can give you a new one or return your money. Which do you want?**
   - 새 제품을 드리거나 돈을 돌려드릴 수 있어요. 어느 쪽이 좋으세요?
3. **Would you like a box of tea as a gift?**
   - 선물로 차 한 상자를 받으시겠어요?

#### `LEVEL_2_TO_3`

1. **I'm sorry about the problem. May we test the kettle before we process your request?**
   - 제품 문제로 죄송합니다. 요청을 처리하기 전에 주전자를 확인해 봐도 될까요?
2. **We can replace it today or refund you within five days. Which option would you prefer?**
   - 오늘 교환하거나 5일 안에 환불할 수 있어요. 어느 쪽을 원하세요?
3. **We'd like to offer you a box of tea for the trouble. Would you accept it?**
   - 불편을 드려 차 한 상자를 드리고 싶어요. 받아주시겠어요?

### 시나리오 38. 중고 자전거 상태 확인하고 가격 협상하기

- 시작 화자. `AI`.
- AI 역할. 중고 자전거 개인 판매자.

#### `LEVEL_1`

- 첫 질문 속마음. 먼저 타볼지 살펴볼지 고르게 하면 상태를 쉽게 확인할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **Do you want to ride the bike or look at it first?**
   - 자전거를 먼저 타볼래요, 살펴볼래요?
2. **The price is $200. What price do you want?**
   - 가격은 200달러예요. 얼마를 원하세요?
3. **Can you pay in cash today?**
   - 오늘 현금으로 낼 수 있나요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 시험 주행과 상태 확인 중 하나를 고르게 하면 거래를 차근차근 시작할 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **You can test-ride the bike or check the scratches first. What would you like to do?**
   - 자전거를 타보거나 긁힌 부분을 먼저 볼 수 있어요. 무엇부터 할래요?
2. **I'm asking $200, but I can discuss the price. What price would you offer, and why?**
   - 200달러를 생각하지만 가격을 이야기해 볼 수 있어요. 얼마를 제안하고, 이유는 무엇인가요?
3. **If we agree, can you pay the full price in cash today?**
   - 가격에 합의하면 오늘 전액을 현금으로 낼 수 있나요?

### 시나리오 36. 가격과 특징을 비교해 선물 고르기

- 시작 화자. `AI`.
- AI 역할. 기념품점 직원.

#### `LEVEL_1`

- 첫 질문 속마음. 과자와 머그잔의 가격을 알려주면 쉽게 선물을 고를 수 있겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The snack set is $15. The mug is $25. Which gift do you want?**
   - 과자 세트는 15달러, 머그잔은 25달러예요. 어떤 선물을 원하세요?
2. **Do you want a short message card?**
   - 짧은 메시지 카드를 넣을까요?
3. **Do you want free wrapping or gift wrapping for $3?**
   - 무료 포장과 3달러짜리 선물 포장 중 어떤 걸 원하세요?

#### `LEVEL_2_TO_3`

- 첫 질문 속마음. 가격과 특징을 함께 알려주면 친구에게 맞는 선물을 고르기 쉽겠다.
- 첫 질문 속마음 유형. `GOOD`.

1. **The $15 snack set is easy to share, while the $25 mug lasts longer. Which suits your friend?**
   - 15달러 과자 세트는 나눠 먹기 좋고, 25달러 머그잔은 오래 쓸 수 있어요. 친구에게 어느 쪽이 맞나요?
2. **We can include a free message card. What short message would you like?**
   - 무료 메시지 카드를 넣을 수 있어요. 어떤 짧은 문구를 적을까요?
3. **Standard wrapping is free, and gift wrapping costs $3. Which would you prefer?**
   - 기본 포장은 무료이고 선물 포장은 3달러예요. 어느 쪽을 원하세요?
