$base = "d:\Uni\DATN\roamtrip\src\main\java\com\roamtrip"

$moves = @(
    "entity\base\BaseEntity.java|common\entity\BaseEntity.java|com.roamtrip.entity.base|com.roamtrip.common.entity",
    "entity\enums\ErrorCode.java|common\enums\ErrorCode.java|com.roamtrip.entity.enums|com.roamtrip.common.enums",
    "utils\ResponseCode.java|common\enums\ResponseCode.java|com.roamtrip.utils|com.roamtrip.common.enums",
    "dto\response\ApiResponse.java|common\dto\ApiResponse.java|com.roamtrip.dto.response|com.roamtrip.common.dto",
    "exception\AppException.java|common\exception\AppException.java|com.roamtrip.exception|com.roamtrip.common.exception",
    "exception\GlobalExceptionHandler.java|common\exception\GlobalExceptionHandler.java|com.roamtrip.exception|com.roamtrip.common.exception",
    "exception\EntityNotFoundException.java|common\exception\EntityNotFoundException.java|com.roamtrip.exception|com.roamtrip.common.exception",
    "aop\ApiResponseWrapper.java|common\aop\ApiResponseWrapper.java|com.roamtrip.aop|com.roamtrip.common.aop",
    "security\SecurityConfig.java|config\SecurityConfig.java|com.roamtrip.security|com.roamtrip.config",
    "utils\CustomUserDetails.java|security\CustomUserDetails.java|com.roamtrip.utils|com.roamtrip.security",
    "utils\CustomUserDetailsService.java|security\CustomUserDetailsService.java|com.roamtrip.utils|com.roamtrip.security",
    "utils\AuthenticatedUserResolver.java|security\AuthenticatedUserResolver.java|com.roamtrip.utils|com.roamtrip.security",
    "utils\JwtUtils.java|security\JwtUtils.java|com.roamtrip.utils|com.roamtrip.security",
    "controller\AuthController.java|auth\AuthController.java|com.roamtrip.controller|com.roamtrip.auth",
    "dto\auth\RegisterRequest.java|auth\dto\RegisterRequest.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "dto\auth\LoginRequest.java|auth\dto\LoginRequest.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "dto\auth\AuthResponse.java|auth\dto\AuthResponse.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "dto\auth\ForgotPasswordRequest.java|auth\dto\ForgotPasswordRequest.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "dto\auth\ResetPasswordRequest.java|auth\dto\ResetPasswordRequest.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "dto\auth\RefreshTokenRequest.java|auth\dto\RefreshTokenRequest.java|com.roamtrip.dto.auth|com.roamtrip.auth.dto",
    "entity\user\EmailVerification.java|auth\entity\EmailVerification.java|com.roamtrip.entity.user|com.roamtrip.auth.entity",
    "entity\user\PasswordReset.java|auth\entity\PasswordReset.java|com.roamtrip.entity.user|com.roamtrip.auth.entity",
    "repository\EmailVerificationRepository.java|auth\repository\EmailVerificationRepository.java|com.roamtrip.repository|com.roamtrip.auth.repository",
    "repository\PasswordResetRepository.java|auth\repository\PasswordResetRepository.java|com.roamtrip.repository|com.roamtrip.auth.repository",
    "auth\UserService.java|user\UserService.java|com.roamtrip.auth|com.roamtrip.user",
    "controller\UserController.java|user\UserController.java|com.roamtrip.controller|com.roamtrip.user",
    "controller\UserMeController.java|user\UserMeController.java|com.roamtrip.controller|com.roamtrip.user",
    "dto\UserDto.java|user\dto\UserDto.java|com.roamtrip.dto|com.roamtrip.user.dto",
    "dto\auth\UserMeResponse.java|user\dto\UserMeResponse.java|com.roamtrip.dto.auth|com.roamtrip.user.dto",
    "dto\auth\UpdateProfileRequest.java|user\dto\UpdateProfileRequest.java|com.roamtrip.dto.auth|com.roamtrip.user.dto",
    "dto\auth\ChangePasswordRequest.java|user\dto\ChangePasswordRequest.java|com.roamtrip.dto.auth|com.roamtrip.user.dto",
    "entity\user\User.java|user\entity\User.java|com.roamtrip.entity.user|com.roamtrip.user.entity",
    "entity\user\UserSession.java|user\entity\UserSession.java|com.roamtrip.entity.user|com.roamtrip.user.entity",
    "repository\UserRepository.java|user\repository\UserRepository.java|com.roamtrip.repository|com.roamtrip.user.repository",
    "repository\UserSessionRepository.java|user\repository\UserSessionRepository.java|com.roamtrip.repository|com.roamtrip.user.repository",
    "entity\org\Organization.java|organization\entity\Organization.java|com.roamtrip.entity.org|com.roamtrip.organization.entity",
    "entity\org\OrganizationMember.java|organization\entity\OrganizationMember.java|com.roamtrip.entity.org|com.roamtrip.organization.entity",
    "entity\enums\OrgRole.java|organization\enums\OrgRole.java|com.roamtrip.entity.enums|com.roamtrip.organization.enums",
    "entity\enums\InvitationStatus.java|organization\enums\InvitationStatus.java|com.roamtrip.entity.enums|com.roamtrip.organization.enums",
    "entity\project\Project.java|project\entity\Project.java|com.roamtrip.entity.project|com.roamtrip.project.entity",
    "entity\project\ProjectMember.java|project\entity\ProjectMember.java|com.roamtrip.entity.project|com.roamtrip.project.entity",
    "entity\enums\ProjectRole.java|project\enums\ProjectRole.java|com.roamtrip.entity.enums|com.roamtrip.project.enums",
    "entity\enums\ProjectVisibility.java|project\enums\ProjectVisibility.java|com.roamtrip.entity.enums|com.roamtrip.project.enums",
    "entity\workflow\Workflow.java|workflow\entity\Workflow.java|com.roamtrip.entity.workflow|com.roamtrip.workflow.entity",
    "entity\workflow\WorkflowStatus.java|workflow\entity\WorkflowStatus.java|com.roamtrip.entity.workflow|com.roamtrip.workflow.entity",
    "entity\workflow\WorkflowTransition.java|workflow\entity\WorkflowTransition.java|com.roamtrip.entity.workflow|com.roamtrip.workflow.entity",
    "entity\enums\WorkflowTransitionType.java|workflow\enums\WorkflowTransitionType.java|com.roamtrip.entity.enums|com.roamtrip.workflow.enums",
    "entity\sprint\Sprint.java|sprint\entity\Sprint.java|com.roamtrip.entity.sprint|com.roamtrip.sprint.entity",
    "entity\enums\SprintStatus.java|sprint\enums\SprintStatus.java|com.roamtrip.entity.enums|com.roamtrip.sprint.enums",
    "entity\issue\Issue.java|issue\entity\Issue.java|com.roamtrip.entity.issue|com.roamtrip.issue.entity",
    "entity\enums\IssuePriority.java|issue\enums\IssuePriority.java|com.roamtrip.entity.enums|com.roamtrip.issue.enums",
    "entity\enums\IssueType.java|issue\enums\IssueType.java|com.roamtrip.entity.enums|com.roamtrip.issue.enums",
    "entity\comment\Comment.java|comment\entity\Comment.java|com.roamtrip.entity.comment|com.roamtrip.comment.entity",
    "entity\notification\Notification.java|notification\entity\Notification.java|com.roamtrip.entity.notification|com.roamtrip.notification.entity",
    "entity\enums\NotificationChannel.java|notification\enums\NotificationChannel.java|com.roamtrip.entity.enums|com.roamtrip.notification.enums",
    "entity\enums\NotificationStatus.java|notification\enums\NotificationStatus.java|com.roamtrip.entity.enums|com.roamtrip.notification.enums",
    "entity\enums\NotificationType.java|notification\enums\NotificationType.java|com.roamtrip.entity.enums|com.roamtrip.notification.enums",
    "entity\audit\AuditLog.java|audit\entity\AuditLog.java|com.roamtrip.entity.audit|com.roamtrip.audit.entity",
    "entity\enums\AuditAction.java|audit\enums\AuditAction.java|com.roamtrip.entity.enums|com.roamtrip.audit.enums",
    "entity\ai\AiSession.java|ai\entity\AiSession.java|com.roamtrip.entity.ai|com.roamtrip.ai.entity",
    "entity\ai\AiMessage.java|ai\entity\AiMessage.java|com.roamtrip.entity.ai|com.roamtrip.ai.entity",
    "entity\enums\AiSessionStatus.java|ai\enums\AiSessionStatus.java|com.roamtrip.entity.enums|com.roamtrip.ai.enums",
    "entity\Tenant.java|tenant\entity\Tenant.java|com.roamtrip.entity|com.roamtrip.tenant.entity",
    "entity\enums\Gender.java|common\enums\Gender.java|com.roamtrip.entity.enums|com.roamtrip.common.enums"
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
