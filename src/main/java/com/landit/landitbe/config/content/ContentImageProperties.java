// 콘텐츠 이미지 저장소와 조회 주소 설정을 바인딩한다.

package com.landit.landitbe.config.content;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 콘텐츠 이미지 저장소와 조회 주소 설정이다.
 *
 * @param bucketName 이미지를 업로드할 S3 버킷 이름
 * @param cloudfrontUrl 업로드 이미지의 CloudFront 기본 URL
 * @param region S3 버킷 AWS 리전
 */
@ConfigurationProperties(prefix = "landit.content-image")
public record ContentImageProperties(String bucketName, URI cloudfrontUrl, String region) {}
