# Ubuntu 24.04 기준

---

### 1. CloudFlare 설치
- `sudo apt update`
- `sudo apt install cloudflared`
- `cloudflared --version`

### 2. CloudFlare Tunnel 생성 및 설정
- `cloudflared tunnel login`
  - 터미널에서 웹 경로가 나오면 브라우저에서 붙여넣어 로그인
- `cloudflared tunnel create [터널명]`
- `mkdir -p ~/.cloudflared`
- `vi ~/.cloudflared/config.yml`
  - ```bash
    tunnel: [생성한 터널명]
    credentials-file: /home/[유저명]/.cloudflared/<Tunnel-UUID>.json

    ingress:
      - hostname: [호스트명]
        service: http://localhost:8080
      - service: http_status:404
- `cloudflared tunnel route dns [터널명] [호스트명]`
  - CloudFlare DNS 레코드 생성
  - CloudFlare DNS에 CNAME 레코드가 자동으로 생성

### 3. CloudFlare Tunnel 실행
- 실행
  - `cloudflared tunnel run my-tunnel`
- 서비스로 등록
  - ```bash
    sudo cloudflared service install
    sudo systemctl enable cloudflared
    sudo systemctl start cloudflared
- 브라우저에서 확인