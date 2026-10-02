# LAN-391 이미지 마이그레이션의 실제 PostgreSQL 매핑, 보존 및 트랜잭션 롤백을 격리 검증한다.
import argparse
from copy import deepcopy
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
MIGRATIONS = ROOT / 'src/main/resources/db/postgresql'
V136 = MIGRATIONS / 'V136__insert_scenario_41_70_writing_expressions.sql'
V138 = MIGRATIONS / 'V138__update_scenario_41_70_practice_example_image_urls.sql'
CDN = 'https://d19azau1un4t7r.cloudfront.net/content/'
PRACTICE_SOURCE = '2c07c44c76446b7c7f1d923f9a154365a3f49bd9cd59b6b0b3e2f52c58d80adc'
REPRESENTATIVE_SOURCE = '5af4794cc120dad8e3e897dc03117cfbcfd37125de23e8305a1e8b69fd4943f4'

# 전체 애플리케이션 스키마 대신 V136/V138이 사용하는 열과 보존 확인용 열을 구성한다.
SCHEMA = '''
CREATE TABLE writing_expression (
 id BIGSERIAL PRIMARY KEY, scenario_id BIGINT, expression_type TEXT,
 usage_frequency_level TEXT, target_locale TEXT, base_locale TEXT,
 display_order INTEGER, target_expression_text TEXT, base_expression_meaning_text TEXT,
 usage_summary TEXT, usage_description TEXT, representative_question_text TEXT,
 representative_question_translation TEXT, representative_sentence_text TEXT,
 representative_sentence_translation TEXT, representative_sentence_words VARCHAR[],
 representative_sentence_word_choices VARCHAR[], representative_image_url TEXT,
 practice_examples_payload JSONB, expression_source TEXT, status TEXT,
 difficulty_level INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP,
 preservation_probe TEXT DEFAULT 'must remain unchanged'
);
CREATE TABLE transaction_probe (value INTEGER);
'''


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_map(sql, name, text_columns):
    values = re.search(rf'INSERT INTO {name}\b.*?VALUES\s*(.*?);\s*DO \$\$', sql, re.S)[1]
    literal = r"'((?:[^']|'')*)'"
    pattern = r'\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*'
    pattern += r',\s*'.join([literal] * text_columns) + r'\)'
    rows = []
    for match in re.finditer(pattern, values):
        rows.append(tuple(int(v) for v in match.groups()[:4])
                    + tuple(v.replace("''", "'") for v in match.groups()[4:]))
    require([r[0] for r in rows] == list(range(4001, 4329)), f'{name}: 328 ordered IDs')
    require([r[3] for r in rows] == list(range(1, 329)), f'{name}: 328 SQL row ordinals')
    return rows


class IsolatedPostgres:
    def __init__(self, binaries, port):
        self.bin = Path(binaries)
        self.port = port
        self.root = Path(tempfile.mkdtemp(prefix='lan391-image-pg-', dir='/tmp'))
        self.data = self.root / 'data'
        self.socket = self.root / 'socket'
        self.socket.mkdir()
        self.env = {k: v for k, v in os.environ.items() if not k.startswith('PG')}
        self.env['LC_ALL'] = 'C'
        self.started = False

    def command(self, name, arguments, **kwargs):
        return subprocess.run([str(self.bin / name), *arguments], env=self.env,
                              text=True, capture_output=True, timeout=60, **kwargs)

    def start(self):
        result = self.command('initdb', ['-D', str(self.data), '--auth=trust',
                                       '--no-locale', '-E', 'UTF8', '-U', 'lan391_test'])
        require(result.returncode == 0, result.stderr)
        options = f"-F -h '' -k {self.socket} -p {self.port}"
        result = self.command('pg_ctl', ['-D', str(self.data), '-l', str(self.root / 'server.log'),
                                        '-o', options, '-w', 'start'])
        self.started = (self.data / 'postmaster.pid').exists()
        require(result.returncode == 0, result.stderr + (self.root / 'server.log').read_text())

    def sql(self, sql, failure=None):
        result = self.command('psql', ['-X', '-h', str(self.socket), '-p', str(self.port),
                                      '-U', 'lan391_test', '-d', 'postgres', '-A', '-t', '-q',
                                      '-v', 'ON_ERROR_STOP=1', '--single-transaction', '-f', '-'],
                              input=sql)
        if failure:
            require(result.returncode != 0 and failure in result.stderr,
                    f'Expected {failure!r}, got {result.returncode}: {result.stderr}')
        else:
            require(result.returncode == 0, result.stderr)
        return result.stdout.strip()

    def snapshot(self):
        return json.loads(self.sql(
            'SELECT json_agg(to_jsonb(w) ORDER BY id) FROM writing_expression w;'))

    def close(self):
        if self.started or (self.data / 'postmaster.pid').exists():
            result = self.command('pg_ctl', ['-D', str(self.data), '-m', 'fast', '-w', 'stop'])
            require(result.returncode == 0, f'Server cleanup failed; retained {self.root}: {result.stderr}')
        require(not (self.data / 'postmaster.pid').exists(), 'Server still running')
        shutil.rmtree(self.root)


def expected_rows(baseline, practices, representatives):
    expected = deepcopy(baseline)
    by_id = {row['id']: row for row in expected}
    for rid, scenario, order, ordinal, en3, en4, sha3, sha4 in practices:
        row = by_id[rid]
        require((row['scenario_id'], row['display_order']) == (scenario, order), f'{rid}: practice identity')
        for number, sentence, digest in [(3, en3, sha3), (4, en4, sha4)]:
            item = row['practice_examples_payload'][number - 1]
            require(item['sentenceText'] == sentence, f'{rid}.{number}: practice sentence')
            item['imageUrl'] = (f'{CDN}scenario-expression-practice/sha256-{PRACTICE_SOURCE}/'
                                f'scenario-order-{scenario}/sql-row-{ordinal:04d}/example-{number}/{digest}.webp')
    for rid, scenario, order, ordinal, en, ko, qen, qko, digest in representatives:
        row = by_id[rid]
        fields = ['representative_sentence_text', 'representative_sentence_translation',
                  'representative_question_text', 'representative_question_translation']
        require((row['scenario_id'], row['display_order']) == (scenario, order), f'{rid}: representative identity')
        require([row[field] for field in fields] == [en, ko, qen, qko], f'{rid}: representative text')
        row['representative_image_url'] = (
            f'{CDN}scenario-expression-representative/sha256-{REPRESENTATIVE_SOURCE}/'
            f'scenario-order-{scenario}/sql-row-{ordinal:04d}/{digest}.webp')
    return expected


def verify(db, source_sql, migration_sql):
    practices = read_map(migration_sql, 'lan391_practice_image_map', 4)
    representatives = read_map(migration_sql, 'lan391_representative_image_map', 5)
    db.sql(SCHEMA + source_sql)
    require(len(db.snapshot()) == 328, 'V136 must insert exactly 328 expressions')
    db.sql('''
      INSERT INTO writing_expression(id, scenario_id, representative_image_url, practice_examples_payload)
      VALUES (3999, 44, 'preserved-neighbor', '[{"imageUrl":"neighbor"}]'),
             (4329, 999, 'preserved-outside-scenario', '{"keep":true}');
      UPDATE writing_expression SET practice_examples_payload = jsonb_set(
        practice_examples_payload, '{2,preservationProbe}', '{"keep":[1,true,null]}'::jsonb)
      WHERE id = 4001;
      CREATE TABLE baseline AS SELECT * FROM writing_expression;
    ''')
    baseline = db.snapshot()
    sequence = db.sql('SELECT last_value FROM writing_expression_id_seq;')
    expected = expected_rows(baseline, practices, representatives)
    db.sql(migration_sql)
    require(db.snapshot() == expected, 'Exact 984 URL mapping or other-field preservation failed')
    require(db.sql('SELECT last_value FROM writing_expression_id_seq;') == sequence, 'Sequence changed')
    checks = ['V136 inserts 328 expressions', '328 representative and 656 practice URLs match exactly',
              'all other fields, nested JSON keys, timestamps, sequence and two unrelated rows preserved']
    db.sql(migration_sql, failure='LAN-391 image mapping differs from V136')
    require(db.snapshot() == expected, 'Rerun changed data')
    checks.append('rerun rejected without changes')

    cases = []
    for field in ['representative_sentence_text', 'representative_sentence_translation',
                  'representative_question_text', 'representative_question_translation']:
        for label, value in [('changed', "'drift'"), ('null', 'NULL')]:
            cases.append((f'{field}_{label}', f'{field} = {value}', 'representative mapping differs'))
    cases += [
        ('existing_representative_url', "representative_image_url = 'already-set'", 'representative mapping differs'),
        ('scenario_drift', 'scenario_id = 999', 'image mapping differs'),
        ('display_order_drift', 'display_order = 999', 'image mapping differs'),
        ('practice_sql_null', 'practice_examples_payload = NULL', 'image mapping differs'),
        ('practice_json_null', "practice_examples_payload = 'null'::jsonb", 'image mapping differs'),
        ('practice_object', "practice_examples_payload = '{}'::jsonb", 'image mapping differs'),
        ('practice_short_array', "practice_examples_payload = practice_examples_payload - 3", 'image mapping differs'),
        ('missing_practice_url_key', "practice_examples_payload = practice_examples_payload #- '{2,imageUrl}'", 'image mapping differs'),
        ('existing_practice_url', "practice_examples_payload = jsonb_set(practice_examples_payload, '{3,imageUrl}', '\"already-set\"')", 'image mapping differs'),
        ('practice_string_null', "practice_examples_payload = jsonb_set(practice_examples_payload, '{2,imageUrl}', '\"null\"')", 'image mapping differs'),
        ('practice_sentence_drift', "practice_examples_payload = jsonb_set(practice_examples_payload, '{2,sentenceText}', '\"drift\"')", 'image mapping differs'),
        ('missing_target', None, 'image mapping differs'),
    ]
    for name, mutation, error in cases:
        db.sql('TRUNCATE writing_expression; INSERT INTO writing_expression SELECT * FROM baseline;')
        db.sql(f'UPDATE writing_expression SET {mutation} WHERE id = 4001;' if mutation
               else 'DELETE FROM writing_expression WHERE id = 4001;')
        before = db.snapshot()
        db.sql('INSERT INTO transaction_probe VALUES (1);\n' + migration_sql, failure=error)
        require(db.snapshot() == before, f'{name}: transaction did not fully roll back')
        require(db.sql('SELECT count(*) FROM transaction_probe;') == '0', f'{name}: transaction probe persisted')
        checks.append(f'{name}: rejected and entire transaction rolled back')
    return checks


def main():
    parser = argparse.ArgumentParser(description='Disposable local PostgreSQL only; no external database options.')
    parser.add_argument('--pg-bin', default='/opt/homebrew/opt/postgresql@15/bin')
    parser.add_argument('--port', type=int, default=55483)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    source_sql, migration_sql = V136.read_text(), V138.read_text()
    db = IsolatedPostgres(args.pg_bin, args.port)
    report = {'schema_scope': 'isolated V136/V138 fixture, not the complete application migration chain',
              'v136_sha256': hashlib.sha256(V136.read_bytes()).hexdigest(),
              'v138_sha256': hashlib.sha256(V138.read_bytes()).hexdigest(),
              'transport': 'temporary UNIX socket only; TCP listening disabled', 'port': args.port}
    try:
        db.start()
        report['postgresql_version'] = db.sql('SELECT version();')
        report['checks'] = verify(db, source_sql, migration_sql)
        report['status'] = 'PASS'
    finally:
        db.close()
    report['server_stopped_and_directory_removed'] = not db.root.exists()
    encoded = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if args.report:
        args.report.write_text(encoded)
    print(encoded, end='')


if __name__ == '__main__':
    main()
