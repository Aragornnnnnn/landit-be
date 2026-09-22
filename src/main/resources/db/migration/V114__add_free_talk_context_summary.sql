-- 프리톡 원문과 분리된 세션 요약 파생 상태를 저장한다.
create table free_talk_context_summary (
    free_talk_session_id bigint primary key references free_talk_session (id) on delete cascade,
    policy_version varchar(20) not null,
    summary_content jsonb,
    covered_through_sequence integer not null default 0,
    revision integer not null default 0,
    lease_token varchar(36),
    lease_until timestamp with time zone,
    next_attempt_at timestamp with time zone,
    source_byte_limit integer not null default 6000,
    suspended_reason varchar(40),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create index idx_free_talk_context_summary_next_attempt
    on free_talk_context_summary (next_attempt_at);
