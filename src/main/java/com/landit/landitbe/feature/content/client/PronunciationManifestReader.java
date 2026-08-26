// 발음 평가 자산 매니페스트 JSON 원문을 읽어온다.

package com.landit.landitbe.feature.content.client;

/**
 * 발음 평가 자산 매니페스트 JSON 원문을 읽어온다.
 *
 * <p>운영에서는 S3 콘텐츠 버킷에서 내려받고, 테스트·로컬에서는 클래스패스 리소스로 대체한다. 구현 선택은
 * {@code landit.pronunciation-asset.manifest-mode} 설정을 따른다.
 */
public interface PronunciationManifestReader {

  /**
   * 매니페스트 키에 해당하는 JSON 원문을 읽는다.
   *
   * @param manifestKey 매니페스트 키 (예: content/expression-pronunciation-audio/manifests/2026-08-26.json)
   * @return 매니페스트 JSON 문자열
   * @throws com.landit.landitbe.shared.exception.ApiException 매니페스트가 존재하지 않을 때
   */
  String read(String manifestKey);
}
