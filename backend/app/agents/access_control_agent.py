"""
Alpha VPN - Admin RBAC Agent #5: AccessControlAgent
مسئولیت: بررسی دسترسی ادمین‌ها — middleware مرکزی
این ایجنت dependency اصلی همه endpoint های ادمین است.
"""

import hashlib
from typing import Dict, List, Optional
from datetime import datetime

from fastapi import HTTPException, status, Request
from sqlalchemy.orm import Session

from app.agents.base import BaseAgent, AgentResult
from app.database import (
    Admin, AdminRole, AdminSession,
    Permission, RolePermission, AuditLog
)


# ─── نگاشت endpoint به permission ───────────────────────────────────────
ENDPOINT_PERMISSIONS: Dict[str, str] = {
    # Users
    "GET:/api/admin/users":                   "users:read",
    "GET:/api/admin/users/{username}":         "users:read",
    "POST:/api/admin/users":                   "users:write",
    "POST:/api/admin/users/bulk":              "users:bulk",
    "PATCH:/api/admin/users/{username}":       "users:write",
    "DELETE:/api/admin/users/{username}":      "users:delete",
    "POST:/api/admin/users/{username}/ban":    "users:ban",
    "POST:/api/admin/users/{username}/unban":  "users:ban",
    "POST:/api/admin/users/{username}/reset-password": "users:reset_password",
    "POST:/api/admin/users/{username}/extend": "users:extend",
    "POST:/api/admin/users/{username}/reset-devices":  "users:reset_devices",
    # Servers
    "GET:/api/admin/servers":                  "servers:read",
    "GET:/api/admin/servers/inactive":         "servers:read",
    "GET:/api/admin/servers/{server_id}":      "servers:read",
    "POST:/api/admin/servers":                 "servers:write",
    "POST:/api/admin/servers/bulk-import":     "servers:manage",
    "PATCH:/api/admin/servers/{server_id}":    "servers:write",
    "DELETE:/api/admin/servers/{server_id}":   "servers:delete",
    "POST:/api/admin/servers/{server_id}/toggle": "servers:manage",
    "PATCH:/api/admin/servers/{server_id}/ping":  "servers:write",
    "POST:/api/admin/servers/reorder":         "servers:manage",
    # Gift Codes
    "GET:/api/admin/giftcodes":                "giftcodes:read",
    "GET:/api/admin/giftcodes/stats":          "giftcodes:read",
    "GET:/api/admin/giftcodes/{code}":         "giftcodes:read",
    "POST:/api/admin/giftcodes":               "giftcodes:write",
    "POST:/api/admin/giftcodes/bulk":          "giftcodes:write",
    "DELETE:/api/admin/giftcodes/{code}":      "giftcodes:delete",
    "POST:/api/admin/giftcodes/{code}/deactivate":       "giftcodes:write",
    "POST:/api/admin/giftcodes/{code}/redeem-for-user":  "giftcodes:redeem",
    "DELETE:/api/admin/giftcodes/{code}/redemptions/{username}": "giftcodes:delete",
    # Analytics
    "GET:/api/admin/analytics/dashboard":      "analytics:read",
    "GET:/api/admin/analytics/users":          "analytics:read",
    "GET:/api/admin/analytics/servers":        "analytics:read",
    "GET:/api/admin/analytics/traffic":        "analytics:read",
    "GET:/api/admin/analytics/expiring-users": "analytics:read",
    "GET:/api/admin/analytics/top-users":      "analytics:read",
    "GET:/api/admin/analytics/daily-signups":  "analytics:read",
    "GET:/api/admin/analytics/giftcodes":      "analytics:read",
    # Notifications
    "POST:/api/admin/notifications/telegram":       "notifications:write",
    "POST:/api/admin/notifications/broadcast":      "notifications:write",
    "DELETE:/api/admin/notifications/broadcast":    "notifications:write",
    "GET:/api/admin/notifications/config":          "notifications:read",
    "PATCH:/api/admin/notifications/config":        "notifications:config",
    "POST:/api/admin/notifications/maintenance":    "notifications:config",
    "POST:/api/admin/notifications/update-notice":  "notifications:config",
    "GET:/api/admin/notifications/announcements":   "notifications:read",
    "POST:/api/admin/notifications/announcements":  "notifications:write",
    "DELETE:/api/admin/notifications/announcements/{ann_id}": "notifications:write",
    # Admin management
    "GET:/api/admin/admins":                   "admins:read",
    "GET:/api/admin/admins/{admin_id}":        "admins:read",
    "POST:/api/admin/admins":                  "admins:write",
    "PATCH:/api/admin/admins/{admin_id}":      "admins:write",
    "DELETE:/api/admin/admins/{admin_id}":     "admins:delete",
    "POST:/api/admin/admins/{admin_id}/activate":   "admins:write",
    "POST:/api/admin/admins/{admin_id}/deactivate": "admins:write",
    # Roles
    "GET:/api/admin/roles":                    "admins:manage_roles",
    "GET:/api/admin/roles/{role_name}":        "admins:manage_roles",
    "POST:/api/admin/roles":                   "admins:manage_roles",
    "PATCH:/api/admin/roles/{role_name}":      "admins:manage_roles",
    "DELETE:/api/admin/roles/{role_name}":     "admins:manage_roles",
    "POST:/api/admin/roles/{role_name}/permissions": "admins:manage_roles",
    # Audit
    "GET:/api/admin/audit":                    "audit:read",
    "GET:/api/admin/audit/{log_id}":           "audit:read",
    "GET:/api/admin/audit/stats":              "audit:read",
    "POST:/api/admin/audit/purge":             "admins:manage_roles",
}


class AccessControlAgent(BaseAgent):
    name = "AccessControlAgent"
    description = "بررسی دسترسی ادمین‌ها — middleware مرکزی RBAC"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        dispatch = {
            "check":                self._check,
            "check_permission":     self._check_permission,
            "get_admin_from_token": self._get_admin_from_token,
            "get_permissions":      self._get_permissions,
        }
        handler = dispatch.get(action)
        if not handler:
            return AgentResult.fail(f"عملیات '{action}' پشتیبانی نمی‌شود", "UNKNOWN_ACTION")
        try:
            return handler(payload)
        except Exception as exc:
            self.log(f"خطا: {exc}", "error")
            return AgentResult.fail(f"خطای داخلی: {exc}", "INTERNAL_ERROR")

    def _get_admin_from_token(self, p: Dict) -> AgentResult:
        """توکن رو اعتبارسنجی کن و ادمین رو برگردون"""
        token = p.get("token", "").strip()
        if not token:
            return AgentResult.fail("توکن ارسال نشده", "MISSING_TOKEN")

        token_hash = hashlib.sha256(token.encode()).hexdigest()

        session = self.db.query(AdminSession).filter(
            AdminSession.token_hash == token_hash,
            AdminSession.is_revoked == False,
        ).first()
        if not session:
            return AgentResult.fail("توکن نامعتبر یا منقضی", "INVALID_TOKEN")

        if session.expires_at < datetime.utcnow():
            session.is_revoked = True
            self.db.commit()
            return AgentResult.fail("توکن منقضی شده", "TOKEN_EXPIRED")

        admin = self.db.query(Admin).filter(
            Admin.id == session.admin_id,
            Admin.is_active == True,
        ).first()
        if not admin:
            return AgentResult.fail("ادمین یافت نشد یا غیرفعال است", "ADMIN_INACTIVE")

        role = self.db.query(AdminRole).filter(AdminRole.id == admin.role_id).first()
        if not role or not role.is_active:
            return AgentResult.fail("نقش ادمین فعال نیست", "ROLE_INACTIVE")

        return AgentResult.ok("ادمین شناسایی شد", {
            "admin_id":   admin.id,
            "username":   admin.username,
            "full_name":  admin.full_name,
            "role":       role.name,
            "role_id":    role.id,
        })

    def _get_permissions(self, p: Dict) -> AgentResult:
        """لیست دسترسی‌های یه ادمین"""
        admin_id = p.get("admin_id")
        admin = self.db.query(Admin).filter(Admin.id == admin_id).first()
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")

        perms = (
            self.db.query(Permission.code)
            .join(RolePermission, RolePermission.permission_id == Permission.id)
            .filter(RolePermission.role_id == admin.role_id)
            .all()
        )
        return AgentResult.ok("دسترسی‌ها", [p[0] for p in perms])

    def _check_permission(self, p: Dict) -> AgentResult:
        """بررسی داشتن یه دسترسی خاص"""
        admin_id   = p.get("admin_id")
        permission = p.get("permission", "")

        perms_result = self._get_permissions({"admin_id": admin_id})
        if not perms_result.success:
            return perms_result

        has = permission in perms_result.data
        return AgentResult.ok(
            "دارد" if has else "ندارد",
            {"has_permission": has, "permission": permission}
        )

    def _check(self, p: Dict) -> AgentResult:
        """بررسی کامل: توکن + دسترسی"""
        token      = p.get("token", "")
        permission = p.get("permission", "")

        # 1. شناسایی ادمین
        auth_result = self._get_admin_from_token({"token": token})
        if not auth_result.success:
            return auth_result

        admin_data = auth_result.data
        admin_id   = admin_data["admin_id"]

        # 2. بررسی دسترسی
        if permission:
            perm_result = self._check_permission({
                "admin_id":   admin_id,
                "permission": permission,
            })
            if not perm_result.success:
                return perm_result
            if not perm_result.data["has_permission"]:
                return AgentResult.fail(
                    f"دسترسی '{permission}' ندارید",
                    "PERMISSION_DENIED"
                )

        return AgentResult.ok("دسترسی تأیید شد", admin_data)


# ─── FastAPI Dependency ──────────────────────────────────────────────────

def get_current_admin(required_permission: Optional[str] = None):
    """
    factory برای ساخت dependency های FastAPI
    مثال: Depends(get_current_admin("users:write"))
    """
    def dependency(request: Request, db: Session = None):
        from app.database import get_db
        from fastapi import Depends

        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="توکن ارسال نشده",
                headers={"WWW-Authenticate": "Bearer"},
            )
        token = auth_header.removeprefix("Bearer ").strip()

        agent  = AccessControlAgent(db)
        result = agent.execute("check", {
            "token":      token,
            "permission": required_permission or "",
        })

        if not result.success:
            code = result.error_code or "FORBIDDEN"
            http_code = (
                status.HTTP_401_UNAUTHORIZED if code in ("INVALID_TOKEN", "TOKEN_EXPIRED", "MISSING_TOKEN", "SESSION_REVOKED")
                else status.HTTP_403_FORBIDDEN
            )
            raise HTTPException(status_code=http_code, detail=result.message)

        return result.data   # dict با admin_id, username, role, ...

    return dependency


def require_permission(permission: str):
    """
    Dependency ساده برای بررسی یه دسترسی مشخص
    مثال: Depends(require_permission("users:write"))
    """
    from fastapi import Depends
    from app.database import get_db

    def inner(request: Request, db: Session = Depends(get_db)):
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer "):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="توکن ارسال نشده",
            )
        token  = auth_header.removeprefix("Bearer ").strip()
        agent  = AccessControlAgent(db)
        result = agent.execute("check", {"token": token, "permission": permission})
        if not result.success:
            code = result.error_code or "FORBIDDEN"
            http_code = (
                status.HTTP_401_UNAUTHORIZED
                if code in ("INVALID_TOKEN", "TOKEN_EXPIRED", "MISSING_TOKEN", "SESSION_REVOKED")
                else status.HTTP_403_FORBIDDEN
            )
            raise HTTPException(status_code=http_code, detail=result.message)
        return result.data

    return inner
