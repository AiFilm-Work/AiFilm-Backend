# 배포 절차

배포 서버(EC2) 한 대에서 앱 컨테이너를 띄우고, DB는 RDS(MySQL 8.4)를 쓴다. 이미지는 ECR에 커밋 해시 태그로 올린다.
main에 머지되면 GitHub Actions(CD)가 이미지 빌드부터 서버 반영까지 한다. 서버를 처음 만들 때만 1~3단계를 직접 한다. 밖에서는 `https://api.aifilm.life`로 접속한다.

## 구성

| 항목 | 값 |
| --- | --- |
| 리전 | `ap-northeast-2` (서울) |
| ECR | `aifilm-app` (태그 변경 불가, 최근 이미지 10개만 유지) |
| EC2 | `aifilm-server`, `t3.small`, Ubuntu 24.04(x86_64), `ap-northeast-2c`, 20GiB gp3(암호화), CPU 크레딧 사양 Standard, IMDSv2만 허용 |
| EC2 IAM 역할 | `aifilm-ec2-role` (`AmazonEC2ContainerRegistryReadOnly`, `AmazonSSMManagedInstanceCore`) |
| RDS | `aifilm-db`, MySQL 8.4, `db.t4g.micro`, 단일 AZ(`ap-northeast-2c`), 20GiB gp3, 퍼블릭 액세스 없음, 초기 DB `aifilm`, 백업 7일, 삭제 방지 |
| RDS 파라미터 그룹 | `aifilm-mysql84` (`utf8mb4`, `utf8mb4_unicode_ci`, `time_zone=Asia/Seoul`) |
| 보안 그룹 | `aifilm-ec2-sg`: 80, 443 허용, 8080·SSH 없음 / `aifilm-rds-sg`: 3306은 `aifilm-ec2-sg`에서만 |
| 고정 IP | Elastic IP `aifilm-server-eip`, `aifilm-server`에 연결 |
| 도메인 | `api.aifilm.life` (가비아에서 구매, `api` A 레코드 → Elastic IP) |
| HTTPS | 서버의 Nginx가 443을 받아 앱(`127.0.0.1:8080`)으로 넘긴다. 인증서는 Let's Encrypt(certbot, 자동 갱신) |
| 서버 접속 | Session Manager (키 페어 없음) |
| 서버 경로 | `/opt/aifilm` (`compose.yml`, `.env`) |
| 배포 역할 | `aifilm-github-deploy` (GitHub OIDC). 이 저장소의 `production` 환경에서만 받을 수 있고, ECR `aifilm-app` 푸시와 `aifilm-server`의 `AWS-RunShellScript` 실행만 허용 |
| GitHub 환경 | `production` (main 브랜치만 배포). 환경 변수 `AWS_DEPLOY_ROLE_ARN`, `EC2_INSTANCE_ID` |

EC2는 RDS와 같은 가용 영역에 둔다. 다른 영역에 두면 둘 사이 트래픽에 영역 간 전송 요금이 붙는다.

아래 명령의 `<계정ID>`는 AWS 계정 ID(12자리)다.

## 1. 서버 최초 준비 (한 번만)

EC2 콘솔에서 인스턴스를 선택하고 연결 → Session Manager로 접속한다. CloudShell에서 `aws ssm start-session --target <인스턴스 ID>`로 접속해도 된다.

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 amazon-ecr-credential-helper mysql-client

# root의 docker가 로그인 없이 EC2 IAM 역할로 ECR 이미지를 받게 한다
sudo mkdir -p /root/.docker
echo '{"credHelpers": {"<계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com": "ecr-login"}}' | sudo tee /root/.docker/config.json

# 메모리가 2GB라 1GB 스왑을 둔다
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

sudo mkdir -p /opt/aifilm
```

설정 파일 두 개를 서버에 둔다.

```bash
sudo nano /opt/aifilm/compose.yml   # 저장소의 deploy/compose.yml 내용을 붙여넣는다
sudo nano /opt/aifilm/.env          # deploy/.env.example을 보고 실제 값을 채운다
sudo chmod 600 /opt/aifilm/.env
```

## 2. 앱 전용 DB 사용자 만들기 (한 번만)

서버에서 RDS 마스터 계정으로 접속한다. RDS는 EC2 보안 그룹에서만 열려 있어서 로컬 PC에서는 접속되지 않는다.

```bash
mysql -h <RDS 엔드포인트> -u admin -p
```

```sql
CREATE USER 'aifilm'@'%' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON aifilm.* TO 'aifilm'@'%';
```

앱은 이 사용자로 접속하고, 마스터 계정은 관리할 때만 쓴다.

비밀번호는 영문, 숫자와 `-`, `_` 정도로 정한다. 이 값은 SQL 문과 서버 `.env`에 그대로 들어가는데, 따옴표와 `\`는 SQL 문을 깨거나 값을 바꾸고 `$`는 `.env`에서 변수로 읽힌다.

## 3. 도메인과 HTTPS 설정 (한 번만)

보안 그룹 `aifilm-ec2-sg`에 80, 443이 열려 있고 `api.aifilm.life`가 Elastic IP를 가리키는 상태에서 진행한다. (`nslookup api.aifilm.life`로 확인)

```bash
sudo apt-get install -y nginx certbot python3-certbot-nginx

# 인증서만 받는다. 도메인 확인은 nginx 플러그인이 80 포트로 잠시 처리하고, 설정 파일은 고치지 않는다
sudo certbot certonly --nginx -d api.aifilm.life -m <이메일> --agree-tos --no-eff-email

# 저장소의 deploy/nginx/aifilm.conf 내용을 붙여넣고, 기본 사이트는 끈다
sudo nano /etc/nginx/conf.d/aifilm.conf
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx

# 인증서가 갱신되면 Nginx가 새 인증서를 다시 읽도록 reload한다
printf '#!/bin/sh\nsystemctl reload nginx\n' | sudo tee /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh
sudo chmod +x /etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh

# 자동 갱신 확인: certbot 타이머가 있고, 갱신 시험이 성공해야 한다
systemctl list-timers | grep certbot
sudo certbot renew --dry-run
```

Nginx를 적용한 뒤 앱을 띄우고(이미 떠 있으면 다시 만들고) HTTPS로 확인한다. 1단계에서 저장소의 `deploy/compose.yml`을 옮겼으므로 앱 포트는 서버 안(`127.0.0.1:8080`)에만 열린다.

```bash
cd /opt/aifilm && sudo docker compose up -d
sudo docker compose ps             # STATUS가 (healthy)가 될 때까지 확인한다
```

밖에서 `https://api.aifilm.life/actuator/health`가 `UP`인지 확인한다.

8080을 밖에 열어 쓰던 기존 서버에 적용할 때는 위 `docker compose up -d` 전에 `/opt/aifilm/compose.yml`을 저장소의 `deploy/compose.yml`로 바꾸고(`sudo nano /opt/aifilm/compose.yml`), HTTPS 확인이 끝나면 보안 그룹 `aifilm-ec2-sg`에서 8080 규칙을 지운다.

## 4. 자동 배포 (GitHub Actions)

main에 머지되면 아래 순서로 배포된다. 손으로 할 일은 없다.

```
main에 머지
 → CI (.github/workflows/ci.yml)  빌드·테스트 후 aifilm-app.jar를 아티팩트로 올림
 → CD (.github/workflows/cd.yml)
     image   CI의 jar로 이미지를 만들어 ECR에 <커밋 짧은 해시> 태그로 푸시 (이미 있으면 건너뜀)
     deploy  SSM Run Command로 서버에서 deploy/deploy.sh 실행
```

`deploy.sh`는 새 이미지를 먼저 받은 뒤(최대 180초) 서버 `.env`의 `APP_IMAGE`를 새 태그로 바꾸고, `docker compose up -d`(최대 120초) 후 healthy가 될 때까지 최대 150초 기다린다. 실패했을 때 남기는 로그 출력도 30초로 제한해서, 어느 단계가 멈춰도 이전 이미지로 되돌리는 것까지 750초 안에 끝난다.

- 이미지를 받지 못하면(ECR에 없는 태그, 받기가 멈춤 등) `.env`와 실행 중인 컨테이너를 건드리지 않고 실패한다.
- 컨테이너를 띄우지 못하거나 healthy가 되지 않으면 마지막 로그를 남기고 이전 이미지로 되돌린 뒤 실패한다. 이전 이미지로도 healthy가 되지 않으면 그렇게 출력하고 끝나므로 서버를 직접 확인한다.
- 서버에서 잠금(`/run/lock/aifilm-deploy.lock`)을 걸어, 앞선 배포가 아직 돌고 있으면 바로 실패한다.
- 같은 태그를 다시 배포하면 컨테이너를 새로 만들지 않는다.

배포 결과는 Actions의 CD 실행 화면에서 본다. `Deploy to EC2` 스텝에 서버 출력이 그대로 나오고, 같은 내용이 Systems Manager → Run Command 기록에도 남는다.
CD는 한 번에 하나씩 실행된다. 배포 중에 들어온 실행(머지, 롤백)은 대기열에서 기다렸다가 차례로 실행된다. 순서는 머지하거나 Run workflow를 누른 시각이 아니라 실행이 대기를 시작한 시각 기준이다. 대기열은 최대 100개이고, 가득 차면 새로 들어온 실행은 취소된다. 취소된 실행은 Actions의 CD 실행 화면에서 Re-run하고, 롤백이면 Run workflow로 다시 실행한다.
자동 배포는 서버에 반영하기 직전에 그 커밋이 아직 main의 최신 커밋인지 확인하고, 아니면 배포를 건너뛴다. 이때 CD는 성공으로 끝나고 실행 요약에 건너뛴 이유가 남는다. 최신 커밋은 그 커밋의 CI가 성공해 CD가 배포 단계에 도달한 경우에 그 CD가 배포한다. 최신 커밋의 CI가 실패하면 서버는 마지막으로 배포된 커밋에 남으므로, 고친 커밋을 머지하거나 Run workflow로 원하는 태그를 배포한다.
서버 명령은 전달 제한 120초, 실행 제한 900초이고, CD는 두 제한에 여유 60초를 더한 시간까지 기다려 명령이 끝난 것을 확인한 뒤에 끝난다.

밖에서는 `https://api.aifilm.life/actuator/health`와 `https://api.aifilm.life/swagger-ui.html`로 확인한다. `http://`로 들어오면 HTTPS로 넘어가고, 8080은 밖에서 열리지 않는다.

## 5. 롤백

GitHub → Actions → CD → Run workflow에서 브랜치는 `main`, `image_tag`에 되돌릴 태그를 넣고 실행한다. 서버 반영과 실패 처리는 자동 배포와 같다.

- ECR에 올라가 있는 태그만 배포할 수 있다. 태그 목록은 ECR 콘솔의 `aifilm-app` 이미지 목록이나 아래 명령으로 확인한다.
- 태그는 커밋 짧은 해시 형식(소문자 16진수 7~40자리)만 받는다.
- 수동 배포는 main 최신 커밋 검사를 하지 않으므로 이전 태그도 그대로 배포된다.

```bash
aws ecr describe-images --repository-name aifilm-app --query "imageDetails[].imageTags" --output text
```

## 6. 비상: 손으로 배포하기

Actions를 쓸 수 없을 때만 쓴다. Session Manager로 서버에 접속해서 반영한다.

```bash
cd /opt/aifilm
sudo nano .env                     # APP_IMAGE의 태그를 바꾼다
sudo docker compose pull
sudo docker compose up -d
sudo docker compose ps             # STATUS가 (healthy)가 될 때까지 확인한다
sudo docker compose logs app       # The following 1 profile is active: "prod" 와 RDS 접속 로그 확인
curl -fsS http://localhost:8080/actuator/health
```

배포할 이미지가 ECR에 없을 때만 로컬 PC에서 빌드해서 올린다. (PowerShell)

```powershell
git switch main
git pull
./gradlew :app:bootJar

$registry = "<계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com"
$tag = git rev-parse --short HEAD
cmd /c "aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin $registry"
docker build -t "$registry/aifilm-app:$tag" .
docker push "$registry/aifilm-app:$tag"
```

- 로컬 PC에서 푸시하려면 ECR 푸시 권한이 있는 IAM 사용자의 액세스 키가 필요하다. 평소에는 두지 않고, 필요할 때 푸시 권한만 있는 키를 만들어 쓰고 끝나면 삭제한다.
- ECR 로그인은 `cmd /c`로 실행한다. Windows PowerShell(5.1)에서 `aws ... | docker login`을 파이프로 바로 이으면 토큰이 그대로 전달되지 않아 `400 Bad Request`로 실패한다.
- `aws`를 찾지 못한다는 에러가 나면 AWS CLI를 설치하기 전에 연 터미널이다. 터미널(IntelliJ 포함)을 새로 연다.
- 저장소가 태그 변경 불가라 같은 태그로 다시 푸시하면 실패한다. 이미 올라간 커밋이면 그 이미지를 그대로 쓴다.

## 운영 메모

- 서버의 `compose.yml`은 직접 고치지 않는다. 저장소의 `deploy/compose.yml`을 고친 뒤 옮긴다. CD는 이 파일을 옮기지 않는다.
- 서버 `.env`에는 DB 비밀번호가 들어 있으므로 권한을 600으로 두고, 저장소나 채팅에 남기지 않는다.
- 고정 IP는 Elastic IP다. 인스턴스를 중지하거나 연결을 끊은 상태에서도 요금이 나오므로, 서버를 없앨 때는 Elastic IP도 해제한다.
- Nginx 설정은 저장소의 `deploy/nginx/aifilm.conf`가 기준이다. 서버의 `/etc/nginx/conf.d/aifilm.conf`를 직접 고치지 않고, 저장소에서 고친 뒤 옮겨서 `sudo nginx -t && sudo systemctl reload nginx`로 반영한다.
- 브라우저에서 API를 부를 프론트 출처는 서버 `.env`의 `AIFILM_CORS_ALLOWED_ORIGINS`에 쉼표로 넣고 `cd /opt/aifilm && sudo docker compose up -d`로 컨테이너를 다시 만든다.
- 도메인 `aifilm.life`는 자동 연장을 꺼 두었다. 만료일은 가비아에서 확인한다.
- EC2를 새로 만들면 배포 역할 정책의 인스턴스 ARN과 GitHub 환경 변수 `EC2_INSTANCE_ID`를 새 인스턴스 ID로 바꾼다.
- 배포 역할의 신뢰 조건 `sub`는 GitHub가 발급하는 OIDC 토큰 값과 정확히 같아야 한다. 이 저장소에는 `repo:AiFilm-Work@<소유자 ID>/AiFilm-Backend@<저장소 ID>:environment:production`처럼 ID가 붙은 형식으로 발급되고, ID는 `gh api repos/AiFilm-Work/AiFilm-Backend --jq '.owner.id, .id'`로 확인한다. 역할을 다시 만들거나 저장소를 옮긴 뒤 CD가 `Not authorized to perform sts:AssumeRoleWithWebIdentity`로 실패하면, CloudTrail 이벤트 기록에서 `AssumeRoleWithWebIdentity`를 찾아 실제 `sub`(사용자 이름)를 확인하고 신뢰 정책을 맞춘다.
- 크레딧 사용량은 결제 및 비용 관리의 크레딧 화면과 예산 알림(월 $30)으로 확인한다.
- RDS는 콘솔에서 일시 중지할 수 있지만 7일이 지나면 자동으로 다시 시작된다.
- RDS 마스터 암호를 잊으면 RDS 콘솔의 수정에서 새 마스터 암호를 넣고 즉시 적용한다. 1~2분이면 바뀌고 데이터는 그대로다.
