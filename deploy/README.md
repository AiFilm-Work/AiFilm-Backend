# 배포 절차

배포 서버(EC2) 한 대에서 앱 컨테이너를 띄우고, DB는 RDS(MySQL 8.4)를 쓴다. 이미지는 ECR에 커밋 해시 태그로 올린다.
GitHub Actions로 자동화하기 전까지는 이 문서 순서대로 직접 배포한다.

## 구성

| 항목 | 값 |
| --- | --- |
| 리전 | `ap-northeast-2` (서울) |
| ECR | `aifilm-app` (태그 변경 불가, 최근 이미지 10개만 유지) |
| EC2 | `aifilm-server`, `t3.small`, Ubuntu 24.04(x86_64), `ap-northeast-2c`, 20GiB gp3(암호화), CPU 크레딧 사양 Standard, IMDSv2만 허용 |
| EC2 IAM 역할 | `aifilm-ec2-role` (`AmazonEC2ContainerRegistryReadOnly`, `AmazonSSMManagedInstanceCore`) |
| RDS | `aifilm-db`, MySQL 8.4, `db.t4g.micro`, 단일 AZ(`ap-northeast-2c`), 20GiB gp3, 퍼블릭 액세스 없음, 초기 DB `aifilm`, 백업 7일, 삭제 방지 |
| RDS 파라미터 그룹 | `aifilm-mysql84` (`utf8mb4`, `utf8mb4_unicode_ci`, `time_zone=Asia/Seoul`) |
| 보안 그룹 | `aifilm-ec2-sg`: 8080 허용(임시), SSH 없음 / `aifilm-rds-sg`: 3306은 `aifilm-ec2-sg`에서만 |
| 서버 접속 | Session Manager (키 페어 없음) |
| 서버 경로 | `/opt/aifilm` (`compose.yml`, `.env`) |

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

## 3. 이미지 빌드·푸시 (로컬 PC, PowerShell)

main에 머지된 커밋으로 만든다. 이미지 태그는 그 커밋의 짧은 해시다.

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

- 푸시는 ECR 푸시 권한만 가진 IAM 사용자(`aifilm-ecr-pusher`)의 키로 한다.
- ECR 로그인은 `cmd /c`로 실행한다. Windows PowerShell(5.1)에서 `aws ... | docker login`을 파이프로 바로 이으면 토큰이 그대로 전달되지 않아 `400 Bad Request`로 실패한다.
- `aws`를 찾지 못한다는 에러가 나면 AWS CLI를 설치하기 전에 연 터미널이다. 터미널(IntelliJ 포함)을 새로 연다.
- 저장소가 태그 변경 불가라 같은 태그로 다시 푸시하면 실패한다. 이미 올라간 커밋이면 그 이미지를 그대로 쓴다.

## 4. 서버에 반영

```bash
cd /opt/aifilm
sudo nano .env                     # APP_IMAGE의 태그를 새 태그로 바꾼다
sudo docker compose pull
sudo docker compose up -d
sudo docker compose ps             # STATUS가 (healthy)가 될 때까지 확인한다
sudo docker compose logs app       # The following 1 profile is active: "prod" 와 RDS 접속 로그 확인
curl -fsS http://localhost:8080/actuator/health
```

밖에서는 `http://<EC2 퍼블릭 IP>:8080/actuator/health`와 `http://<EC2 퍼블릭 IP>:8080/swagger-ui.html`로 확인한다.

## 5. 롤백

```bash
cd /opt/aifilm
sudo nano .env                     # APP_IMAGE를 이전 태그로 바꾼다
sudo docker compose up -d
```

올라가 있는 태그는 ECR 콘솔의 `aifilm-app` 이미지 목록이나 아래 명령으로 확인한다.

```powershell
aws ecr describe-images --repository-name aifilm-app --query "imageDetails[].imageTags" --output text
```

## 운영 메모

- 서버의 `compose.yml`은 직접 고치지 않는다. 저장소의 `deploy/compose.yml`을 고친 뒤 옮긴다.
- 서버 `.env`에는 DB 비밀번호가 들어 있으므로 권한을 600으로 두고, 저장소나 채팅에 남기지 않는다.
- EC2를 중지했다 켜면 퍼블릭 IP가 바뀐다. 고정 IP는 HTTPS 작업에서 정한다.
- 크레딧 사용량은 결제 및 비용 관리의 크레딧 화면과 예산 알림(월 $30)으로 확인한다.
- RDS는 콘솔에서 일시 중지할 수 있지만 7일이 지나면 자동으로 다시 시작된다.
- RDS 마스터 암호를 잊으면 RDS 콘솔의 수정에서 새 마스터 암호를 넣고 즉시 적용한다. 1~2분이면 바뀌고 데이터는 그대로다.
- 로컬 PC의 푸시용 액세스 키는 GitHub Actions 배포로 바꾸면 삭제한다.
