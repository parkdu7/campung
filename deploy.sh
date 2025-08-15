#!/bin/bash

# 설정 변수
DOMAIN="campung.my"
EMAIL="cbkjh0225@gmail.com"
GITHUB_URL="https://github.com/Jaeboong/Campung_Backend.git"
BRANCH="server"
APP_DIR="/home/kjh/Campung_Backend"
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
    docker.io \
    docker-compose \
    certbot \
    python3-certbot-nginx \
    ufw \
    openjdk-17-jdk \
    git \
    curl \
    htop

# 3. Docker 설정
log_step "Docker 설정 중..."
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# 4. 방화벽 설정
log_step "방화벽 설정 중..."
sudo ufw allow 22      # SSH
sudo ufw allow 80      # HTTP
sudo ufw allow 443     # HTTPS
sudo ufw allow 8080    # Spring Boot (임시)
sudo ufw --force enable

# 5. 기존 애플리케이션 정지
log_step "기존 애플리케이션 정지 중..."
sudo pkill -f "java.*jar" || true
sudo docker-compose down || true

# 6. 프로젝트 클론 또는 업데이트
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

# 7. Gradle 빌드
log_step "애플리케이션 빌드 중..."
chmod +x gradlew
./gradlew clean build -x test

# 8. Docker Compose 실행 (DB, Redis)
log_step "데이터베이스 및 Redis 시작 중..."
docker-compose up -d

# 9. Nginx 설정
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

# 10. Spring Boot 애플리케이션 실행
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

# 11. 애플리케이션 시작 대기
log_step "애플리케이션 시작 대기 중..."
sleep 30

# 12. SSL 인증서 발급
log_step "SSL 인증서 발급 중..."
sudo certbot --nginx -d $DOMAIN --email $EMAIL --agree-tos --non-interactive --redirect

# 13. 서비스 상태 확인
log_step "서비스 상태 확인 중..."

# Docker 서비스 확인
if docker ps | grep -q "campung-redis\|Campung"; then
    log_info "✅ Docker 컨테이너 정상 실행"
else
    log_warn "⚠️  Docker 컨테이너 상태 확인 필요"
fi

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

# 14. 배포 완료
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
log_info "  - Docker 로그: docker-compose logs -f"
echo "================================"

# 15. 자동 갱신 설정 (SSL)
log_step "SSL 인증서 자동 갱신 설정 중..."
(crontab -l 2>/dev/null; echo "0 12 * * * /usr/bin/certbot renew --quiet && systemctl reload nginx") | crontab -

log_info "배포 스크립트 실행 완료!"
