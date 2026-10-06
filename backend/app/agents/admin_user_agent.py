"""
Alpha VPN - Admin RBAC Agent #3: AdminUserAgent
مسئولیت: مدیریت حساب‌های ادمین (نه کاربران VPN)
عملیات: create, update, delete, get, list, activate, deactivate
"""

from typing import Dict, List
from datetime import datetime

from app.agents.base import BaseAgent, AgentResult
from app.database import Admin, AdminRole, AuditLog, AdminSession


class AdminUserAgent(BaseAgent):
    name = "AdminUserAgent"
    description = "مدیریت حساب‌های ادمین — ایجاد، ویرایش، فعال/غیرفعال"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "create":     self._create,
            "update":     self._update,
            "delete":     self._delete,
            "get":        self._get,
            "list":       self._list,
            "activate":   self._activate,
            "deactivate": self._deactivate,
            "profile":    self._profile,
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

    def _create(self, p: Dict) -> AgentResult:
        from app.agents.admin_auth_agent import hash_admin_password

        username  = p.get("username", "").strip()
        password  = p.get("password", "").strip()
        role_name = p.get("role", "SUPPORT").strip().upper()

        if not username or not password:
            return AgentResult.fail("نام کاربری و رمز الزامی است", "VALIDATION_ERROR")
        if len(password) < 6:
            return AgentResult.fail("رمز باید حداقل ۶ کاراکتر باشد", "VALIDATION_ERROR")
        if self.db.query(Admin).filter(Admin.username == username).first():
            return AgentResult.fail(f"ادمین '{username}' از قبل وجود دارد", "DUPLICATE")

        role = self.db.query(AdminRole).filter(AdminRole.name == role_name).first()
        if not role:
            return AgentResult.fail(f"نقش '{role_name}' یافت نشد", "ROLE_NOT_FOUND")

        admin = Admin(
            username=username,
            password_hash=hash_admin_password(password),
            full_name=p.get("full_name", ""),
            email=p.get("email", ""),
            role_id=role.id,
            is_active=True,
            created_by=p.get("created_by"),
        )
        self.db.add(admin)
        self.db.commit()

        self.db.add(AuditLog(
            admin_id=p.get("created_by"),
            admin_name=p.get("creator_name", ""),
            action="admin.create_admin",
            resource="admins",
            target_id=str(admin.id),
            detail=f"username={username} role={role_name}",
            ip_address=p.get("ip_address", ""),
            status="success",
        ))
        self.db.commit()
        return AgentResult.ok(f"ادمین '{username}' با نقش '{role_name}' ساخته شد", self._serialize(admin))

    def _update(self, p: Dict) -> AgentResult:
        admin = self._find(p.get("admin_id") or p.get("username", ""))
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")

        if "full_name" in p: admin.full_name = p["full_name"]
        if "email"     in p: admin.email     = p["email"]

        if "role" in p:
            role = self.db.query(AdminRole).filter(AdminRole.name == p["role"].upper()).first()
            if not role:
                return AgentResult.fail(f"نقش '{p['role']}' یافت نشد", "ROLE_NOT_FOUND")
            admin.role_id = role.id

        self.db.commit()
        return AgentResult.ok(f"ادمین '{admin.username}' به‌روز شد", self._serialize(admin))

    def _delete(self, p: Dict) -> AgentResult:
        admin = self._find(p.get("admin_id") or p.get("username", ""))
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")

        # نمی‌شه آخرین SUPER_ADMIN رو حذف کرد
        from app.database import AdminRole
        super_role = self.db.query(AdminRole).filter(AdminRole.name == "SUPER_ADMIN").first()
        if super_role and admin.role_id == super_role.id:
            count = self.db.query(Admin).filter(
                Admin.role_id == super_role.id, Admin.is_active == True
            ).count()
            if count <= 1:
                return AgentResult.fail("آخرین SUPER_ADMIN قابل حذف نیست", "LAST_SUPER_ADMIN")

        # همه session هاش رو باطل کن
        self.db.query(AdminSession).filter(AdminSession.admin_id == admin.id).update({"is_revoked": True})

        self.db.add(AuditLog(
            admin_id=p.get("deleted_by"),
            admin_name=p.get("deleter_name", ""),
            action="admin.delete_admin",
            resource="admins",
            target_id=str(admin.id),
            detail=f"deleted username={admin.username}",
            ip_address=p.get("ip_address", ""),
            status="success",
        ))

        username = admin.username
        self.db.delete(admin)
        self.db.commit()
        return AgentResult.ok(f"ادمین '{username}' حذف شد")

    def _get(self, p: Dict) -> AgentResult:
        admin = self._find(p.get("admin_id") or p.get("username", ""))
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")
        data = self._serialize(admin)
        # دسترسی‌ها
        from app.database import Permission, RolePermission
        perms = (
            self.db.query(Permission.code)
            .join(RolePermission, RolePermission.permission_id == Permission.id)
            .filter(RolePermission.role_id == admin.role_id)
            .all()
        )
        data["permissions"] = [p[0] for p in perms]
        return AgentResult.ok("اطلاعات ادمین", data)

    def _list(self, p: Dict) -> AgentResult:
        role_filter = p.get("role")
        active_only = p.get("active_only", False)
        query = self.db.query(Admin)
        if role_filter:
            role = self.db.query(AdminRole).filter(AdminRole.name == role_filter.upper()).first()
            if role:
                query = query.filter(Admin.role_id == role.id)
        if active_only:
            query = query.filter(Admin.is_active == True)
        admins = query.order_by(Admin.id).all()
        return AgentResult.ok(f"{len(admins)} ادمین", [self._serialize(a) for a in admins])

    def _activate(self, p: Dict) -> AgentResult:
        admin = self._find(p.get("admin_id") or p.get("username", ""))
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")
        admin.is_active = True
        self.db.commit()
        return AgentResult.ok(f"ادمین '{admin.username}' فعال شد")

    def _deactivate(self, p: Dict) -> AgentResult:
        admin = self._find(p.get("admin_id") or p.get("username", ""))
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")
        # همه session ها باطل
        self.db.query(AdminSession).filter(AdminSession.admin_id == admin.id).update({"is_revoked": True})
        admin.is_active = False
        self.db.commit()
        return AgentResult.ok(f"ادمین '{admin.username}' غیرفعال شد")

    def _profile(self, p: Dict) -> AgentResult:
        """پروفایل خود ادمین لاگین‌شده"""
        return self._get(p)

    def _find(self, identifier):
        if isinstance(identifier, int):
            return self.db.query(Admin).filter(Admin.id == identifier).first()
        if isinstance(identifier, str) and identifier.strip():
            return self.db.query(Admin).filter(Admin.username == identifier.strip()).first()
        return None

    def _serialize(self, a: Admin) -> Dict:
        role = self.db.query(AdminRole).filter(AdminRole.id == a.role_id).first()
        return {
            "id":            a.id,
            "username":      a.username,
            "full_name":     a.full_name,
            "email":         a.email,
            "role":          role.name if role else "",
            "role_display":  role.display_name if role else "",
            "is_active":     a.is_active,
            "last_login_at": a.last_login_at.isoformat() if a.last_login_at else None,
            "created_at":    a.created_at.isoformat() if a.created_at else None,
        }
