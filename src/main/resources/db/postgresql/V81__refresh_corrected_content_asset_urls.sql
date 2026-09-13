-- LAN-405 보정 이미지와 질문 음성을 새 immutable URL로 전환한다.
SELECT pg_advisory_xact_lock(hashtext('lan405-corrected-content-asset-urls'));
LOCK TABLE writing_expression, scenario_question_language_variant IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE lan405_practice_image_url_updates (
    expression_id BIGINT NOT NULL,
    example_index INTEGER NOT NULL CHECK (example_index BETWEEN 1 AND 4),
    old_url TEXT NOT NULL UNIQUE,
    new_url TEXT NOT NULL UNIQUE,
    PRIMARY KEY (expression_id, example_index)
) ON COMMIT DROP;

INSERT INTO lan405_practice_image_url_updates (
    expression_id, example_index, old_url, new_url
) VALUES
    (22, 4, 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/eb034584-8774-4259-a7f0-deaeedb0f929.webp', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/6/expressions/22/practice-examples/8a351259-56a0-49ac-bca3-5a306fb6683e.webp'),
    (121, 3, 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/1abc1bdf-ecad-4fa3-b86c-abfd8a550644.webp', 'https://d19azau1un4t7r.cloudfront.net/content/scenarios/30/expressions/121/practice-examples/c8a84458-2fa4-492f-a22d-3e49a54f9631.webp'),
    (176, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/176/practice-examples/2b5ee4c1-c404-496f-8f7f-878bb2621f01.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/176/practice-examples/ea3b0fcf-f22f-4be5-b679-eadc1efa004f.webp'),
    (208, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/208/practice-examples/9f7d4188-65a1-48b1-9e97-0eb107b613bb.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/208/practice-examples/e00a14f4-126a-4830-993f-8b40e726c488.webp'),
    (335, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/335/practice-examples/f9a49fa1-6ad7-4064-b064-bafc33eb399d.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/335/practice-examples/c006cf40-df54-4c47-a34c-ea6ac1b111ab.webp'),
    (386, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/386/practice-examples/21577f25-dcb4-49b1-af00-ba30f9817c33.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/386/practice-examples/2c868c89-e240-480d-ae9e-c76f0a1a7774.webp'),
    (446, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/446/practice-examples/6c72066a-1f0a-4358-b96b-ae62b86bbeee.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/446/practice-examples/41173add-0286-4795-b423-aa90ba699d1b.webp'),
    (470, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/470/practice-examples/a7e65d7f-2261-451d-8d7d-ef37aa5dad66.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/470/practice-examples/1148fe04-68a1-4f86-a5bb-79190fe0e658.webp'),
    (540, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/540/practice-examples/2633c53c-223c-4e5d-899d-305e2f37d745.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/540/practice-examples/b6d8689d-9c51-4859-8d5c-ce13c663a346.webp'),
    (779, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/779/practice-examples/dc3c141e-4ab6-426a-831d-d33152d12dad.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/779/practice-examples/57782e93-3f99-41dd-b046-4b85c0ea631f.webp'),
    (928, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/928/practice-examples/3c841fa3-65da-424e-8026-5ccd8f578121.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/928/practice-examples/d38c6036-e56f-4374-b52d-1a3fdd9eb3e0.webp'),
    (959, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/959/practice-examples/0ae512b9-ef59-48c1-ae8a-3987943f69ce.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/959/practice-examples/74df401a-5a1d-4866-b20f-f5228a636862.webp'),
    (972, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/972/practice-examples/55b0c3e9-d6a5-4797-8e92-cb55a6bf1fcf.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/972/practice-examples/6329cc1e-6312-4bc2-860b-ba6e7ce76bb1.webp'),
    (1167, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1167/practice-examples/c9afbe4f-9b8e-44ea-a6a7-ccd4a2d6783c.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1167/practice-examples/26a77fbb-65f5-43ba-b864-8ef223c0c898.webp'),
    (1224, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1224/practice-examples/18151e8f-8f25-4cc4-bed2-18403a8692e6.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1224/practice-examples/790563d5-82ea-49ae-b288-0787d1b7327a.webp'),
    (1231, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1231/practice-examples/e508dbb3-19f1-4f1d-aa99-ddc28d7239a2.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1231/practice-examples/44ba77af-9c91-429a-ad8c-a39108f8d3c8.webp'),
    (1237, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1237/practice-examples/1da6fd98-3732-42b3-b81d-76ae723bfd57.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1237/practice-examples/de4f8268-7dcf-4874-888e-dadef682820f.webp'),
    (1266, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1266/practice-examples/229e5e74-b673-492b-9a13-b0eab75ed152.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1266/practice-examples/cdbdfca4-d871-4ba7-93e7-f4e0163d07cf.webp'),
    (1268, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1268/practice-examples/ff4e0a5e-2f4e-44de-b251-385234803897.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1268/practice-examples/4d65930a-5ac5-4c34-89ef-aa98a04299c7.webp'),
    (1387, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1387/practice-examples/eb8e6cd8-178f-462e-88e8-529d031fe37c.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1387/practice-examples/6ca99438-975a-4ff2-ada4-ca85b058f9ae.webp'),
    (1387, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1387/practice-examples/a7748fed-4a01-4eb6-80ae-87ff1ba14536.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1387/practice-examples/4dbc9a90-7624-4c72-9a61-7e55441ed51f.webp'),
    (1451, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1451/practice-examples/fe4a3a35-f7d0-4fdb-9ff8-3aa962485aa4.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1451/practice-examples/83c040a5-b121-47b1-8f78-ec3333f580bb.webp'),
    (1451, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1451/practice-examples/e27e3306-ddf7-43b4-875f-031298955c29.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1451/practice-examples/a20954ae-119d-4bbf-b9b5-058610ef0f82.webp'),
    (1455, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1455/practice-examples/64bc55bb-0483-41f0-85ed-0174f8b977a3.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1455/practice-examples/cee30181-1b82-469e-970c-968a57afbcb1.webp'),
    (1461, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1461/practice-examples/e7b54713-13d9-4576-885a-3335ed33f16e.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1461/practice-examples/b1f0166c-78d0-482e-82be-09f1bace8296.webp'),
    (1492, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1492/practice-examples/9a800111-c574-4a69-a1b0-e0108e8635b4.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1492/practice-examples/3af286f7-4631-4187-b991-119c85f857c6.webp'),
    (1525, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1525/practice-examples/2a12c8c1-8f08-41bb-848d-04341267388c.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1525/practice-examples/e875b59b-9e35-489d-9d76-50f97c09a8d3.webp'),
    (1529, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1529/practice-examples/e9af93f5-c4d3-4f37-8d72-19d4b831f8ef.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1529/practice-examples/7cd915e8-d4a2-4ad5-98f3-8009b933ccad.webp'),
    (1531, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1531/practice-examples/a019d05e-2eae-4d17-8cbb-772241c73a3c.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1531/practice-examples/42abd6a8-18f0-4226-90b8-ca17b6ab523b.webp'),
    (1562, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1562/practice-examples/6ce556fc-6f7b-4523-a112-2b15cf56bab1.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1562/practice-examples/40549640-60ec-42f7-bea6-acbcbc64a3e5.webp'),
    (1586, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1586/practice-examples/a723b91c-8a91-4fde-baeb-cc80d2756f05.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1586/practice-examples/957c2da7-79f0-425c-b3a7-19b8c586db95.webp'),
    (1587, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1587/practice-examples/b7d0fad9-9028-45d2-9ada-4a5b0cadf580.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1587/practice-examples/3080133b-1d8a-483f-8fce-9bbabcce127a.webp'),
    (1724, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1724/practice-examples/7fbcd231-4ed4-4273-8857-a7876f51957b.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1724/practice-examples/919c831e-b5af-4df8-ac35-7d02f96a976d.webp'),
    (1725, 3, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1725/practice-examples/9011d432-5d22-46da-ad6c-987875674494.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1725/practice-examples/c6342507-e003-4252-b431-b6dea7e0359a.webp'),
    (1728, 4, 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1728/practice-examples/d6daa5c0-90e2-40c9-8234-0dc81182c443.webp', 'https://d19azau1un4t7r.cloudfront.net/content/expressions/1728/practice-examples/380d5f16-b29f-45f4-b069-d77b9c5b9cf7.webp');

CREATE TEMP TABLE lan405_question_audio_url_updates (
    scenario_question_id BIGINT PRIMARY KEY,
    old_url TEXT NOT NULL UNIQUE,
    new_url TEXT NOT NULL UNIQUE
) ON COMMIT DROP;

INSERT INTO lan405_question_audio_url_updates (
    scenario_question_id, old_url, new_url
) VALUES
    (13, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/13/6fa0c678f69adc5e355ec9582c594e6a6e3ab1beeda17e33a7fe3f4e5db2336a.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/13/revisions/3b8e678a18a85b2283350011647d4dbf71213663ac6eb3c28d62c74fc52fd9bb.mp3'),
    (14, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/14/c794267a8f51058f35fc3bd3205c5a64982a60654aaadc67d118f8f1a4129121.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/14/revisions/966f20ab38c34c561ab713c6599a528cc4d5221a8fcadbefb9caae474c265014.mp3'),
    (21, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/21/a5e47a4e3af632ccf88683bfb32b02192ed3007d739b739c3f00e1c8db999551.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/21/revisions/5741c381551b09e971a3fd1c270b5f42f9e757b0f071b828d4f25e04803bd216.mp3'),
    (56, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/56/f64c57b6c30e447f4b72de2cf688571a6d480262e85adeff134c56ffe2f2a7ea.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/56/revisions/762e94255f4cb9ed4e99e651a0c31da79b9694d62b9a586d4b1943602975abd4.mp3'),
    (96, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/96/1f063f3af129982ebddf6727248a6eb29649557391b9f99604629c18368e16d1.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/96/revisions/e34f4578a4535858f05fb5c7e6585a61390e3a3175358645bb379ff34f57588f.mp3'),
    (111, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/111/4e10cb3a2c91e1b4109e5da469eabd9159c7068caaacfd4ea739d96d25464a10.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/111/revisions/f5ea24ce8a34495a5e7f80906bb72bd7e7fad6b85a5fadbcc48d61fe752bfb43.mp3'),
    (124, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/124/4e90ea1e70e875fe4964047d356480ec9c1d69d7d0af4070d89299112235bbdc.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/124/revisions/024232b8b604a09db2a83fbc95ede76d282864470cda824dfa56651ed1c269c2.mp3'),
    (128, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/128/4e7a5f4f4a7dadbaa05a74b7195282aa7a8ef80bd9baba71db3af1db669cb0df.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/128/revisions/7d8918278f5e111657d72bea5c59e4d1f6b9733dddc9c311f8d6474b0343b15f.mp3'),
    (142, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/142/1cd3618117611ad85674e6c69072ea65e01775cf7c591f86b7c425c5fd8df3b0.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/142/revisions/d4d0af1d9e7a830a99a2132b1b612080afeba7885485dbb705871e6380ab5ef8.mp3'),
    (158, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/158/69d450c32de49f15b4dde1935933f6266207f3cdc94bf4bc6fec7ec680d44a22.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/158/revisions/81cc74871e901b5e7a29ad8b86735e217721366661b28575d4ec650a6870cbd8.mp3'),
    (245, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/245/342bfb75c9850bf777ff35c8b50f6f408666d9453f41bcde29a130f7acb5b08f.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/245/revisions/9942d2afc554c6dcce0324fe086f2432ca273f0648e3ff1cfc17149886b79085.mp3'),
    (252, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/252/8e70f99272c196d24f2c734a95a574a086916c2ff215a9172f5db26090f9277a.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/252/revisions/4bab29e4596a4d663d07d25b361b0e8a61a58f5841175d91094e6c59b1073b36.mp3'),
    (298, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/298/13b111675e1bf042a40b59b08e7d2cd236e15f6cca41d44851d5b67f699c51e7.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/298/revisions/7beb2a28b5639e7842c1ba09ae97d735a99dea11f9b30b8e13c76dbe6225864f.mp3'),
    (299, 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/299/8ca98ac106cd298b3c45611a108f0e1f562792bb91ddf468e7652ebee26ec0dc.mp3', 'https://d19azau1un4t7r.cloudfront.net/content/scenario-question-audio/299/revisions/bce23eda6362f2ff510b7e7171ef6518ecb575b995cc8794454f1a461a047896.mp3');

DO $$
DECLARE
    image_mapping_count INTEGER;
    matched_image_count INTEGER;
    audio_mapping_count INTEGER;
    matched_audio_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO image_mapping_count
    FROM lan405_practice_image_url_updates;
    IF image_mapping_count <> 35 THEN
        RAISE EXCEPTION 'LAN-405 requires 35 image URL mappings, got %', image_mapping_count;
    END IF;

    SELECT COUNT(*) INTO matched_image_count
    FROM lan405_practice_image_url_updates mapping
    JOIN writing_expression expression
      ON expression.id = mapping.expression_id
     AND expression.practice_examples_payload -> (mapping.example_index - 1) ->> 'imageUrl'
         = mapping.old_url;
    IF matched_image_count <> 35 THEN
        RAISE EXCEPTION 'LAN-405 image URL precondition failed: matched % of 35', matched_image_count;
    END IF;

    SELECT COUNT(*) INTO audio_mapping_count
    FROM lan405_question_audio_url_updates;
    IF audio_mapping_count <> 14 THEN
        RAISE EXCEPTION 'LAN-405 requires 14 audio URL mappings, got %', audio_mapping_count;
    END IF;

    SELECT COUNT(*) INTO matched_audio_count
    FROM lan405_question_audio_url_updates mapping
    JOIN scenario_question_language_variant variant
      ON variant.scenario_question_id = mapping.scenario_question_id
     AND variant.target_locale = 'EN'
     AND variant.base_locale = 'KR'
     AND variant.audio_url = mapping.old_url;
    IF matched_audio_count <> 14 THEN
        RAISE EXCEPTION 'LAN-405 audio URL precondition failed: matched % of 14', matched_audio_count;
    END IF;
END $$;

WITH patched_images AS (
    SELECT expression.id,
           jsonb_agg(
               CASE
                   WHEN mapping.new_url IS NULL THEN example.value
                   ELSE jsonb_set(
                       example.value,
                       ARRAY['imageUrl'],
                       to_jsonb(mapping.new_url),
                       false
                   )
               END
               ORDER BY example.ordinality
           ) AS payload
    FROM writing_expression expression
    CROSS JOIN LATERAL jsonb_array_elements(expression.practice_examples_payload)
        WITH ORDINALITY AS example(value, ordinality)
    LEFT JOIN lan405_practice_image_url_updates mapping
      ON mapping.expression_id = expression.id
     AND mapping.example_index = example.ordinality
    WHERE EXISTS (
        SELECT 1
        FROM lan405_practice_image_url_updates target
        WHERE target.expression_id = expression.id
    )
    GROUP BY expression.id
)
UPDATE writing_expression target
SET practice_examples_payload = patched.payload,
    updated_at = CURRENT_TIMESTAMP
FROM patched_images patched
WHERE target.id = patched.id;

UPDATE scenario_question_language_variant variant
SET audio_url = mapping.new_url,
    updated_at = CURRENT_TIMESTAMP
FROM lan405_question_audio_url_updates mapping
WHERE variant.scenario_question_id = mapping.scenario_question_id
  AND variant.target_locale = 'EN'
  AND variant.base_locale = 'KR'
  AND variant.audio_url = mapping.old_url;

DO $$
DECLARE
    verified_image_count INTEGER;
    remaining_old_image_count INTEGER;
    verified_audio_count INTEGER;
    remaining_old_audio_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO verified_image_count
    FROM lan405_practice_image_url_updates mapping
    JOIN writing_expression expression
      ON expression.id = mapping.expression_id
     AND expression.practice_examples_payload -> (mapping.example_index - 1) ->> 'imageUrl'
         = mapping.new_url;
    IF verified_image_count <> 35 THEN
        RAISE EXCEPTION 'LAN-405 image URL postcondition failed: verified % of 35', verified_image_count;
    END IF;

    SELECT COUNT(*) INTO remaining_old_image_count
    FROM lan405_practice_image_url_updates mapping
    JOIN writing_expression expression
      ON expression.practice_examples_payload @> jsonb_build_array(
          jsonb_build_object('imageUrl', mapping.old_url)
      );
    IF remaining_old_image_count <> 0 THEN
        RAISE EXCEPTION 'LAN-405 old image URLs remain: %', remaining_old_image_count;
    END IF;

    SELECT COUNT(*) INTO verified_audio_count
    FROM lan405_question_audio_url_updates mapping
    JOIN scenario_question_language_variant variant
      ON variant.scenario_question_id = mapping.scenario_question_id
     AND variant.target_locale = 'EN'
     AND variant.base_locale = 'KR'
     AND variant.audio_url = mapping.new_url;
    IF verified_audio_count <> 14 THEN
        RAISE EXCEPTION 'LAN-405 audio URL postcondition failed: verified % of 14', verified_audio_count;
    END IF;

    SELECT COUNT(*) INTO remaining_old_audio_count
    FROM lan405_question_audio_url_updates mapping
    JOIN scenario_question_language_variant variant
      ON variant.audio_url = mapping.old_url;
    IF remaining_old_audio_count <> 0 THEN
        RAISE EXCEPTION 'LAN-405 old audio URLs remain: %', remaining_old_audio_count;
    END IF;
END $$;
