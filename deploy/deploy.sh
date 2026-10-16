#!/usr/bin/env bash
# 배포 서버에서 앱 이미지를 바꾼다. CD가 SSM Run Command로 root 권한으로 실행한다.
# 사용법: IMAGE_TAG=<커밋 짧은 해시> bash deploy.sh
set -euo pipefail

: "${IMAGE_TAG:?IMAGE_TAG를 지정하세요}"

APP_DIR=/opt/aifilm
CONTAINER=aifilm-app
LOCK_FILE=/run/lock/aifilm-deploy.lock
# 이미지 받기가 멈춰도 SSM 실행 제한 안에 끝나도록 상한을 둔다
PULL_TIMEOUT_SECONDS=180
# 컨테이너 HEALTHCHECK의 start-period(60초)와 재시도 시간을 넉넉히 넘긴다
HEALTH_TIMEOUT_SECONDS=150

# 앞선 배포가 아직 서버에서 돌고 있으면 겹쳐서 바꾸지 않는다
exec 9> "$LOCK_FILE"
if ! flock -n 9; then
  echo "다른 배포가 진행 중이라 중단합니다."
  exit 1
fi

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

# 이미지를 먼저 받아 둔다. 받지 못하면 .env와 실행 중인 컨테이너를 건드리지 않는다
if ! timeout "$PULL_TIMEOUT_SECONDS" docker pull --quiet "$target_image"; then
  echo "이미지를 받지 못해 배포를 중단했습니다."
  exit 1
fi

set_image "$target_image"

# 컨테이너를 띄우지 못하거나 healthy가 되지 않으면 이전 이미지로 되돌린다
if ! docker compose up -d || ! wait_healthy; then
  docker compose logs --tail 50 app || true
  echo "이전 이미지로 되돌립니다: $current_image"
  set_image "$current_image"
  if docker compose up -d && wait_healthy; then
    echo "이전 이미지로 복구했습니다."
  else
    echo "이전 이미지로도 healthy가 되지 않았습니다. 서버를 직접 확인하세요."
  fi
  exit 1
fi

docker compose ps
echo "배포 완료: $target_image"
