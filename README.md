# IWAS — Intelligent Workforce Arrangement System

Hệ thống backend xây dựng bằng **Spring Boot 3.5** (Java 21), cung cấp các chức năng xác thực người dùng, quản lý dự án/công việc, sắp xếp công việc tự động (ATC), tìm kiếm với Elasticsearch, lưu trữ file với S3/MinIO, gửi email qua RabbitMQ và thông báo realtime qua WebSocket.

## Mục lục

1. [Kiến trúc & thành phần](#1-kiến-trúc--thành-phần)
2. [Công nghệ, thư viện và phiên bản](#2-công-nghệ-thư-viện-và-phiên-bản)
3. [Yêu cầu môi trường](#3-yêu-cầu-môi-trường)
4. [Cài đặt và chạy hệ thống](#4-cài-đặt-và-chạy-hệ-thống)
5. [Cấu hình cơ sở dữ liệu](#5-cấu-hình-cơ-sở-dữ-liệu)
6. [Biến môi trường](#6-biến-môi-trường)
7. [Tài khoản thử nghiệm](#7-tài-khoản-thử-nghiệm)
8. [Truy cập API & công cụ](#8-truy-cập-api--công-cụ)
9. [Kiểm thử](#9-kiểm-thử)
10. [Cấu trúc mã nguồn](#10-cấu-trúc-mã-nguồn)
11. [Xử lý sự cố](#11-xử-lý-sự-cố)

---

## 1. Kiến trúc & thành phần

| Thành phần | Vai trò | Cổng mặc định |
|------------|---------|---------------|
| Ứng dụng IWAS (Spring Boot) | REST API + WebSocket | `9090` |
| PostgreSQL | Cơ sở dữ liệu chính | `5432` |
| RabbitMQ | Hàng đợi gửi email & đồng bộ search | `5672` (AMQP), `15672` (UI) |
| Redis | Cache | `6379` |
| Elasticsearch | Tìm kiếm & autocomplete | `9200` |
| Kibana | Giao diện xem log/ES | `5601` |
| Logstash | Thu thập log | `5000`, `5044` |
| MinIO | Lưu trữ file (S3-compatible) cho môi trường dev | `9000` (API), `9001` (Console) |

---

## 2. Công nghệ, thư viện và phiên bản

### Nền tảng

| Công nghệ | Phiên bản |
|-----------|-----------|
| Java (JDK) | 21 |
| Spring Boot | 3.5.11 |
| Maven | dùng Maven Wrapper `mvnw` kèm theo repo |
| Build artifact | `iwas.jar` |

### Thư viện Java (khai báo trong `pom.xml`)

| Thư viện | Phiên bản | Ghi chú |
|----------|-----------|---------|
| spring-boot-starter-web | 3.5.11 (BOM) | REST API |
| spring-boot-starter-data-jpa | 3.5.11 (BOM) | ORM/Hibernate |
| spring-boot-starter-security | 3.5.11 (BOM) | Bảo mật, xác thực |
| spring-boot-starter-validation | 3.5.11 (BOM) | Validate dữ liệu |
| spring-boot-starter-amqp | 3.5.11 (BOM) | RabbitMQ |
| spring-boot-starter-mail | 3.5.11 (BOM) | Gửi email |
| spring-boot-starter-thymeleaf | 3.5.11 (BOM) | Template email |
| spring-boot-starter-cache | 3.5.11 (BOM) | Cache |
| spring-boot-starter-data-redis | 3.5.11 (BOM) | Redis (Lettuce) |
| spring-boot-starter-data-elasticsearch | 3.5.11 (BOM) | Elasticsearch |
| spring-boot-starter-websocket | 3.5.11 (BOM) | Realtime notification |
| jjwt (api/impl/jackson) | 0.12.6 | JWT |
| springdoc-openapi-starter-webmvc-ui | 2.8.13 | Swagger UI / OpenAPI |
| software.amazon.awssdk:s3 | 2.26.31 | Lưu trữ S3/MinIO |
| logstash-logback-encoder | 8.0 | Log dạng JSON gửi Logstash |
| org.json:json | 20240303 | Xử lý JSON |
| postgresql (driver) | 3.5.11 (BOM) | JDBC PostgreSQL |
| lombok | 3.5.11 (BOM) | Giảm boilerplate |
| commons-codec | 3.5.11 (BOM) | Mã hóa/encode |
| h2 | 3.5.11 (BOM) | DB in-memory cho test |
| spring-security-test | 3.5.11 (BOM) | Test bảo mật |

> "(BOM)" nghĩa là phiên bản do Spring Boot 3.5.11 quản lý tự động, không khai báo cứng trong `pom.xml`.

### Dịch vụ hạ tầng (khai báo trong `docker-compose.yml`)

| Dịch vụ | Image / phiên bản |
|---------|-------------------|
| RabbitMQ | `rabbitmq:3-management` |
| Redis | `redis:7` |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.13.4` |
| Kibana | `docker.elastic.co/kibana/kibana:8.13.4` |
| Logstash | `docker.elastic.co/logstash/logstash:8.13.4` |
| MinIO | `minio/minio` (latest) |
| PostgreSQL | cài riêng — khuyến nghị `postgres:16` |

---

## 3. Yêu cầu môi trường

Cần cài đặt trước:

- **JDK 21**
- **Docker** và **Docker Compose** — chạy RabbitMQ, Redis, Elasticsearch, Kibana, Logstash, MinIO
- **PostgreSQL** — cài tại local.
- **Maven** — không bắt buộc, đã có sẵn Maven Wrapper `mvnw` trong repo.

Kiểm tra Java:

```bash
java -version
```

---

## 4. Cài đặt và chạy hệ thống

### Bước 1 — Khởi động các dịch vụ hạ tầng

```bash
docker compose up -d      # RabbitMQ, Redis, Elasticsearch, Kibana, Logstash, MinIO
docker compose ps         # kiểm tra trạng thái
```

### Bước 2 — Build và chạy ứng dụng

Chạy ở chế độ phát triển:

```bash
# Linux/macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

Hoặc build ra file JAR rồi chạy:

```bash
./mvnw clean package
java -jar target/iwas.jar
```

Ứng dụng chạy tại **http://localhost:9090**.

> **Bucket MinIO tự động tạo**: khi khởi động, ứng dụng tự kiểm tra và tạo bucket `iwas` nếu chưa có (`S3StorageService.initBucket()` qua `@PostConstruct`). Không cần tạo bucket thủ công. Nếu vẫn muốn tạo tay: MinIO Console http://localhost:9001 → **Create Bucket** → `iwas`.

---

## 5. Cấu hình cơ sở dữ liệu

Cấu hình mặc định trong `src/main/resources/application.yml`:

| Thông số | Giá trị mặc định |
|----------|------------------|
| URL | `jdbc:postgresql://localhost:5432/workforce` |
| Database | `workforce` |
| Username | `postgres` |
| Password | `password` |
| Driver | `org.postgresql.Driver` |
| DDL mode | `spring.jpa.hibernate.ddl-auto = update` |

### Khởi tạo schema

- **Không cần chạy script tạo bảng thủ công.** Hibernate với `ddl-auto: update` tự sinh toàn bộ bảng từ các entity ngay khi ứng dụng khởi động. Chỉ cần tạo sẵn **database rỗng** tên `workforce`.

Tạo database rỗng:

```sql
CREATE DATABASE workforce;
```

---

## 6. Biến môi trường

Ở môi trường dev, mọi cấu hình đã có sẵn trong `application.yml` nên **không bắt buộc đặt biến môi trường**. Khi triển khai thực tế, nên override bằng biến môi trường (Spring Boot hỗ trợ relaxed binding).

### Biến dùng cho profile `prod` (`application-prod.yml`)

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `SPRING_PROFILES_ACTIVE` | `default` | Đặt `prod` để dùng AWS S3 thật thay cho MinIO |
| `AWS_REGION` | `ap-southeast-1` | Vùng AWS cho S3 |
| `S3_BUCKET` | `iwas-files` | Tên bucket S3 |

Ở profile `prod`, credential S3 lấy từ **IAM role** gắn vào EC2 (không cần access key), và IAM role cần quyền `s3:CreateBucket` nếu muốn app tự tạo bucket.

### Các giá trị nên override khi triển khai (ví dụ qua biến môi trường)

| Cấu hình | Biến môi trường tương ứng |
|----------|---------------------------|
| Kết nối DB | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| SMTP gửi mail | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` |
| Khóa ký JWT | `APP_JWT_SECRET` |
| RabbitMQ | `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` |
| Redis | `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` |
| Elasticsearch | `SPRING_ELASTICSEARCH_URIS` |
| Storage (S3/MinIO) | `APP_STORAGE_ENDPOINT`, `APP_STORAGE_ACCESS_KEY`, `APP_STORAGE_SECRET_KEY`, `APP_STORAGE_BUCKET` |
| Frontend/WebSocket | `APP_FRONTEND_BASE_URL`, `APP_WS_ALLOWED_ORIGINS` |

Chạy với profile prod:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/iwas.jar
```

> ⚠️ File `application.yml` đang chứa secret mẫu (JWT secret, mật khẩu email). Trước khi đưa lên môi trường thật, hãy thay bằng biến môi trường/secret riêng và không commit giá trị nhạy cảm.

---

## 7. Tài khoản thử nghiệm

Khi khởi động lần đầu, hệ thống **tự tạo tài khoản admin mặc định** (`DataInitializer`) nếu chưa tồn tại:

| Thông tin | Giá trị |
|-----------|---------|
| Email | `admin@workforce.com` |
| Mật khẩu | `Admin@123` |
| Vai trò | `ADMIN` |

Dùng tài khoản này để đăng nhập qua API auth và lấy JWT. Các tài khoản khác có thể tạo qua luồng đăng ký/quản lý người dùng trong ứng dụng.

> Nên đổi mật khẩu admin mặc định ngay khi triển khai môi trường thật.

---

## 8. Truy cập API & công cụ

| Công cụ | Địa chỉ | Ghi chú |
|---------|---------|---------|
| Swagger UI | http://localhost:9090/swagger-ui.html | Tài liệu & thử API |
| OpenAPI docs | http://localhost:9090/v3/api-docs | Đặc tả OpenAPI (JSON) |
| WebSocket | `ws://localhost:9090/ws` | Realtime notification |
| RabbitMQ UI | http://localhost:15672 | user `admin` / pass `admin` |
| Kibana | http://localhost:5601 | Xem log & dữ liệu ES |
| MinIO Console | http://localhost:9001 | user `minioadmin` / pass `minioadmin` |

---

## 9. Kiểm thử

```bash
./mvnw test
```

Test sử dụng H2 in-memory database nên **không cần** các dịch vụ Docker đang chạy.

---

## 10. Cấu trúc mã nguồn

```
IWAS/
├── src/main/java/com/iwas/
│   ├── IWASApplication.java        # Entry point
│   ├── arrangement/                # Sắp xếp công việc tự động (ATC)
│   ├── auth/                       # Xác thực, JWT, OTP, refresh token
│   ├── user/  project/  task/      # Nghiệp vụ chính
│   ├── skill/  search/             # Kỹ năng & tìm kiếm (Elasticsearch)
│   ├── common/                     # DTO, exception, storage, messaging dùng chung
│   └── config/                     # Cấu hình (Storage, DataInitializer, ...)
├── src/main/resources/
│   ├── application.yml             # Cấu hình dev
│   ├── application-prod.yml        # Cấu hình prod (AWS S3)
│   ├── db/migration/               # SQL tài liệu/dọn dẹp thủ công (Flyway không chạy tự động)
│   ├── elasticsearch/              # Cấu hình index ES
│   └── templates/                  # Template email (Thymeleaf)
├── logstash/pipeline/              # Pipeline Logstash
├── docker-compose.yml              # Hạ tầng dev
└── pom.xml                         # Khai báo dependency & build
```

---

## 11. Xử lý sự cố

- **Ứng dụng không kết nối được DB**: kiểm tra PostgreSQL đã chạy và database `workforce` đã tồn tại.
- **Lỗi upload/tải file**: kiểm tra MinIO đang chạy và app đã tạo bucket `iwas` khi khởi động (xem log `[Storage] Bucket 'iwas' is ready` hoặc `Created bucket 'iwas'`).
- **Elasticsearch không sẵn sàng**: ES cần vài chục giây để khởi động; theo dõi bằng `docker compose logs -f elasticsearch`.
- **Không đăng nhập được bằng admin**: kiểm tra log khởi động có dòng tạo tài khoản admin mặc định; nếu DB đã có user cùng email thì bước seed sẽ bị bỏ qua.
- **Cổng bị trùng**: đảm bảo các cổng `9090`, `5432`, `5672`, `6379`, `9200`, `9000`, `9001` chưa bị dịch vụ khác chiếm.
