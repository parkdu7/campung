#!/bin/bash

# 설정 변수
DOMAIN="campung.my"
EMAIL="cbkjh0225@gmail.com"
GITHUB_URL="https://github.com/Jaeboong/Campung_Backend.git"
BRANCH="server"
APP_DIR="/home/kjh/Project/Campung_Backend"
DB_DIR="/home/kjh/Project/DB_Setting/campung"
SERVICE_NAME="campung-backend"

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 실행 확인
log_step "Campung Backend 배포 시작"
log_info "도메인: $DOMAIN"
log_info "브랜치: $BRANCH"
read -p "계속 진행하시겠습니까? (y/N): " confirm
if [[ $confirm != [yY] ]]; then
    log_error "배포가 취소되었습니다."
fi

# 1. 시스템 업데이트
log_step "시스템 업데이트 중..."
sudo apt update && sudo apt upgrade -y

# 2. 필요한 패키지 설치
log_step "필요한 패키지 설치 중..."
sudo apt install -y \
    nginx \
    certbot \
    python3-certbot-nginx \
    ufw \
    openjdk-17-jdk \
    git \
    curl \
    htop

# Docker 관련은 별도로 설치 (이미 설치되어 있을 수 있음)
if ! command -v docker &> /dev/null; then
    log_info "Docker 설치 중..."
    sudo apt install -y docker.io docker-compose
else
    log_info "Docker는 이미 설치되어 있습니다."
fi

# 3. Java 환경 설정
log_step "Java 환경 설정 중..."
# JAVA_HOME 설정
JAVA_HOME_PATH=$(find /usr/lib/jvm -name "java-17-openjdk*" -type d | head -1)
if [ -n "$JAVA_HOME_PATH" ]; then
    export JAVA_HOME=$JAVA_HOME_PATH
    export PATH=$JAVA_HOME/bin:$PATH
    echo "export JAVA_HOME=$JAVA_HOME_PATH" >> ~/.bashrc
    echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> ~/.bashrc
    log_info "JAVA_HOME 설정 완료: $JAVA_HOME_PATH"
else
    log_error "Java 17이 제대로 설치되지 않았습니다."
fi

# 4. Docker 설정
log_step "Docker 설정 중..."
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# 5. 방화벽 설정
log_step "방화벽 설정 중..."
sudo ufw allow 22      # SSH
sudo ufw allow 80      # HTTP
sudo ufw allow 443     # HTTPS
sudo ufw allow 8080    # Spring Boot (임시)
sudo ufw --force enable

# 6. 기존 애플리케이션 정지
log_step "기존 애플리케이션 정지 중..."
sudo pkill -f "java.*jar" || true
sudo systemctl stop $SERVICE_NAME || true

# 7. 프로젝트 클론 또는 업데이트
log_step "소스코드 업데이트 중..."
if [ -d "$APP_DIR" ]; then
    cd $APP_DIR
    git fetch origin
    git checkout $BRANCH
    git pull origin $BRANCH
else
    git clone -b $BRANCH $GITHUB_URL $APP_DIR
    cd $APP_DIR
fi

# 8. Gradle 빌드
log_step "애플리케이션 빌드 중..."
chmod +x gradlew
./gradlew clean build -x test

# 9. Docker 컨테이너 상태 확인
log_step "데이터베이스 및 Redis 상태 확인 중..."

# MariaDB 컨테이너 확인 (정확한 패턴으로 수정)
if docker ps --format "table {{.Names}}" | grep -q "^Campung$"; then
    log_info "✅ MariaDB 컨테이너 실행 중"
else
    log_warn "⚠️  MariaDB 컨테이너가 실행되지 않았습니다. 수동으로 시작해주세요:"
    log_warn "   cd $DB_DIR && docker-compose up -d"
fi

# Redis 컨테이너 확인 (정확한 패턴으로 수정)
if docker ps --format "table {{.Names}}" | grep -q "^campung-redis$"; then
    log_info "✅ Redis 컨테이너 실행 중"
else
    log_warn "⚠️  Redis 컨테이너가 실행되지 않았습니다. 수동으로 시작해주세요:"
    log_warn "   cd $DB_DIR && docker-compose up -d"
fi

# 10. Nginx 설정
log_step "Nginx 설정 중..."
sudo tee /etc/nginx/sites-available/$DOMAIN > /dev/null <<EOF
server {
    listen 80;
    server_name $DOMAIN;

    # 보안 헤더
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 로그 설정
    access_log /var/log/nginx/${DOMAIN}_access.log;
    error_log /var/log/nginx/${DOMAIN}_error.log;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        
        # 타임아웃 설정
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 헬스체크 엔드포인트 (로그 제외)
    location = /health {
        proxy_pass http://localhost:8080/;
        access_log off;
    }
}
EOF

# Nginx 사이트 활성화
sudo ln -sf /etc/nginx/sites-available/$DOMAIN /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default

# Nginx 테스트 및 재시작
sudo nginx -t || log_error "Nginx 설정 오류"
sudo systemctl restart nginx

# 11. Spring Boot 애플리케이션 실행
log_step "Spring Boot 애플리케이션 시작 중..."
# systemd 서비스 파일 생성
sudo tee /etc/systemd/system/$SERVICE_NAME.service > /dev/null <<EOF
[Unit]
Description=Campung Backend Application
After=network.target

[Service]
Type=simple
User=kjh
WorkingDirectory=$APP_DIR
ExecStart=/usr/bin/java -jar $APP_DIR/build/libs/Campung-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=10

Environment=SPRING_PROFILES_ACTIVE=prod
Environment=SERVER_PORT=8080

StandardOutput=journal
StandardError=journal
SyslogIdentifier=$SERVICE_NAME

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable $SERVICE_NAME
sudo systemctl start $SERVICE_NAME

# 12. 애플리케이션 시작 대기
log_step "애플리케이션 시작 대기 중..."
sleep 30

# 13. SSL 인증서 발급
log_step "SSL 인증서 발급 중..."

# DNS 설정 확인
log_info "DNS 설정 확인 중..."
DOMAIN_IP=$(dig +short $DOMAIN || echo "조회실패")
if [ "$DOMAIN_IP" = "119.56.208.5" ]; then
    log_info "✅ DNS 설정 정상: $DOMAIN → $DOMAIN_IP"
    sudo certbot --nginx -d $DOMAIN --email $EMAIL --agree-tos --non-interactive --redirect
else
    log_warn "⚠️  DNS 설정 문제 발견!"
    log_warn "   현재 $DOMAIN → $DOMAIN_IP"
    log_warn "   필요한 설정: $DOMAIN → 119.56.208.5"
    log_warn "   도메인 관리 페이지에서 A 레코드를 119.56.208.5로 설정한 후 재시도하세요."
    log_warn "   DNS 전파까지 최대 24시간 소요될 수 있습니다."
    log_warn "   SSL 인증서는 DNS 설정 완료 후 수동으로 발급하세요:"
    log_warn "   sudo certbot --nginx -d $DOMAIN --email $EMAIL --agree-tos --non-interactive --redirect"
fi

# 14. 서비스 상태 확인
log_step "서비스 상태 확인 중..."

# Docker 서비스 확인
MARIADB_STATUS=$(docker ps --filter "name=Campung" --format "table {{.Names}}\t{{.Status}}" | grep -v NAMES || echo "없음")
REDIS_STATUS=$(docker ps --filter "name=campung-redis" --format "table {{.Names}}\t{{.Status}}" | grep -v NAMES || echo "없음")
PHPMYADMIN_STATUS=$(docker ps --filter "name=Campung-phpmyadmin" --format "table {{.Names}}\t{{.Status}}" | grep -v NAMES || echo "없음")

log_info "Docker 컨테이너 상태:"
log_info "  - MariaDB: $MARIADB_STATUS"
log_info "  - Redis: $REDIS_STATUS"  
log_info "  - phpMyAdmin: $PHPMYADMIN_STATUS"

# Spring Boot 확인
if systemctl is-active --quiet $SERVICE_NAME; then
    log_info "✅ Spring Boot 서비스 정상 실행"
else
    log_warn "⚠️  Spring Boot 서비스 상태 확인 필요"
    sudo systemctl status $SERVICE_NAME
fi

# HTTP 응답 확인
sleep 10
if curl -f http://localhost:8080/ > /dev/null 2>&1; then
    log_info "✅ 애플리케이션 HTTP 응답 정상"
else
    log_warn "⚠️  애플리케이션 HTTP 응답 확인 필요"
fi

# HTTPS 응답 확인
if curl -f https://$DOMAIN > /dev/null 2>&1; then
    log_info "✅ HTTPS 접속 정상"
else
    log_warn "⚠️  HTTPS 접속 확인 필요"
fi

# 15. 배포 완료
log_step "🎉 배포 완료!"
echo "================================"
log_info "접속 주소: https://$DOMAIN"
log_info "관리 도구:"
log_info "  - phpMyAdmin: http://$DOMAIN:9012"
echo "================================"
log_info "테스트 URL:"
log_info "  - https://$DOMAIN/"
log_info "  - https://$DOMAIN/test-db"
log_info "  - https://$DOMAIN/test-redis"
log_info "  - https://$DOMAIN/test-all"
echo "================================"
log_info "서비스 관리 명령어:"
log_info "  - 서비스 상태: sudo systemctl status $SERVICE_NAME"
log_info "  - 서비스 재시작: sudo systemctl restart $SERVICE_NAME"
log_info "  - 로그 확인: sudo journalctl -u $SERVICE_NAME -f"
log_info "  - DB 컨테이너 관리: cd $DB_DIR && docker-compose logs -f"
echo "================================"

# 16. 자동 갱신 설정 (SSL)
log_step "SSL 인증서 자동 갱신 설정 중..."
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && systemctl reload nginx") | crontab -

log_info "배포 스크립트 실행 완료!"
