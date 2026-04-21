$base = "d:\Uni\DATN\roamtrip\src\main\java\com\roamtrip"

$moves = @(
    "entity\base\BaseEntity.java|common\entity\BaseEntity.java|com.iwas.entity.base|com.iwas.common.entity",
    "entity\enums\ErrorCode.java|common\enums\ErrorCode.java|com.iwas.entity.enums|com.iwas.common.enums",
    "utils\ResponseCode.java|common\enums\ResponseCode.java|com.iwas.utils|com.iwas.common.enums",
    "dto\response\ApiResponse.java|common\dto\ApiResponse.java|com.iwas.dto.response|com.iwas.common.dto",
    "exception\AppException.java|common\exception\AppException.java|com.iwas.exception|com.iwas.common.exception",
    "exception\GlobalExceptionHandler.java|common\exception\GlobalExceptionHandler.java|com.iwas.exception|com.iwas.common.exception",
    "exception\EntityNotFoundException.java|common\exception\EntityNotFoundException.java|com.iwas.exception|com.iwas.common.exception",
    "aop\ApiResponseWrapper.java|common\aop\ApiResponseWrapper.java|com.iwas.aop|com.iwas.common.aop",
    "security\SecurityConfig.java|config\SecurityConfig.java|com.iwas.security|com.iwas.config",
    "utils\CustomUserDetails.java|security\CustomUserDetails.java|com.iwas.utils|com.iwas.security",
    "utils\CustomUserDetailsService.java|security\CustomUserDetailsService.java|com.iwas.utils|com.iwas.security",
    "utils\AuthenticatedUserResolver.java|security\AuthenticatedUserResolver.java|com.iwas.utils|com.iwas.security",
    "utils\JwtUtils.java|security\JwtUtils.java|com.iwas.utils|com.iwas.security",
    "controller\AuthController.java|auth\AuthController.java|com.iwas.controller|com.iwas.auth",
    "dto\auth\RegisterRequest.java|auth\dto\RegisterRequest.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "dto\auth\LoginRequest.java|auth\dto\LoginRequest.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "dto\auth\AuthResponse.java|auth\dto\AuthResponse.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "dto\auth\ForgotPasswordRequest.java|auth\dto\ForgotPasswordRequest.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "dto\auth\ResetPasswordRequest.java|auth\dto\ResetPasswordRequest.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "dto\auth\RefreshTokenRequest.java|auth\dto\RefreshTokenRequest.java|com.iwas.dto.auth|com.iwas.auth.dto",
    "entity\user\EmailVerification.java|auth\entity\EmailVerification.java|com.iwas.entity.user|com.iwas.auth.entity",
    "entity\user\PasswordReset.java|auth\entity\PasswordReset.java|com.iwas.entity.user|com.iwas.auth.entity",
    "repository\EmailVerificationRepository.java|auth\repository\EmailVerificationRepository.java|com.iwas.repository|com.iwas.auth.repository",
    "repository\PasswordResetRepository.java|auth\repository\PasswordResetRepository.java|com.iwas.repository|com.iwas.auth.repository",
    "auth\UserService.java|user\UserService.java|com.iwas.auth|com.iwas.user",
    "controller\UserController.java|user\UserController.java|com.iwas.controller|com.iwas.user",
    "controller\UserMeController.java|user\UserMeController.java|com.iwas.controller|com.iwas.user",
    "dto\UserDto.java|user\dto\UserDto.java|com.iwas.dto|com.iwas.user.dto",
    "dto\auth\UserMeResponse.java|user\dto\UserMeResponse.java|com.iwas.dto.auth|com.iwas.user.dto",
    "dto\auth\UpdateProfileRequest.java|user\dto\UpdateProfileRequest.java|com.iwas.dto.auth|com.iwas.user.dto",
    "dto\auth\ChangePasswordRequest.java|user\dto\ChangePasswordRequest.java|com.iwas.dto.auth|com.iwas.user.dto",
    "entity\user\User.java|user\entity\User.java|com.iwas.entity.user|com.iwas.user.entity",
    "entity\user\UserSession.java|user\entity\UserSession.java|com.iwas.entity.user|com.iwas.user.entity",
    "repository\UserRepository.java|user\repository\UserRepository.java|com.iwas.repository|com.iwas.user.repository",
    "repository\UserSessionRepository.java|user\repository\UserSessionRepository.java|com.iwas.repository|com.iwas.user.repository",
    "entity\org\Organization.java|organization\entity\Organization.java|com.iwas.entity.org|com.iwas.organization.entity",
    "entity\org\OrganizationMember.java|organization\entity\OrganizationMember.java|com.iwas.entity.org|com.iwas.organization.entity",
    "entity\enums\OrgRole.java|organization\enums\OrgRole.java|com.iwas.entity.enums|com.iwas.organization.enums",
    "entity\enums\InvitationStatus.java|organization\enums\InvitationStatus.java|com.iwas.entity.enums|com.iwas.organization.enums",
    "entity\project\Project.java|project\entity\Project.java|com.iwas.entity.project|com.iwas.project.entity",
    "entity\project\ProjectMember.java|project\entity\ProjectMember.java|com.iwas.entity.project|com.iwas.project.entity",
    "entity\enums\ProjectRole.java|project\enums\ProjectRole.java|com.iwas.entity.enums|com.iwas.project.enums",
    "entity\enums\ProjectVisibility.java|project\enums\ProjectVisibility.java|com.iwas.entity.enums|com.iwas.project.enums",
    "entity\workflow\Workflow.java|workflow\entity\Workflow.java|com.iwas.entity.workflow|com.iwas.workflow.entity",
    "entity\workflow\WorkflowStatus.java|workflow\entity\WorkflowStatus.java|com.iwas.entity.workflow|com.iwas.workflow.entity",
    "entity\workflow\WorkflowTransition.java|workflow\entity\WorkflowTransition.java|com.iwas.entity.workflow|com.iwas.workflow.entity",
    "entity\enums\WorkflowTransitionType.java|workflow\enums\WorkflowTransitionType.java|com.iwas.entity.enums|com.iwas.workflow.enums",
    "entity\sprint\Sprint.java|sprint\entity\Sprint.java|com.iwas.entity.sprint|com.iwas.sprint.entity",
    "entity\enums\SprintStatus.java|sprint\enums\SprintStatus.java|com.iwas.entity.enums|com.iwas.sprint.enums",
    "entity\issue\Issue.java|issue\entity\Issue.java|com.iwas.entity.issue|com.iwas.issue.entity",
    "entity\enums\IssuePriority.java|issue\enums\IssuePriority.java|com.iwas.entity.enums|com.iwas.issue.enums",
    "entity\enums\IssueType.java|issue\enums\IssueType.java|com.iwas.entity.enums|com.iwas.issue.enums",
    "entity\comment\Comment.java|comment\entity\Comment.java|com.iwas.entity.comment|com.iwas.comment.entity",
    "entity\notification\Notification.java|notification\entity\Notification.java|com.iwas.entity.notification|com.iwas.notification.entity",
    "entity\enums\NotificationChannel.java|notification\enums\NotificationChannel.java|com.iwas.entity.enums|com.iwas.notification.enums",
    "entity\enums\NotificationStatus.java|notification\enums\NotificationStatus.java|com.iwas.entity.enums|com.iwas.notification.enums",
    "entity\enums\NotificationType.java|notification\enums\NotificationType.java|com.iwas.entity.enums|com.iwas.notification.enums",
    "entity\audit\AuditLog.java|audit\entity\AuditLog.java|com.iwas.entity.audit|com.iwas.audit.entity",
    "entity\enums\AuditAction.java|audit\enums\AuditAction.java|com.iwas.entity.enums|com.iwas.audit.enums",
    "entity\ai\AiSession.java|ai\entity\AiSession.java|com.iwas.entity.ai|com.iwas.ai.entity",
    "entity\ai\AiMessage.java|ai\entity\AiMessage.java|com.iwas.entity.ai|com.iwas.ai.entity",
    "entity\enums\AiSessionStatus.java|ai\enums\AiSessionStatus.java|com.iwas.entity.enums|com.iwas.ai.enums",
    "entity\Tenant.java|tenant\entity\Tenant.java|com.iwas.entity|com.iwas.tenant.entity",
    "entity\enums\Gender.java|common\enums\Gender.java|com.iwas.entity.enums|com.iwas.common.enums"
)

# Build FQCN mapping for import replacement
$packageMap = @{}
foreach ($entry in $moves) {
    $parts = $entry.Split("|")
    $oldPkg = $parts[2]
    $newPkg = $parts[3]
    $fileName = [System.IO.Path]::GetFileNameWithoutExtension($parts[0])
    $oldFqcn = "$oldPkg.$fileName"
    $newFqcn = "$newPkg.$fileName"
    $packageMap[$oldFqcn] = $newFqcn
}

Write-Host "=== Step 1: Copy files to new locations ==="
foreach ($entry in $moves) {
    $parts = $entry.Split("|")
    $oldRel = $parts[0]
    $newRel = $parts[1]
    $oldPath = Join-Path $base $oldRel
    $newPath = Join-Path $base $newRel

    if (Test-Path $oldPath) {
        $newDir = Split-Path $newPath -Parent
        if (!(Test-Path $newDir)) {
            New-Item -ItemType Directory -Path $newDir -Force | Out-Null
        }
        Copy-Item $oldPath $newPath -Force
        Write-Host "  COPY: $oldRel -> $newRel"
    } else {
        Write-Host "  SKIP (not found): $oldRel"
    }
}

Write-Host "`n=== Step 2: Update package declarations and imports ==="
$allJavaFiles = Get-ChildItem -Path $base -Recurse -Filter "*.java" | Where-Object { $_.FullName -notlike "*\target\*" }

foreach ($file in $allJavaFiles) {
    $content = Get-Content $file.FullName -Raw -Encoding UTF8
    if ($null -eq $content) { continue }
    $original = $content

    # Update package declarations for moved files
    foreach ($entry in $moves) {
        $parts = $entry.Split("|")
        $newRel = $parts[1]
        $oldPkg = $parts[2]
        $newPkg = $parts[3]
        $newPath = Join-Path $base $newRel

        if ($file.FullName -eq $newPath) {
            $content = $content -replace "^package\s+$([regex]::Escape($oldPkg))\s*;", "package $newPkg;"
        }
    }

    # Update all imports
    foreach ($oldFqcn in $packageMap.Keys) {
        $newFqcn = $packageMap[$oldFqcn]
        $content = $content -replace "import\s+$([regex]::Escape($oldFqcn))\s*;", "import $newFqcn;"
    }

    if ($content -ne $original) {
        [System.IO.File]::WriteAllText($file.FullName, $content, [System.Text.UTF8Encoding]::new($false))
        $relPath = $file.FullName.Substring($base.Length + 1)
        Write-Host "  UPDATED: $relPath"
    }
}

Write-Host "`n=== Step 3: Delete old files ==="
foreach ($entry in $moves) {
    $parts = $entry.Split("|")
    $oldRel = $parts[0]
    $newRel = $parts[1]
    if ($oldRel -eq $newRel) { continue }
    $oldPath = Join-Path $base $oldRel
    $newPath = Join-Path $base $newRel
    if ((Test-Path $oldPath) -and (Test-Path $newPath)) {
        Remove-Item $oldPath -Force
        Write-Host "  DELETED: $oldRel"
    }
}

Write-Host "`n=== Step 4: Clean up empty directories ==="
$emptyDirs = @("entity\base", "entity\enums", "entity\user", "entity\org", "entity\project",
               "entity\workflow", "entity\sprint", "entity\issue", "entity\comment",
               "entity\notification", "entity\audit", "entity\ai", "entity",
               "dto\auth", "dto\response", "dto",
               "repository", "controller", "utils", "aop")
foreach ($d in $emptyDirs) {
    $dirPath = Join-Path $base $d
    if (Test-Path $dirPath) {
        $remaining = Get-ChildItem $dirPath -Recurse -File
        if ($remaining.Count -eq 0) {
            Remove-Item $dirPath -Recurse -Force
            Write-Host "  REMOVED DIR: $d"
        } else {
            Write-Host "  KEEPING DIR (not empty): $d"
        }
    }
}

Write-Host "`n=== DONE ==="
