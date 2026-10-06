"""
Alpha VPN - Admin RBAC Agent #2: RolePermissionAgent
مسئولیت: مدیریت کامل نقش‌ها و دسترسی‌ها
عملیات: create_role, update_role, delete_role, list_roles, get_role,
         create_permission, list_permissions,
         assign_permission, revoke_permission,
         assign_role_to_admin, get_admin_permissions
"""

from typing import Dict, List
from app.agents.base import BaseAgent, AgentResult
from app.database import AdminRole, Permission, RolePermission, Admin

# ─── دسترسی‌های پیش‌فرض سیستم ───────────────────────────────────────────
DEFAULT_PERMISSIONS = [
    # users
    ("users:read",           "users",       "read",   "مشاهده کاربران"),
    ("users:write",          "users",       "write",  "ایجاد/ویرایش کاربران"),
    ("users:delete",         "users",       "delete", "حذف کاربران"),
    ("users:ban",            "users",       "ban",    "مسدود/رفع مسدودیت کاربران"),
    ("users:reset_password", "users",       "reset_password", "ریست رمز کاربران"),
    ("users:extend",         "users",       "extend", "تمدید اشتراک کاربران"),
    ("users:reset_devices",  "users",       "reset_devices",  "ریست دستگاه کاربران"),
    ("users:bulk",           "users",       "bulk",   "عملیات دسته‌جمعی کاربران"),
    # servers
    ("servers:read",         "servers",     "read",   "مشاهده سرورها"),
    ("servers:write",        "servers",     "write",  "ایجاد/ویرایش سرورها"),
    ("servers:delete",       "servers",     "delete", "حذف سرورها"),
    ("servers:manage",       "servers",     "manage", "مدیریت کامل سرورها"),
    # giftcodes
    ("giftcodes:read",       "giftcodes",   "read",   "مشاهده کدهای هدیه"),
    ("giftcodes:write",      "giftcodes",   "write",  "ایجاد کدهای هدیه"),
    ("giftcodes:delete",     "giftcodes",   "delete", "حذف کدهای هدیه"),
    ("giftcodes:redeem",     "giftcodes",   "redeem", "اعمال کد هدیه برای کاربران"),
    # analytics
    ("analytics:read",       "analytics",   "read",   "مشاهده آمار و گزارش‌ها"),
    # notifications
    ("notifications:read",   "notifications","read",  "مشاهده اعلان‌ها"),
    ("notifications:write",  "notifications","write", "ارسال اعلان و پیام"),
    ("notifications:config", "notifications","config","ویرایش تنظیمات اپ"),
    # admins
    ("admins:read",          "admins",      "read",   "مشاهده ادمین‌ها"),
    ("admins:write",         "admins",      "write",  "ایجاد/ویرایش ادمین‌ها"),
    ("admins:delete",        "admins",      "delete", "حذف ادمین‌ها"),
    ("admins:manage_roles",  "admins",      "manage_roles", "مدیریت نقش‌ها"),
    # audit
    ("audit:read",           "audit",       "read",   "مشاهده لاگ فعالیت‌ها"),
]

# ─── نقش‌های پیش‌فرض و دسترسی‌هاشون ─────────────────────────────────────
DEFAULT_ROLES = {
    "SUPER_ADMIN": {
        "display_name": "سوپر ادمین",
        "description":  "دسترسی کامل به تمام بخش‌ها",
        "permissions":  "*",   # همه دسترسی‌ها
    },
    "USER_MANAGER": {
        "display_name": "مدیر کاربران",
        "description":  "مدیریت کاربران و اشتراک‌ها",
        "permissions":  [
            "users:read", "users:write", "users:ban",
            "users:reset_password", "users:extend",
            "users:reset_devices", "users:bulk",
            "analytics:read",
        ],
    },
    "SERVER_MANAGER": {
        "display_name": "مدیر سرورها",
        "description":  "مدیریت کامل سرورهای VPN",
        "permissions":  [
            "servers:read", "servers:write",
            "servers:delete", "servers:manage",
            "analytics:read",
        ],
    },
    "SUPPORT": {
        "display_name": "پشتیبانی",
        "description":  "مشاهده اطلاعات و ریست دستگاه",
        "permissions":  [
            "users:read", "users:reset_devices",
            "servers:read", "analytics:read",
            "giftcodes:read",
        ],
    },
    "FINANCIAL": {
        "display_name": "مالی",
        "description":  "مدیریت کدهای هدیه و گزارش مالی",
        "permissions":  [
            "giftcodes:read", "giftcodes:write",
            "giftcodes:delete", "giftcodes:redeem",
            "users:read", "users:extend",
            "analytics:read",
        ],
    },
    "ANALYST": {
        "display_name": "تحلیل‌گر",
        "description":  "فقط مشاهده آمار و گزارش‌ها",
        "permissions":  ["analytics:read", "users:read", "servers:read"],
    },
}


class RolePermissionAgent(BaseAgent):
    name = "RolePermissionAgent"
    description = "مدیریت نقش‌ها و دسترسی‌ها — RBAC کامل"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "create_role":          self._create_role,
            "update_role":          self._update_role,
            "delete_role":          self._delete_role,
            "list_roles":           self._list_roles,
            "get_role":             self._get_role,
            "create_permission":    self._create_permission,
            "list_permissions":     self._list_permissions,
            "assign_permission":    self._assign_permission,
            "revoke_permission":    self._revoke_permission,
            "assign_role_to_admin": self._assign_role_to_admin,
            "get_admin_permissions":self._get_admin_permissions,
            "seed_defaults":        self._seed_defaults,
            "check_permission":     self._check_permission,
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

    def _create_role(self, p: Dict) -> AgentResult:
        name = p.get("name", "").strip().upper()
        if not name:
            return AgentResult.fail("نام نقش الزامی است", "VALIDATION_ERROR")
        if self.db.query(AdminRole).filter(AdminRole.name == name).first():
            return AgentResult.fail(f"نقش '{name}' از قبل وجود دارد", "DUPLICATE")

        role = AdminRole(
            name=name,
            display_name=p.get("display_name", name),
            description=p.get("description", ""),
            is_active=True,
        )
        self.db.add(role)
        self.db.commit()

        # اگه لیست دسترسی داده شده، assign کن
        perms = p.get("permissions", [])
        if perms:
            self._assign_perms_to_role(role.id, perms)

        return AgentResult.ok(f"نقش '{name}' ساخته شد", self._ser_role(role))

    def _update_role(self, p: Dict) -> AgentResult:
        role = self._find_role(p.get("name", "") or p.get("id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        if "display_name" in p: role.display_name = p["display_name"]
        if "description"  in p: role.description  = p["description"]
        if "is_active"    in p: role.is_active     = bool(p["is_active"])
        self.db.commit()
        return AgentResult.ok(f"نقش '{role.name}' به‌روز شد", self._ser_role(role))

    def _delete_role(self, p: Dict) -> AgentResult:
        role = self._find_role(p.get("name", "") or p.get("id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        if self.db.query(Admin).filter(Admin.role_id == role.id).first():
            return AgentResult.fail("این نقش به ادمین‌هایی اختصاص دارد، ابتدا نقش آن‌ها را تغییر دهید", "ROLE_IN_USE")
        self.db.query(RolePermission).filter(RolePermission.role_id == role.id).delete()
        self.db.delete(role)
        self.db.commit()
        return AgentResult.ok(f"نقش '{role.name}' حذف شد")

    def _list_roles(self, p: Dict) -> AgentResult:
        roles = self.db.query(AdminRole).order_by(AdminRole.id).all()
        return AgentResult.ok(f"{len(roles)} نقش", [self._ser_role(r) for r in roles])

    def _get_role(self, p: Dict) -> AgentResult:
        role = self._find_role(p.get("name", "") or p.get("id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        data = self._ser_role(role)
        # دسترسی‌های کامل
        perms = (
            self.db.query(Permission)
            .join(RolePermission, RolePermission.permission_id == Permission.id)
            .filter(RolePermission.role_id == role.id)
            .all()
        )
        data["permissions"] = [self._ser_perm(p) for p in perms]
        admin_count = self.db.query(Admin).filter(Admin.role_id == role.id).count()
        data["admin_count"] = admin_count
        return AgentResult.ok("اطلاعات نقش", data)

    def _create_permission(self, p: Dict) -> AgentResult:
        code = p.get("code", "").strip()
        if not code or ":" not in code:
            return AgentResult.fail("کد دسترسی باید به فرمت resource:action باشد", "VALIDATION_ERROR")
        if self.db.query(Permission).filter(Permission.code == code).first():
            return AgentResult.fail(f"دسترسی '{code}' از قبل وجود دارد", "DUPLICATE")
        parts = code.split(":", 1)
        perm = Permission(
            code=code,
            resource=p.get("resource", parts[0]),
            action=p.get("action", parts[1]),
            description=p.get("description", ""),
        )
        self.db.add(perm)
        self.db.commit()
        return AgentResult.ok(f"دسترسی '{code}' ساخته شد", self._ser_perm(perm))

    def _list_permissions(self, p: Dict) -> AgentResult:
        resource = p.get("resource")
        query = self.db.query(Permission)
        if resource:
            query = query.filter(Permission.resource == resource)
        perms = query.order_by(Permission.resource, Permission.action).all()
        return AgentResult.ok(f"{len(perms)} دسترسی", [self._ser_perm(p) for p in perms])

    def _assign_permission(self, p: Dict) -> AgentResult:
        role = self._find_role(p.get("role_name", "") or p.get("role_id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        perm_codes: List[str] = p.get("permissions", [])
        added = self._assign_perms_to_role(role.id, perm_codes)
        return AgentResult.ok(f"{added} دسترسی به نقش '{role.name}' اضافه شد")

    def _revoke_permission(self, p: Dict) -> AgentResult:
        role = self._find_role(p.get("role_name", "") or p.get("role_id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        perm_code = p.get("permission", "").strip()
        perm = self.db.query(Permission).filter(Permission.code == perm_code).first()
        if not perm:
            return AgentResult.fail("دسترسی یافت نشد", "NOT_FOUND")
        deleted = (
            self.db.query(RolePermission)
            .filter(RolePermission.role_id == role.id, RolePermission.permission_id == perm.id)
            .delete()
        )
        self.db.commit()
        return AgentResult.ok(f"دسترسی '{perm_code}' از نقش '{role.name}' برداشته شد" if deleted else "دسترسی قبلاً اختصاص نداشت")

    def _assign_role_to_admin(self, p: Dict) -> AgentResult:
        admin_id = p.get("admin_id")
        role = self._find_role(p.get("role_name", "") or p.get("role_id", 0))
        if not role:
            return AgentResult.fail("نقش یافت نشد", "NOT_FOUND")
        admin = self.db.query(Admin).filter(Admin.id == admin_id).first()
        if not admin:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")
        old_role_id = admin.role_id
        admin.role_id = role.id
        self.db.commit()
        return AgentResult.ok(f"نقش ادمین '{admin.username}' به '{role.name}' تغییر کرد", {
            "old_role_id": old_role_id,
            "new_role_id": role.id,
        })

    def _get_admin_permissions(self, p: Dict) -> AgentResult:
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
        return AgentResult.ok("دسترسی‌های ادمین", [p[0] for p in perms])

    def _check_permission(self, p: Dict) -> AgentResult:
        admin_id   = p.get("admin_id")
        permission = p.get("permission", "")
        admin = self.db.query(Admin).filter(Admin.id == admin_id).first()
        if not admin or not admin.is_active:
            return AgentResult.fail("ادمین یافت نشد", "NOT_FOUND")
        role = self.db.query(AdminRole).filter(AdminRole.id == admin.role_id).first()
        if not role or not role.is_active:
            return AgentResult.fail("نقش ادمین فعال نیست", "ROLE_INACTIVE")
        perms = (
            self.db.query(Permission.code)
            .join(RolePermission, RolePermission.permission_id == Permission.id)
            .filter(RolePermission.role_id == admin.role_id)
            .all()
        )
        codes = [p[0] for p in perms]
        has = permission in codes
        return AgentResult.ok("بررسی دسترسی", {"has_permission": has, "permission": permission})

    def _seed_defaults(self, p: Dict) -> AgentResult:
        """seed کردن دسترسی‌ها و نقش‌های پیش‌فرض"""
        # 1) ساخت permission ها
        for code, resource, action, desc in DEFAULT_PERMISSIONS:
            if not self.db.query(Permission).filter(Permission.code == code).first():
                self.db.add(Permission(code=code, resource=resource, action=action, description=desc))
        self.db.commit()

        # 2) ساخت نقش‌ها
        for role_name, role_cfg in DEFAULT_ROLES.items():
            role = self.db.query(AdminRole).filter(AdminRole.name == role_name).first()
            if not role:
                role = AdminRole(
                    name=role_name,
                    display_name=role_cfg["display_name"],
                    description=role_cfg["description"],
                    is_active=True,
                )
                self.db.add(role)
                self.db.commit()

            # 3) assign دسترسی‌ها
            perm_list = role_cfg["permissions"]
            if perm_list == "*":
                all_perms = self.db.query(Permission).all()
                perm_list = [p.code for p in all_perms]
            self._assign_perms_to_role(role.id, perm_list)

        return AgentResult.ok("داده‌های پیش‌فرض RBAC seed شد")

    # ─── helpers ────────────────────────────────────────────

    def _find_role(self, identifier):
        if isinstance(identifier, int) and identifier > 0:
            return self.db.query(AdminRole).filter(AdminRole.id == identifier).first()
        if isinstance(identifier, str) and identifier:
            return self.db.query(AdminRole).filter(AdminRole.name == identifier.upper()).first()
        return None

    def _assign_perms_to_role(self, role_id: int, perm_codes: List[str]) -> int:
        added = 0
        for code in perm_codes:
            perm = self.db.query(Permission).filter(Permission.code == code).first()
            if not perm:
                continue
            exists = self.db.query(RolePermission).filter(
                RolePermission.role_id == role_id,
                RolePermission.permission_id == perm.id,
            ).first()
            if not exists:
                self.db.add(RolePermission(role_id=role_id, permission_id=perm.id))
                added += 1
        self.db.commit()
        return added

    def _ser_role(self, r: AdminRole) -> Dict:
        return {
            "id":           r.id,
            "name":         r.name,
            "display_name": r.display_name,
            "description":  r.description,
            "is_active":    r.is_active,
        }

    def _ser_perm(self, p: Permission) -> Dict:
        return {
            "id":          p.id,
            "code":        p.code,
            "resource":    p.resource,
            "action":      p.action,
            "description": p.description,
        }
