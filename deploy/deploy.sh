#!/usr/bin/env bash
# 배포 서버에서 앱 이미지를 바꾼다. CD가 SSM Run Command로 root 권한으로 실행한다.
# 사용법: IMAGE_TAG=<커밋 짧은 해시> bash deploy.sh
set -euo pipefail

: "${IMAGE_TAG:?IMAGE_TAG를 지정하세요}"

APP_DIR=/opt/aifilm
CONTAINER=aifilm-app
# 컨테이너 HEALTHCHECK의 start-period(60초)와 재시도 시간을 넉넉히 넘긴다
HEALTH_TIMEOUT_SECONDS=150

cd "$APP_DIR"

current_image=$(grep '^APP_IMAGE=' .env | cut -d= -f2-)
target_image="${current_image%:*}:${IMAGE_TAG}"

set_image() {
  sed -i "s#^APP_IMAGE=.*#APP_IMAGE=$1#" .env
}

wait_healthy() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  local status=unknown
  while ((SECONDS < deadline)); do
    status=$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo missing)
    case "$status" in
      healthy) return 0 ;;
      unhealthy) break ;;
    esac
    sleep 5
  done
  echo "헬스체크 실패 (마지막 상태: $status)"
  return 1
}

echo "배포: $current_image -> $target_image"
set_image "$target_image"

# 없는 태그 등으로 이미지를 받지 못하면 실행 중인 컨테이너는 건드리지 않는다
if ! docker compose pull --quiet; then
  set_image "$current_image"
  echo "이미지를 받지 못해 배포를 중단했습니다."
  exit 1
fi

docker compose up -d

if ! wait_healthy; then
  docker compose logs --tail 50 app || true
  echo "이전 이미지로 되돌립니다: $current_image"
  set_image "$current_image"
  docker compose up -d
  if wait_healthy; then
    echo "이전 이미지로 복구했습니다."
  else
    echo "이전 이미지로도 healthy가 되지 않았습니다. 서버를 직접 확인하세요."
  fi
  exit 1
fi

docker compose ps
echo "배포 완료: $target_image"
