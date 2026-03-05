## Agile SaaS (Jira-like) — MVP Document

Tài liệu này mô tả **MVP Jira-core** cho Roamtrip theo hướng **multi-tenant shared database**, tenant key là `Organization.id` (quy ước `orgId`).

---

## Glossary

- **Tenant / Organization (Org)**: Đơn vị tenant. Mọi dữ liệu nghiệp vụ được scope theo `orgId`.
- **Org role**: Quyền ở cấp organization: `OWNER | ADMIN | MEMBER` (xem `organization_members.role`).
- **Project**: Không gian làm việc trong Org; `Project.key` unique theo Org.
- **Project role**: Quyền ở cấp project: `OWNER | ADMIN | MEMBER` (xem `project_members.role`).
- **Workflow**: Tập trạng thái + transition điều khiển luồng Issue của một Project.
- **Issue**: Ticket (Story/Task/Bug/Epic) trong Project, có thể thuộc Sprint.
- **Sprint**: Chu kỳ làm việc (Planned/Active/Closed).
- **Notification**: Thông báo in-app (MVP), có thể mở rộng email/push.
- **AuditLog**: Log hành động theo org để truy vết.
- **Tenant-safety**: Không thể truy cập/chỉnh sửa dữ liệu khác org, kể cả khi đoán ID.

---

## Assumptions (MVP)

- **MVP scope**: Jira-like core: Org/Project/Workflow/Sprint/Issue/Comment + basic Notifications + Audit support.
- **Tenant model**: **Shared DB**, tenant key = `organizations.id` (alias `orgId`).
- **API scoping by design**: Mọi endpoint nghiệp vụ (trừ auth) đi theo pattern: `/orgs/{orgId}/...`
- **Data scoping strategy**:
  - Entity có `org_id` trực tiếp: scope theo `org_id`.
  - Entity không có `org_id` trực tiếp (vd `issues`, `comments`): scope qua join `project.org_id`.
- **Auth**: Spring Security (JWT hoặc session) — MVP chỉ cần principal có `userId`, và có khả năng xác định membership theo Org/Project.
- **Soft delete**: Nhiều bảng có `is_deleted`/`is_active` → mặc định query chỉ lấy `is_deleted=false` (có thể enforce ở layer repository/spec).

---

## Module map (bounded contexts)

Các module dưới đây là “ranh giới trách nhiệm” (package/module boundaries) để triển khai service/controller và test.

- **Auth & Identity**
  - Users, sessions, email verification, password reset.
- **Tenant / Organization & Membership**
  - Org CRUD, org membership, org roles.
- **Project**
  - Project CRUD, project membership, visibility.
- **Workflow**
  - Workflow + statuses + transitions, validate transition.
- **Sprint**
  - Sprint lifecycle + sprint assignment (thông qua issue.sprint_id).
- **Issue**
  - Issue CRUD, assign, status transition, parent link, due date, sprint link.
- **Comment**
  - Comment CRUD + thread (parent).
- **Notification**
  - In-app inbox + event generation từ issue/comment.
- **Audit**
  - Audit logs ghi nhận hành động.
- **AI (optional in MVP core)**
  - Ai sessions/messages (có sẵn schema), chưa bắt buộc có API ở MVP Jira-core.

---

## Actors & permissions (MVP)

- **OWNER (Org/Project)**: toàn quyền trong phạm vi Org/Project tương ứng.
- **ADMIN**: quản trị trong phạm vi Org/Project tương ứng, trừ một số “owner-only” (vd transfer ownership).
- **MEMBER**: thao tác nghiệp vụ thường ngày (tạo issue, comment, chuyển trạng thái nếu workflow cho phép).

Quy ước tối thiểu:
- **Org role** quyết định quyền quản trị Org (create project, manage org members).
- **Project role** quyết định quyền quản trị Project (workflow, sprint, project members).

---

## Use cases theo module (actor + preconditions)

### Auth & Identity

- **Register**
  - **Actor**: Anonymous
  - **Preconditions**: email chưa tồn tại
  - **Result**: tạo `users`, phát `email_verifications`
- **Login**
  - **Actor**: Anonymous
  - **Preconditions**: user tồn tại, password đúng, user active
  - **Result**: tạo `user_sessions` (hoặc JWT), trả access/refresh
- **Logout**
  - **Actor**: Authenticated user
  - **Preconditions**: session/token còn hiệu lực
  - **Result**: revoke session/refresh token
- **Verify email**
  - **Actor**: Authenticated/Anonymous (theo design)
  - **Preconditions**: verification token hợp lệ, chưa hết hạn
  - **Result**: set `verified_at`
- **Reset password (request)**
  - **Actor**: Anonymous
  - **Preconditions**: email tồn tại
  - **Result**: tạo `password_resets`
- **Reset password (confirm)**
  - **Actor**: Anonymous
  - **Preconditions**: reset token hợp lệ, chưa dùng, chưa hết hạn
  - **Result**: update `users.password_hash`, set `used_at`

### Tenant / Organization & Membership

- **Create organization**
  - **Actor**: Authenticated user
  - **Preconditions**: org `key` chưa tồn tại
  - **Result**: tạo `organizations`, tạo `organization_members` role=OWNER
- **View org details**
  - **Actor**: Org member (OWNER/ADMIN/MEMBER)
  - **Preconditions**: user là member của orgId
- **Update org**
  - **Actor**: Org OWNER/ADMIN
  - **Preconditions**: user có org role phù hợp
- **Add member to org**
  - **Actor**: Org OWNER/ADMIN
  - **Preconditions**: target user tồn tại, chưa là member
  - **Result**: tạo `organization_members`
- **Change org member role**
  - **Actor**: Org OWNER (hoặc OWNER/ADMIN theo policy)
  - **Preconditions**: target member tồn tại trong org
- **Remove org member**
  - **Actor**: Org OWNER/ADMIN
  - **Preconditions**: target member tồn tại; không được remove last OWNER (policy)

### Project

- **Create project**
  - **Actor**: Org OWNER/ADMIN
  - **Preconditions**: user là member org; `projects.key` unique trong org
  - **Result**: tạo `projects`, tạo `project_members` cho creator (OWNER)
- **View/list projects**
  - **Actor**: Org member
  - **Preconditions**: member org; apply visibility + membership rules
- **Update project**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: user là project member với role phù hợp
- **Add/remove project member**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: user target là org member; chưa là project member
- **Change project member role**
  - **Actor**: Project OWNER (hoặc OWNER/ADMIN theo policy)
  - **Preconditions**: member tồn tại

### Workflow

- **Create workflow for project**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: project tồn tại và thuộc orgId; user có quyền project admin
  - **Result**: tạo `workflows`
- **Manage statuses**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: workflow thuộc project; status name hợp lệ
  - **Result**: CRUD `workflow_statuses`
- **Define transitions**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: from/to status thuộc workflow; `transition_type` hợp lệ
  - **Result**: CRUD `workflow_transitions`
- **Validate transition (runtime)**
  - **Actor**: Member (khi chuyển issue status)
  - **Preconditions**: tồn tại transition từ status hiện tại → status mới; condition_expr (nếu có) thỏa

### Sprint

- **Create sprint**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: project thuộc orgId; sprint status mặc định `PLANNED`
- **Start sprint**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: sprint đang `PLANNED`; không có sprint `ACTIVE` khác (policy)
- **Close sprint**
  - **Actor**: Project OWNER/ADMIN
  - **Preconditions**: sprint đang `ACTIVE`
- **Assign issue to sprint**
  - **Actor**: Project member (role >= MEMBER)
  - **Preconditions**: issue thuộc project; sprint thuộc project; sprint status != CLOSED

### Issue

- **Create issue**
  - **Actor**: Project member
  - **Preconditions**: user là project member; type/priority hợp lệ; generate `issue_key` unique per project
- **View/list issues**
  - **Actor**: Project member
  - **Preconditions**: scoped theo orgId + projectId
- **Update issue fields**
  - **Actor**: Project member (policy có thể hạn chế)
  - **Preconditions**: issue thuộc project/org; field validation
- **Assign issue**
  - **Actor**: Project member
  - **Preconditions**: assignee là project member
  - **Result**: update `issues.assignee_id`, tạo `notifications` (ISSUE_ASSIGNED)
- **Move issue status**
  - **Actor**: Project member
  - **Preconditions**: workflow transition hợp lệ; update `issues.status_id`; tạo audit/notification theo policy
- **Link parent**
  - **Actor**: Project member
  - **Preconditions**: parent issue cùng project (MVP)
- **Set due date**
  - **Actor**: Project member
  - **Preconditions**: date hợp lệ

### Comment

- **Add comment**
  - **Actor**: Project member
  - **Preconditions**: issue thuộc project/org; body không rỗng
  - **Result**: tạo `comments`, tạo `notifications` (ISSUE_COMMENTED) theo policy
- **Reply comment**
  - **Actor**: Project member
  - **Preconditions**: parent comment thuộc cùng issue
- **Edit/delete comment (optional MVP)**
  - **Actor**: Comment author, hoặc Project ADMIN/OWNER
  - **Preconditions**: comment tồn tại, thuộc issue/project/org

### Notification (basic)

- **List inbox notifications**
  - **Actor**: Authenticated user
  - **Preconditions**: userId hợp lệ
  - **Result**: list `notifications` theo `recipient_id` (không cần orgId nhưng payload có thể chứa org/project)
- **Mark notification read**
  - **Actor**: Authenticated user
  - **Preconditions**: notification thuộc recipient

### Audit (supporting)

- **Write audit log on changes**
  - **Actor**: System
  - **Preconditions**: action xảy ra trong context có orgId
  - **Result**: tạo `audit_logs` với `org_id`
- **Query audit logs**
  - **Actor**: Org OWNER/ADMIN
  - **Preconditions**: scoped orgId; pagination

---

## Module boundaries + responsibilities + data ownership

### Auth & Identity

- **Owns data**: `users`, `user_sessions`, `email_verifications`, `password_resets`
- **Responsibilities**:
  - Authentication, session/JWT lifecycle
  - Password hashing, token hashing (`*_token_hash`)
  - Expose current user identity to downstream modules
- **Notes**:
  - `users` không có `org_id` vì 1 user có thể thuộc nhiều orgs.

### Tenant / Organization & Membership

- **Owns data**: `organizations`, `organization_members`
- **Responsibilities**:
  - Org CRUD
  - Org membership & role management
  - Policy: last owner constraints
- **Downstream dependencies**:
  - Project module depends on org existence & membership

### Project

- **Owns data**: `projects`, `project_members`
- **Responsibilities**:
  - Project CRUD scoped org
  - Membership scoped project, validate target user is org member
  - Enforce unique `projects.key` per org

### Workflow

- **Owns data**: `workflows`, `workflow_statuses`, `workflow_transitions`
- **Responsibilities**:
  - Workflow configuration per project
  - Validate transitions on Issue status change
  - Optional: parse/evaluate `condition_expr`
- **Data access**:
  - Must always scope through `workflows.project.org_id`

### Sprint

- **Owns data**: `sprints`
- **Responsibilities**:
  - Sprint lifecycle rules (planned/active/closed)
  - Sprint constraints per project
- **Data access**:
  - Always scope through `sprints.project.org_id`

### Issue

- **Owns data**: `issues`
- **Responsibilities**:
  - Issue CRUD & validation
  - Generate issue keys (`issue_key`) unique per project
  - Assignment, linking, due date, sprint assignment
  - Status transitions (calls Workflow module)
- **Data access**:
  - Always scope through `issues.project.org_id`

### Comment

- **Owns data**: `comments`
- **Responsibilities**:
  - Comment CRUD, thread constraints (parent comment same issue)
- **Data access**:
  - Always scope through `comments.issue.project.org_id` (via issue → project)

### Notification

- **Owns data**: `notifications`
- **Responsibilities**:
  - Create notifications on domain events
  - Inbox query per recipient
- **Data access**:
  - Usually scoped by `recipient_id`; payload should include org/project/issue identifiers for deep-linking.

### Audit

- **Owns data**: `audit_logs`
- **Responsibilities**:
  - Write-on-change hooks in service layer
  - Query by orgId + filters (entity_type/entity_id)

---

## Roadmap (phased) + detailed backlog

### Phase 0 — Foundation (1–2 tuần)

- **Architecture & conventions**
  - Define package layout: `controller`, `service`, `dto`, `mapper`, `repository`, `security`, `tenant`, `error`
  - Add global error handling (Problem Details style), validation, pagination
- **DB & Flyway**
  - Ensure Flyway runs automatically on startup
  - Add indexes/constraints sanity check (already in `V1__init.sql`)
- **Testing baseline**
  - Spring Boot test setup + Testcontainers (Postgres) OR H2 with compatible schema settings
  - Common test utilities: seed org/user/project, auth helper

### Phase 1 — Auth + Tenant context (1–2 tuần)

- **Endpoints (Auth)**
  - `POST /auth/register`
  - `POST /auth/login`
  - `POST /auth/logout`
  - `POST /auth/verify-email`
  - `POST /auth/password-reset/request`
  - `POST /auth/password-reset/confirm`
- **Security**
  - Implement SecurityConfig (JWT or session) + `UserPrincipal(userId)`
  - Add method security annotations (`@PreAuthorize`) or service-level guards
- **Tenant context**
  - Add filter/interceptor reading `orgId` from path `/orgs/{orgId}/...`
  - Set `TenantContext.setOrgId(orgId)` for the request
  - Add guard helpers: ensure current user is org member before proceeding
- **Tests**
  - Auth happy paths + invalid token cases
  - Tenant context: missing orgId should reject org-scoped endpoints

### Phase 2 — Org/Project/Membership (1–2 tuần)

- **Organization endpoints**
  - `POST /orgs`
  - `GET /orgs/{orgId}`
  - `PATCH /orgs/{orgId}`
  - `GET /orgs/{orgId}/members`
  - `POST /orgs/{orgId}/members` (add existing user)
  - `PATCH /orgs/{orgId}/members/{memberId}` (change role)
  - `DELETE /orgs/{orgId}/members/{memberId}`
- **Project endpoints**
  - `POST /orgs/{orgId}/projects`
  - `GET /orgs/{orgId}/projects`
  - `GET /orgs/{orgId}/projects/{projectId}`
  - `PATCH /orgs/{orgId}/projects/{projectId}`
  - `GET /orgs/{orgId}/projects/{projectId}/members`
  - `POST /orgs/{orgId}/projects/{projectId}/members`
  - `PATCH /orgs/{orgId}/projects/{projectId}/members/{memberId}`
  - `DELETE /orgs/{orgId}/projects/{projectId}/members/{memberId}`
- **Services**
  - OrgService: create/update, membership management, role policies
  - ProjectService: create/update, membership management, visibility rules
- **Repositories**
  - Patterns: `findByIdAndOrgId(...)`, `existsBy...AndOrgId(...)`
- **Tests**
  - RBAC: member cannot create project; admin can
  - Cross-tenant: cannot access project of other org even if projectId guessed

### Phase 3 — Workflow/Sprint/Issue/Comment (2–4 tuần)

- **Workflow endpoints**
  - `POST /orgs/{orgId}/projects/{projectId}/workflows`
  - `GET /orgs/{orgId}/projects/{projectId}/workflows/{workflowId}`
  - `POST /orgs/{orgId}/projects/{projectId}/workflows/{workflowId}/statuses`
  - `PATCH /orgs/{orgId}/projects/{projectId}/workflows/{workflowId}/statuses/{statusId}`
  - `POST /orgs/{orgId}/projects/{projectId}/workflows/{workflowId}/transitions`
  - `PATCH /orgs/{orgId}/projects/{projectId}/workflows/{workflowId}/transitions/{transitionId}`
- **Sprint endpoints**
  - `POST /orgs/{orgId}/projects/{projectId}/sprints`
  - `GET /orgs/{orgId}/projects/{projectId}/sprints`
  - `POST /orgs/{orgId}/projects/{projectId}/sprints/{sprintId}:start`
  - `POST /orgs/{orgId}/projects/{projectId}/sprints/{sprintId}:close`
- **Issue endpoints**
  - `POST /orgs/{orgId}/projects/{projectId}/issues`
  - `GET /orgs/{orgId}/projects/{projectId}/issues` (filter by status/sprint/assignee)
  - `GET /orgs/{orgId}/projects/{projectId}/issues/{issueId}`
  - `PATCH /orgs/{orgId}/projects/{projectId}/issues/{issueId}`
  - `POST /orgs/{orgId}/projects/{projectId}/issues/{issueId}:assign`
  - `POST /orgs/{orgId}/projects/{projectId}/issues/{issueId}:transition`
  - `POST /orgs/{orgId}/projects/{projectId}/issues/{issueId}:move-to-sprint`
- **Comment endpoints**
  - `POST /orgs/{orgId}/projects/{projectId}/issues/{issueId}/comments`
  - `GET /orgs/{orgId}/projects/{projectId}/issues/{issueId}/comments`
  - `POST /orgs/{orgId}/projects/{projectId}/issues/{issueId}/comments/{commentId}/replies`
- **Services**
  - WorkflowService: config + transition validation
  - SprintService: lifecycle constraints
  - IssueService: key generation, status transition flow, assignment events
  - CommentService: thread constraints + notification events
- **Tests**
  - Transition invalid should 4xx (no transition)
  - Sprint constraints (only one active)
  - Cross-tenant for issue/comment via project join

### Phase 4 — Notifications basic + Audit (1–2 tuần)

- **Notification endpoints**
  - `GET /me/notifications`
  - `POST /me/notifications/{notificationId}:read`
- **Notification generation**
  - On issue assigned → `ISSUE_ASSIGNED`
  - On comment created → `ISSUE_COMMENTED`
  - Optional: issue updated → `ISSUE_UPDATED`
- **Audit**
  - Implement AuditLogWriter in service layer
  - `GET /orgs/{orgId}/audit-logs` (filters: entity_type/entity_id, date range)
- **Tests**
  - Notification belongs-to recipient
  - Audit is org-scoped

### Phase 5 — Hardening (ongoing)

- **Tenant safety suite**
  - Add dedicated cross-tenant integration tests across all controllers
- **Performance**
  - Review N+1, add fetch joins/specifications where needed
  - Index tuning based on query patterns
- **Observability**
  - Structured logs (orgId, userId, requestId), metrics

---

## Tenant-safety guidelines (API scoping, repository patterns, tests)

### 1) API scoping rules

- **Rule A (mandatory)**: Mọi endpoint nghiệp vụ phải chứa `orgId` trong path: `/orgs/{orgId}/...`
- **Rule B**: `orgId` trong path phải được so khớp với **membership** của `userId` hiện tại trước khi xử lý.
- **Rule C**: Không được nhận `orgId` từ body/query cho các endpoint đã có `orgId` trên path.
- **Rule D**: Đối tượng con (projectId, issueId, commentId, ...) phải được load bằng query đã scope theo org, không load “by id” rồi mới check org sau.

### 2) Repository/service patterns (recommendation)

- **Always-scope find**:
  - Good: `findByIdAndProject_IdAndProject_Organization_Id(issueId, projectId, orgId)`
  - Good: `findByIdAndOrgId(projectId, orgId)` (khi entity có `org_id`)
  - Avoid: `findById(id)` rồi check `entity.getProject().getOrganization().getId()` sau.
- **Exists checks**:
  - Use `existsByIdAnd...OrgId(...)` thay vì fetch entity chỉ để validate quyền.
- **DTO inputs**
  - Không nhận “orgId/projectId” trong DTO nếu đã có ở path.
- **TenantContext**
  - Dùng `TenantContext.getOrgId()` để inject vào repository spec khi cần, nhưng vẫn ưu tiên explicit parameter `(orgId, ...)` ở service method để dễ test.

### 3) Cross-tenant test cases (must-have)

Tạo một “tenant safety suite” cho mọi module:

- **Setup**
  - Org A, Org B
  - User A (member Org A), User B (member Org B)
  - Project A thuộc Org A, Project B thuộc Org B
  - Issue/Comment/Sprint/Workflow... tương ứng theo project

- **Cases**
  - **Project cross-tenant**: userA gọi `GET /orgs/{orgA}/projects/{projectB}` → 404/403
  - **Issue cross-tenant**: userA gọi `GET /orgs/{orgA}/projects/{projectA}/issues/{issueB}` → 404/403
  - **Comment cross-tenant**: userA gọi create reply vào comment thuộc orgB → 404/403
  - **Workflow cross-tenant**: userA patch transition của workflow orgB → 404/403
  - **Audit cross-tenant**: userA query audit logs orgB → 404/403

- **Expected behavior**
  - Prefer **404** để tránh leak existence (khuyến nghị), hoặc **403** nếu policy muốn rõ ràng; chọn 1 và nhất quán.


