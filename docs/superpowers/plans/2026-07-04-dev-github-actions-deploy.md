# Dev GitHub Actions Deploy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Status:** 완료된 LAN-43 당시 실행 계획입니다. 현재 최종 배포 기준은 `.github/workflows/deploy-dev.yml`, `.github/workflows/deploy-prod.yml`, `context-notes.md`를 우선 확인합니다.

**Goal:** 개발자가 수동 실행한 workflow로 Landit BE dev 이미지를 ECR에 push하고 ECS 서비스를 새 배포로 갱신한다.

**Architecture:** 이 repo에는 기존 Dockerfile과 workflow가 없으므로 최소 Dockerfile과 단일 dev 배포 workflow만 추가한다. Terraform task definition이 `latest` 이미지를 보므로 task definition 재등록 없이 ECS `update-service --force-new-deployment`만 수행한다.

**Tech Stack:** GitHub Actions, AWS OIDC, Amazon ECR, Amazon ECS, Docker, Gradle, Java 21.

## Global Constraints

- AWS account: `982529430654`.
- AWS region: `ap-northeast-2`.
- ECS cluster: `develop-landit-cluster`.
- ECS service: `develop-landit-api`.
- ECS task container name: `api`.
- ECR repository: `develop-landit-api`.
- ECR image URI: `982529430654.dkr.ecr.ap-northeast-2.amazonaws.com/develop-landit-api`.
- API port: `8080`.
- Health path: `/actuator/health`.
- Static AWS key를 쓰지 않고 OIDC를 사용한다.
- AWS role ARN은 GitHub variable 또는 secret `AWS_ROLE_ARN`으로 받는다.
- SSM 값과 런타임 secret은 출력하지 않는다.
- Terraform apply/destroy는 이 repo에서 실행하지 않는다.

---

### Task 1: Dev Deploy Workflow

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`
- Create: `.github/workflows/deploy-dev.yml`

**Interfaces:**
- Consumes: Gradle wrapper `./gradlew`, Spring Boot `bootJar`.
- Produces: ECR image tags `${github.sha}` and `latest`, ECS force deployment.

- [x] **Step 1: Add minimal Dockerfile**

Use a two-stage Temurin 21 image and run `./gradlew bootJar --no-daemon`.

- [x] **Step 2: Add `.dockerignore`**

Exclude local build outputs, Git metadata, IDE files, and `.env` files from Docker context.

- [x] **Step 3: Add GitHub Actions workflow**

Trigger only on `workflow_dispatch`. Configure AWS credentials through OIDC, push commit SHA and `latest`, then call `aws ecs update-service --force-new-deployment`.

- [x] **Step 4: Verify**

Run `./gradlew test`, parse workflow YAML, run `git diff --check`, inspect `git diff`, and inspect `git status --short`.

- [x] **Step 5: Commit**

Commit with `deploy: dev ECS 배포 workflow 추가`.
