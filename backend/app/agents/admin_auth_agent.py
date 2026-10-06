"""
Alpha VPN - Admin RBAC Agent #1: AdminAuthAgent
مسئولیت: لاگین ادمین، صدور JWT مخصوص ادمین، refresh توکن
"""

import os
import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Dict

from jose import jwt, JWTError
from passlib.context import CryptContext

from app.agents.base import BaseAgent, AgentResult
from app.database import Admin, AdminSession, AuditLog

SECRET_KEY = os.getenv("SECRET_KEY", "super-secret-key-change-this-now-minimum-32-chars")
ALGORITHM  = "HS256"
ADMIN_TOKEN_EXPIRE_HOURS = int(os.getenv("ADMIN_TOKEN_EXPIRE_HOURS", "12"))

pwd_ctx = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AdminAuthAgent(BaseAgent):
    name = "AdminAuthAgent"
    description = "احراز هویت ادمین‌ها — لاگین، JWT، refresh، logout"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "login":          self._login,
            "refresh":        self._refresh,
            "logout":         self._logout,
            "logout_all":     self._logout_all,
            "verify_token":   self._verify_token,
            "change_password":self._change_password,
        }
        handler = dispatch.get(action)
        if not handler:
            return AgentResult.fail(f"عملیات '{action}' پشتیبانی نمی‌شود", "UNKNOWN_ACTION")
        try:
            return handler(payload)
        except Exception as exc:
            self.log(f"خطا در {action}: {exc}", "error")
            self.db.rollback()
            return AgentResult.fail(f"خطای داخلی: {exc}", "INTERNAL_ERROR")

    def _login(self, p: Dict) -> AgentResult:
        username   = p.get("username", "").strip()
        password   = p.get("password", "")
        ip_address = p.get("ip_address", "")
        user_agent = p.get("user_agent", "")

        admin = self.db.query(Admin).filter(Admin.username == username).first()

        def _audit_fail(reason):
            self.db.add(AuditLog(
                admin_id=admin.id if admin else None,
                admin_name=username,
                action="admin.login_failed",
                resource="auth",
                target_id=username,
                detail=reason,
                ip_address=ip_address,
                status="failed",
            ))
            self.db.commit()

        if not admin:
            _audit_fail("ادمین یافت نشد")
            return AgentResult.fail("نام کاربری یا رمز اشتباه است", "INVALID_CREDENTIALS")

        if not admin.is_active:
            _audit_fail("حساب غیرفعال")
            return AgentResult.fail("حساب ادمین غیرفعال است", "ACCOUNT_DISABLED")

        if not pwd_ctx.verify(password, admin.password_hash):
            _audit_fail("رمز عبور اشتباه")
            return AgentResult.fail("نام کاربری یا رمز اشتباه است", "INVALID_CREDENTIALS")

        # صدور توکن
        token, token_hash, expires_at = self._issue_token(admin.id)

        # ذخیره session
        session = AdminSession(
            admin_id=admin.id,
            token_hash=token_hash,
            ip_address=ip_address,
            user_agent=user_agent,
            expires_at=expires_at,
            is_revoked=False,
        )
        self.db.add(session)

        admin.last_login_at = datetime.utcnow()

        self.db.add(AuditLog(
            admin_id=admin.id,
            admin_name=admin.username,
            action="admin.login",
            resource="auth",
            target_id=str(admin.id),
            detail=f"ip={ip_address}",
            ip_address=ip_address,
            status="success",
        ))
        self.db.commit()

        # بارگذاری نقش و دسترسی‌ها
        from app.database import AdminRole, RolePermission, Permission
        role = self.db.query(AdminRole).filter(AdminRole.id == admin.role_id).first()
        perms = (
            self.db.query(Permission.code)
            .join(RolePermission, RolePermission.permission_id == Permission.id)
            .filter(RolePermission.role_id == admin.role_id)
            .all()
        )
        permission_codes = [p[0] for p in perms]

        return AgentResult.ok("لاگین موفق", {
            "token":       token,
            "expires_at":  expires_at.isoformat(),
            "admin": {
                "id":          admin.id,
                "username":    admin.username,
                "full_name":   admin.full_name,
                "email":       admin.email,
                "role":        role.name if role else "",
                "role_display":role.display_name if role else "",
                "permissions": permission_codes,
            }
        })

    def _refresh(self, p: Dict) -> AgentResult:
        old_token  = p.get("token", "")
        ip_address = p.get("ip_address", "")

        result = self._verify_token({"token": old_token})
        if not result.success:
            return result

        admin_id = result.data["admin_id"]
        old_hash = self._hash_token(old_token)

        session = self.db.query(AdminSession).filter(
            AdminSession.token_hash == old_hash,
            AdminSession.is_revoked == False,
        ).first()
        if not session:
            return AgentResult.fail("session یافت نشد یا منقضی است", "SESSION_NOT_FOUND")

        # revoke قدیمی
        session.is_revoked = True

        # صدور جدید
        token, token_hash, expires_at = self._issue_token(admin_id)
        self.db.add(AdminSession(
            admin_id=admin_id,
            token_hash=token_hash,
            ip_address=ip_address,
            user_agent=session.user_agent,
            expires_at=expires_at,
            is_revoked=False,
        ))
        self.db.commit()

        return AgentResult.ok("توکن تجدید شد", {
            "token":      token,
            "expires_at": expires_at.isoformat(),
        })

    def _logout(self, p: Dict) -> AgentResult:
        token      = p.get("token", "")
        token_hash = self._hash_token(token)

        session = self.db.query(AdminSession).filter(
            AdminSession.token_hash == token_hash
        ).first()
        if session:
            session.is_revoked = True
            self.db.add(AuditLog(
                admin_id=session.admin_id,
                admin_name="",
                action="admin.logout",
                resource="auth",
                target_id=str(session.admin_id),
                ip_address=p.get("ip_address", ""),
                status="success",
            ))
            self.db.commit()
        return AgentResult.ok("خروج موفق")

    def _logout_all(self, p: Dict) -> AgentResult:
        admin_id = p.get("admin_id")
        if not admin_id:
            return AgentResult.fail("admin_id مشخص نشده", "VALIDATION_ERROR")

        count = (
            self.db.query(AdminSession)
            .filter(AdminSession.admin_id == admin_id, AdminSession.is_revoked == False)
            .update({"is_revoked": True})
        )
        self.db.commit()
        return AgentResult.ok(f"{count} session خاتمه یافت")

    def _verify_token(self, p: Dict) -> AgentResult:
        token = p.get("token", "")
        try:
            payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
            admin_id = payload.get("sub")
            if not admin_id:
                return AgentResult.fail("توکن نامعتبر", "INVALID_TOKEN")
        except JWTError:
            return AgentResult.fail("توکن نامعتبر یا منقضی", "INVALID_TOKEN")

        token_hash = self._hash_token(token)
        session = self.db.query(AdminSession).filter(
            AdminSession.token_hash == token_hash,
            AdminSession.is_revoked == False,
        ).first()
        if not session:
            return AgentResult.fail("session یافت نشد یا باطل شده", "SESSION_REVOKED")

        if session.expires_at < datetime.utcnow():
            session.is_revoked = True
            self.db.commit()
            return AgentResult.fail("توکن منقضی شده", "TOKEN_EXPIRED")

        admin = self.db.query(Admin).filter(Admin.id == int(admin_id)).first()
        if not admin or not admin.is_active:
            return AgentResult.fail("ادمین یافت نشد یا غیرفعال است", "ADMIN_INACTIVE")

        return AgentResult.ok("توکن معتبر", {
            "admin_id":  admin.id,
            "username":  admin.username,
            "role_id":   admin.role_id,
        })

    def _change_password(self, p: Dict) -> AgentResult:
        admin_id     = p.get("admin_id")
        old_password = p.get("old_password", "")
        new_password = p.get("new_password", "")

        if len(new_password) < 6:
            return AgentResult.fail("رمز جدید باید حداقل ۶ کاراکتر باشد", "VALIDATION_ERROR")

        admin = self.db.query(Admin).filter(Admin.id == admin_id).first()
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")

        if not pwd_ctx.verify(old_password, admin.password_hash):
            return AgentResult.fail("رمز فعلی اشتباه است", "INVALID_CREDENTIALS")

        admin.password_hash = pwd_ctx.hash(new_password)
        # همه session ها رو باطل کن
        self.db.query(AdminSession).filter(
            AdminSession.admin_id == admin_id
        ).update({"is_revoked": True})

        self.db.add(AuditLog(
            admin_id=admin.id,
            admin_name=admin.username,
            action="admin.change_password",
            resource="auth",
            target_id=str(admin.id),
            ip_address=p.get("ip_address", ""),
            status="success",
        ))
        self.db.commit()
        return AgentResult.ok("رمز عبور تغییر کرد، لطفاً دوباره لاگین کنید")

    # ─── helpers ───────────────────────────────────

    def _issue_token(self, admin_id: int):
        expires_at = datetime.utcnow() + timedelta(hours=ADMIN_TOKEN_EXPIRE_HOURS)
        payload = {
            "sub":  str(admin_id),
            "type": "admin",
            "exp":  expires_at,
            "jti":  secrets.token_hex(16),
        }
        token      = jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM)
        token_hash = self._hash_token(token)
        return token, token_hash, expires_at

    @staticmethod
    def _hash_token(token: str) -> str:
        return hashlib.sha256(token.encode()).hexdigest()


def hash_admin_password(password: str) -> str:
    return pwd_ctx.hash(password)
