@echo off
setlocal enabledelayedexpansion
set BASE=d:\Uni\DATN\roamtrip\src\main\java\com\roamtrip

echo === Step 1: Create new directories ===
mkdir "%BASE%\common\entity" 2>nul
mkdir "%BASE%\common\enums" 2>nul
mkdir "%BASE%\common\dto" 2>nul
mkdir "%BASE%\common\exception" 2>nul
mkdir "%BASE%\common\aop" 2>nul
mkdir "%BASE%\auth\dto" 2>nul
mkdir "%BASE%\auth\entity" 2>nul
mkdir "%BASE%\auth\repository" 2>nul
mkdir "%BASE%\user\dto" 2>nul
mkdir "%BASE%\user\entity" 2>nul
mkdir "%BASE%\user\repository" 2>nul
mkdir "%BASE%\organization\entity" 2>nul
mkdir "%BASE%\organization\enums" 2>nul
mkdir "%BASE%\project\entity" 2>nul
mkdir "%BASE%\project\enums" 2>nul
mkdir "%BASE%\workflow\entity" 2>nul
mkdir "%BASE%\workflow\enums" 2>nul
mkdir "%BASE%\sprint\entity" 2>nul
mkdir "%BASE%\sprint\enums" 2>nul
mkdir "%BASE%\issue\entity" 2>nul
mkdir "%BASE%\issue\enums" 2>nul
mkdir "%BASE%\comment\entity" 2>nul
mkdir "%BASE%\notification\entity" 2>nul
mkdir "%BASE%\notification\enums" 2>nul
mkdir "%BASE%\audit\entity" 2>nul
mkdir "%BASE%\audit\enums" 2>nul
mkdir "%BASE%\ai\entity" 2>nul
mkdir "%BASE%\ai\enums" 2>nul
mkdir "%BASE%\tenant\entity" 2>nul

echo === Step 2: Copy files to new locations ===
REM --- common/ ---
copy /Y "%BASE%\entity\base\BaseEntity.java" "%BASE%\common\entity\BaseEntity.java"
copy /Y "%BASE%\entity\enums\ErrorCode.java" "%BASE%\common\enums\ErrorCode.java"
copy /Y "%BASE%\utils\ResponseCode.java" "%BASE%\common\enums\ResponseCode.java"
copy /Y "%BASE%\dto\response\ApiResponse.java" "%BASE%\common\dto\ApiResponse.java"
copy /Y "%BASE%\exception\AppException.java" "%BASE%\common\exception\AppException.java"
copy /Y "%BASE%\exception\GlobalExceptionHandler.java" "%BASE%\common\exception\GlobalExceptionHandler.java"
copy /Y "%BASE%\exception\EntityNotFoundException.java" "%BASE%\common\exception\EntityNotFoundException.java"
copy /Y "%BASE%\aop\ApiResponseWrapper.java" "%BASE%\common\aop\ApiResponseWrapper.java"
copy /Y "%BASE%\entity\enums\Gender.java" "%BASE%\common\enums\Gender.java"

REM --- config/ (SecurityConfig from security/) ---
copy /Y "%BASE%\security\SecurityConfig.java" "%BASE%\config\SecurityConfig.java"

REM --- security/ (from utils/) ---
copy /Y "%BASE%\utils\CustomUserDetails.java" "%BASE%\security\CustomUserDetails.java"
copy /Y "%BASE%\utils\CustomUserDetailsService.java" "%BASE%\security\CustomUserDetailsService.java"
copy /Y "%BASE%\utils\AuthenticatedUserResolver.java" "%BASE%\security\AuthenticatedUserResolver.java"
copy /Y "%BASE%\utils\JwtUtils.java" "%BASE%\security\JwtUtils.java"

REM --- auth/ ---
copy /Y "%BASE%\controller\AuthController.java" "%BASE%\auth\AuthController.java"
copy /Y "%BASE%\dto\auth\RegisterRequest.java" "%BASE%\auth\dto\RegisterRequest.java"
copy /Y "%BASE%\dto\auth\LoginRequest.java" "%BASE%\auth\dto\LoginRequest.java"
copy /Y "%BASE%\dto\auth\AuthResponse.java" "%BASE%\auth\dto\AuthResponse.java"
copy /Y "%BASE%\dto\auth\ForgotPasswordRequest.java" "%BASE%\auth\dto\ForgotPasswordRequest.java"
copy /Y "%BASE%\dto\auth\ResetPasswordRequest.java" "%BASE%\auth\dto\ResetPasswordRequest.java"
copy /Y "%BASE%\dto\auth\RefreshTokenRequest.java" "%BASE%\auth\dto\RefreshTokenRequest.java"
copy /Y "%BASE%\entity\user\EmailVerification.java" "%BASE%\auth\entity\EmailVerification.java"
copy /Y "%BASE%\entity\user\PasswordReset.java" "%BASE%\auth\entity\PasswordReset.java"
copy /Y "%BASE%\repository\EmailVerificationRepository.java" "%BASE%\auth\repository\EmailVerificationRepository.java"
copy /Y "%BASE%\repository\PasswordResetRepository.java" "%BASE%\auth\repository\PasswordResetRepository.java"

REM --- user/ ---
copy /Y "%BASE%\auth\UserService.java" "%BASE%\user\UserService.java"
copy /Y "%BASE%\controller\UserController.java" "%BASE%\user\UserController.java"
copy /Y "%BASE%\controller\UserMeController.java" "%BASE%\user\UserMeController.java"
copy /Y "%BASE%\dto\UserDto.java" "%BASE%\user\dto\UserDto.java"
copy /Y "%BASE%\dto\auth\UserMeResponse.java" "%BASE%\user\dto\UserMeResponse.java"
copy /Y "%BASE%\dto\auth\UpdateProfileRequest.java" "%BASE%\user\dto\UpdateProfileRequest.java"
copy /Y "%BASE%\dto\auth\ChangePasswordRequest.java" "%BASE%\user\dto\ChangePasswordRequest.java"
copy /Y "%BASE%\entity\user\User.java" "%BASE%\user\entity\User.java"
copy /Y "%BASE%\entity\user\UserSession.java" "%BASE%\user\entity\UserSession.java"
copy /Y "%BASE%\repository\UserRepository.java" "%BASE%\user\repository\UserRepository.java"
copy /Y "%BASE%\repository\UserSessionRepository.java" "%BASE%\user\repository\UserSessionRepository.java"

REM --- organization/ ---
copy /Y "%BASE%\entity\org\Organization.java" "%BASE%\organization\entity\Organization.java"
copy /Y "%BASE%\entity\org\OrganizationMember.java" "%BASE%\organization\entity\OrganizationMember.java"
copy /Y "%BASE%\entity\enums\OrgRole.java" "%BASE%\organization\enums\OrgRole.java"
copy /Y "%BASE%\entity\enums\InvitationStatus.java" "%BASE%\organization\enums\InvitationStatus.java"

REM --- project/ ---
copy /Y "%BASE%\entity\project\Project.java" "%BASE%\project\entity\Project.java"
copy /Y "%BASE%\entity\project\ProjectMember.java" "%BASE%\project\entity\ProjectMember.java"
copy /Y "%BASE%\entity\enums\ProjectRole.java" "%BASE%\project\enums\ProjectRole.java"
copy /Y "%BASE%\entity\enums\ProjectVisibility.java" "%BASE%\project\enums\ProjectVisibility.java"

REM --- workflow/ ---
copy /Y "%BASE%\entity\workflow\Workflow.java" "%BASE%\workflow\entity\Workflow.java"
copy /Y "%BASE%\entity\workflow\WorkflowStatus.java" "%BASE%\workflow\entity\WorkflowStatus.java"
copy /Y "%BASE%\entity\workflow\WorkflowTransition.java" "%BASE%\workflow\entity\WorkflowTransition.java"
copy /Y "%BASE%\entity\enums\WorkflowTransitionType.java" "%BASE%\workflow\enums\WorkflowTransitionType.java"

REM --- sprint/ ---
copy /Y "%BASE%\entity\sprint\Sprint.java" "%BASE%\sprint\entity\Sprint.java"
copy /Y "%BASE%\entity\enums\SprintStatus.java" "%BASE%\sprint\enums\SprintStatus.java"

REM --- issue/ ---
copy /Y "%BASE%\entity\issue\Issue.java" "%BASE%\issue\entity\Issue.java"
copy /Y "%BASE%\entity\enums\IssuePriority.java" "%BASE%\issue\enums\IssuePriority.java"
copy /Y "%BASE%\entity\enums\IssueType.java" "%BASE%\issue\enums\IssueType.java"

REM --- comment/ ---
copy /Y "%BASE%\entity\comment\Comment.java" "%BASE%\comment\entity\Comment.java"

REM --- notification/ ---
copy /Y "%BASE%\entity\notification\Notification.java" "%BASE%\notification\entity\Notification.java"
copy /Y "%BASE%\entity\enums\NotificationChannel.java" "%BASE%\notification\enums\NotificationChannel.java"
copy /Y "%BASE%\entity\enums\NotificationStatus.java" "%BASE%\notification\enums\NotificationStatus.java"
copy /Y "%BASE%\entity\enums\NotificationType.java" "%BASE%\notification\enums\NotificationType.java"

REM --- audit/ ---
copy /Y "%BASE%\entity\audit\AuditLog.java" "%BASE%\audit\entity\AuditLog.java"
copy /Y "%BASE%\entity\enums\AuditAction.java" "%BASE%\audit\enums\AuditAction.java"

REM --- ai/ ---
copy /Y "%BASE%\entity\ai\AiSession.java" "%BASE%\ai\entity\AiSession.java"
copy /Y "%BASE%\entity\ai\AiMessage.java" "%BASE%\ai\entity\AiMessage.java"
copy /Y "%BASE%\entity\enums\AiSessionStatus.java" "%BASE%\ai\enums\AiSessionStatus.java"

REM --- tenant/ ---
copy /Y "%BASE%\entity\Tenant.java" "%BASE%\tenant\entity\Tenant.java"

echo === Step 3: Delete old files ===
del /Q "%BASE%\entity\base\BaseEntity.java"
del /Q "%BASE%\entity\enums\ErrorCode.java"
del /Q "%BASE%\entity\enums\Gender.java"
del /Q "%BASE%\utils\ResponseCode.java"
del /Q "%BASE%\dto\response\ApiResponse.java"
del /Q "%BASE%\exception\AppException.java"
del /Q "%BASE%\exception\GlobalExceptionHandler.java"
del /Q "%BASE%\exception\EntityNotFoundException.java"
del /Q "%BASE%\aop\ApiResponseWrapper.java"
del /Q "%BASE%\security\SecurityConfig.java"
del /Q "%BASE%\utils\CustomUserDetails.java"
del /Q "%BASE%\utils\CustomUserDetailsService.java"
del /Q "%BASE%\utils\AuthenticatedUserResolver.java"
del /Q "%BASE%\utils\JwtUtils.java"
del /Q "%BASE%\controller\AuthController.java"
del /Q "%BASE%\dto\auth\RegisterRequest.java"
del /Q "%BASE%\dto\auth\LoginRequest.java"
del /Q "%BASE%\dto\auth\AuthResponse.java"
del /Q "%BASE%\dto\auth\ForgotPasswordRequest.java"
del /Q "%BASE%\dto\auth\ResetPasswordRequest.java"
del /Q "%BASE%\dto\auth\RefreshTokenRequest.java"
del /Q "%BASE%\entity\user\EmailVerification.java"
del /Q "%BASE%\entity\user\PasswordReset.java"
del /Q "%BASE%\repository\EmailVerificationRepository.java"
del /Q "%BASE%\repository\PasswordResetRepository.java"
del /Q "%BASE%\auth\UserService.java"
del /Q "%BASE%\controller\UserController.java"
del /Q "%BASE%\controller\UserMeController.java"
del /Q "%BASE%\dto\UserDto.java"
del /Q "%BASE%\dto\auth\UserMeResponse.java"
del /Q "%BASE%\dto\auth\UpdateProfileRequest.java"
del /Q "%BASE%\dto\auth\ChangePasswordRequest.java"
del /Q "%BASE%\entity\user\User.java"
del /Q "%BASE%\entity\user\UserSession.java"
del /Q "%BASE%\repository\UserRepository.java"
del /Q "%BASE%\repository\UserSessionRepository.java"
del /Q "%BASE%\entity\org\Organization.java"
del /Q "%BASE%\entity\org\OrganizationMember.java"
del /Q "%BASE%\entity\enums\OrgRole.java"
del /Q "%BASE%\entity\enums\InvitationStatus.java"
del /Q "%BASE%\entity\project\Project.java"
del /Q "%BASE%\entity\project\ProjectMember.java"
del /Q "%BASE%\entity\enums\ProjectRole.java"
del /Q "%BASE%\entity\enums\ProjectVisibility.java"
del /Q "%BASE%\entity\workflow\Workflow.java"
del /Q "%BASE%\entity\workflow\WorkflowStatus.java"
del /Q "%BASE%\entity\workflow\WorkflowTransition.java"
del /Q "%BASE%\entity\enums\WorkflowTransitionType.java"
del /Q "%BASE%\entity\sprint\Sprint.java"
del /Q "%BASE%\entity\enums\SprintStatus.java"
del /Q "%BASE%\entity\issue\Issue.java"
del /Q "%BASE%\entity\enums\IssuePriority.java"
del /Q "%BASE%\entity\enums\IssueType.java"
del /Q "%BASE%\entity\comment\Comment.java"
del /Q "%BASE%\entity\notification\Notification.java"
del /Q "%BASE%\entity\enums\NotificationChannel.java"
del /Q "%BASE%\entity\enums\NotificationStatus.java"
del /Q "%BASE%\entity\enums\NotificationType.java"
del /Q "%BASE%\entity\audit\AuditLog.java"
del /Q "%BASE%\entity\enums\AuditAction.java"
del /Q "%BASE%\entity\ai\AiSession.java"
del /Q "%BASE%\entity\ai\AiMessage.java"
del /Q "%BASE%\entity\enums\AiSessionStatus.java"
del /Q "%BASE%\entity\Tenant.java"

echo === Step 4: Clean up empty directories ===
rmdir "%BASE%\entity\base" 2>nul
rmdir "%BASE%\entity\enums" 2>nul
rmdir "%BASE%\entity\user" 2>nul
rmdir "%BASE%\entity\org" 2>nul
rmdir "%BASE%\entity\project" 2>nul
rmdir "%BASE%\entity\workflow" 2>nul
rmdir "%BASE%\entity\sprint" 2>nul
rmdir "%BASE%\entity\issue" 2>nul
rmdir "%BASE%\entity\comment" 2>nul
rmdir "%BASE%\entity\notification" 2>nul
rmdir "%BASE%\entity\audit" 2>nul
rmdir "%BASE%\entity\ai" 2>nul
rmdir "%BASE%\entity" 2>nul
rmdir "%BASE%\dto\auth" 2>nul
rmdir "%BASE%\dto\response" 2>nul
rmdir "%BASE%\dto" 2>nul
rmdir "%BASE%\repository" 2>nul
rmdir "%BASE%\controller" 2>nul
rmdir "%BASE%\utils" 2>nul
rmdir "%BASE%\aop" 2>nul
rmdir "%BASE%\exception" 2>nul

echo === DONE ===
