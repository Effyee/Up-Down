# 1. 베이스 이미지 선택: 가벼운 Python 3.12 버전을 사용합니다.
FROM python:3.12-slim

# 2. 작업 디렉토리 설정: 컨테이너 내부의 /app 폴더에서 모든 작업을 수행합니다.
WORKDIR /app

# 3. 필요한 라이브러리 설치:
# 먼저 requirements.txt 파일만 복사하여 라이브러리를 설치합니다.
# 이렇게 하면 스크립트만 변경될 경우, 이 단계는 캐시를 사용하여 빌드 속도가 빨라집니다.
COPY ./scripts/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 4. 소스코드 복사:
# 로컬의 scripts 폴더 전체를 컨테이너의 /app/scripts 폴더로 복사합니다.
COPY ./scripts/ ./scripts/

# 5. 컨테이너 시작 시 실행할 명령어:
# "python scripts/realtime_producer.py" 명령어를 실행합니다.
CMD ["python", "scripts/realtime_producer.py"]
