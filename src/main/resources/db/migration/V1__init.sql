-- Core schema for IWAS (multi-tenant via org_id on projects)

create table if not exists tenants (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    key varchar(64) not null,
    name varchar(255) not null,
    constraint uq_tenant_key unique (key)
);

create table if not exists users (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    full_name varchar(255) not null,
    gender varchar(32),
    constraint uq_user_email unique (email),
    constraint ck_user_gender check (gender is null or gender in ('MALE','FEMALE','OTHER'))
);
create index if not exists idx_user_created_at on users(created_at);

create table if not exists user_sessions (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    user_id bigint not null references users(id),
    refresh_token_hash varchar(255) not null,
    token_last4 varchar(8),
    expires_at timestamp not null
);
create index if not exists idx_user_session_user on user_sessions(user_id);
create index if not exists idx_user_session_expires_at on user_sessions(expires_at);

create table if not exists email_verifications (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    user_id bigint not null references users(id),
    verification_token_hash varchar(255) not null,
    token_last4 varchar(8),
    expires_at timestamp not null,
    verified_at timestamp
);
create index if not exists idx_email_verification_user on email_verifications(user_id);
create index if not exists idx_email_verification_expires_at on email_verifications(expires_at);

create table if not exists password_resets (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    user_id bigint not null references users(id),
    reset_token_hash varchar(255) not null,
    token_last4 varchar(8),
    expires_at timestamp not null,
    used_at timestamp
);
create index if not exists idx_password_reset_user on password_resets(user_id);
create index if not exists idx_password_reset_expires_at on password_resets(expires_at);

create table if not exists organizations (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    key varchar(64) not null,
    name varchar(255) not null,
    constraint uq_org_key unique (key)
);
create index if not exists idx_org_created_at on organizations(created_at);

create table if not exists organization_members (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    org_id bigint not null references organizations(id),
    user_id bigint not null references users(id),
    role varchar(32) not null,
    constraint uq_org_member unique (org_id, user_id),
    constraint ck_org_member_role check (role in ('OWNER','ADMIN','MEMBER'))
);
create index if not exists idx_org_member_org on organization_members(org_id);
create index if not exists idx_org_member_user on organization_members(user_id);

create table if not exists projects (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    org_id bigint not null references organizations(id),
    key varchar(32) not null,
    name varchar(255) not null,
    visibility varchar(32) not null,
    constraint uq_project_key_per_org unique (org_id, key),
    constraint ck_project_visibility check (visibility in ('PRIVATE','INTERNAL','PUBLIC'))
);
create index if not exists idx_project_org on projects(org_id);

create table if not exists project_members (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint not null references projects(id),
    user_id bigint not null references users(id),
    role varchar(32) not null,
    constraint uq_project_member unique (project_id, user_id),
    constraint ck_project_member_role check (role in ('OWNER','ADMIN','MEMBER'))
);
create index if not exists idx_project_member_project on project_members(project_id);
create index if not exists idx_project_member_user on project_members(user_id);

create table if not exists workflows (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint not null references projects(id),
    name varchar(255) not null
);
create index if not exists idx_workflow_project on workflows(project_id);

create table if not exists workflow_statuses (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    workflow_id bigint not null references workflows(id),
    name varchar(128) not null,
    category varchar(32)
);
create index if not exists idx_workflow_status_workflow on workflow_statuses(workflow_id);

create table if not exists workflow_transitions (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    workflow_id bigint not null references workflows(id),
    from_status_id bigint not null references workflow_statuses(id),
    to_status_id bigint not null references workflow_statuses(id),
    transition_type varchar(32) not null,
    condition_expr jsonb,
    constraint ck_transition_type check (transition_type in ('NORMAL','AUTOMATIC'))
);
create index if not exists idx_transition_workflow on workflow_transitions(workflow_id);
create index if not exists idx_transition_from_to on workflow_transitions(from_status_id, to_status_id);

create table if not exists sprints (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint not null references projects(id),
    name varchar(255) not null,
    status varchar(32) not null,
    start_date date,
    end_date date,
    constraint ck_sprint_status check (status in ('PLANNED','ACTIVE','CLOSED'))
);
create index if not exists idx_sprint_project on sprints(project_id);
create index if not exists idx_sprint_status on sprints(project_id, status);

create table if not exists issues (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    project_id bigint not null references projects(id),
    issue_key varchar(32) not null,
    title varchar(512) not null,
    description text,
    type varchar(32) not null,
    priority varchar(32) not null,
    status_id bigint references workflow_statuses(id),
    sprint_id bigint references sprints(id),
    assignee_id bigint references users(id),
    reporter_id bigint references users(id),
    parent_id bigint references issues(id),
    due_date date,
    constraint uq_issue_key unique (project_id, issue_key),
    constraint ck_issue_type check (type in ('STORY','TASK','BUG','EPIC')),
    constraint ck_issue_priority check (priority in ('LOW','MEDIUM','HIGH','CRITICAL'))
);
create index if not exists idx_issue_project_status on issues(project_id, status_id);
create index if not exists idx_issue_project_sprint on issues(project_id, sprint_id);
create index if not exists idx_issue_project_assignee on issues(project_id, assignee_id);
create index if not exists idx_issue_project_parent on issues(project_id, parent_id);
create index if not exists idx_issue_project_due on issues(project_id, due_date);

create table if not exists comments (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    issue_id bigint not null references issues(id),
    author_id bigint not null references users(id),
    parent_id bigint references comments(id),
    body text not null
);
create index if not exists idx_comment_issue_created_at on comments(issue_id, created_at);

create table if not exists notifications (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    recipient_id bigint not null references users(id),
    type varchar(32) not null,
    channel varchar(32) not null,
    status varchar(32) not null,
    payload jsonb,
    constraint ck_notification_type check (type in ('ISSUE_ASSIGNED','ISSUE_COMMENTED','ISSUE_UPDATED','MENTION','SYSTEM')),
    constraint ck_notification_channel check (channel in ('IN_APP','EMAIL','PUSH')),
    constraint ck_notification_status check (status in ('UNREAD','READ','ARCHIVED'))
);
create index if not exists idx_notification_recipient_created_at on notifications(recipient_id, created_at);
create index if not exists idx_notification_recipient_status_created_at on notifications(recipient_id, status, created_at);

create table if not exists audit_logs (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    org_id bigint not null references organizations(id),
    action varchar(32) not null,
    entity_type varchar(128) not null,
    entity_id bigint,
    old_value jsonb,
    new_value jsonb,
    constraint ck_audit_action check (action in ('CREATE','UPDATE','DELETE','RESTORE','LOGIN','LOGOUT'))
);
create index if not exists idx_audit_org_created_at on audit_logs(org_id, created_at);
create index if not exists idx_audit_entity_lookup on audit_logs(org_id, entity_type, entity_id, created_at);

create table if not exists ai_sessions (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    user_id bigint not null references users(id),
    status varchar(32) not null,
    constraint ck_ai_session_status check (status in ('ACTIVE','ENDED'))
);
create index if not exists idx_ai_session_user on ai_sessions(user_id);

create table if not exists ai_messages (
    id bigserial primary key,
    is_active boolean not null default true,
    is_deleted boolean not null default false,
    created_at timestamp not null default now(),
    created_by bigint,
    updated_at timestamp,
    updated_by bigint,
    session_id bigint not null references ai_sessions(id),
    role varchar(32) not null,
    content text not null,
    metadata jsonb
);
create index if not exists idx_ai_message_session_created_at on ai_messages(session_id, created_at);

