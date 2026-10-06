"""
Alpha VPN - Agent #1: UserManagementAgent
مسئولیت: مدیریت کامل کاربران
عملیات: create, update, delete, ban, unban, reset_password,
         extend_plan, reset_devices, bulk_create
"""

from typing import Dict, List
from datetime import datetime, timedelta

from app.agents.base import BaseAgent, AgentResult
from app.database import User
from app.auth import hash_password


class UserManagementAgent(BaseAgent):
    name = "UserManagementAgent"
    description = "مدیریت کامل کاربران VPN — ایجاد، ویرایش، تعلیق، تمدید اشتراک"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action} payload_keys={list(payload.keys())}")
        dispatch = {
            "create":         self._create_user,
            "update":         self._update_user,
            "delete":         self._delete_user,
            "ban":            self._ban_user,
            "unban":          self._unban_user,
            "reset_password": self._reset_password,
            "extend_plan":    self._extend_plan,
            "reset_devices":  self._reset_devices,
            "bulk_create":    self._bulk_create,
            "get":            self._get_user,
            "list":           self._list_users,
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

    # ─────────────────── Handlers ───────────────────────────

    def _create_user(self, p: Dict) -> AgentResult:
        username = p.get("username", "").strip()
        password = p.get("password", "").strip()
        if not username or not password:
            return AgentResult.fail("نام کاربری و رمز عبور الزامی است", "VALIDATION_ERROR")
        if len(password) < 4:
            return AgentResult.fail("رمز عبور باید حداقل ۴ کاراکتر باشد", "VALIDATION_ERROR")

        if self.db.query(User).filter(User.username == username).first():
            return AgentResult.fail(f"کاربر '{username}' از قبل وجود دارد", "DUPLICATE")

        quota = float(p.get("total_quota_gb", 10.0))
        days  = int(p.get("remaining_days",  30))
        user  = User(
            username=username,
            password_hash=hash_password(password),
            plan_type=p.get("plan_type", "MULTI_USER"),
            status="ACTIVE",
            remaining_gb=quota,
            used_gb=0.0,
            total_quota_gb=quota,
            remaining_days=days,
            total_days=days,
            expiry_date=p.get("expiry_date", self._calc_expiry(days)),
            max_devices=int(p.get("max_devices", 2)),
            active_devices=0,
        )
        self.db.add(user)
        self.db.commit()
        self.log(f"کاربر {username} ساخته شد")
        return AgentResult.ok(f"کاربر '{username}' با موفقیت ساخته شد", self._serialize(user))

    def _update_user(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")

        fields = ["plan_type", "status", "remaining_gb", "used_gb",
                  "total_quota_gb", "remaining_days", "max_devices", "expiry_date"]
        for f in fields:
            if f in p and p[f] is not None:
                setattr(user, f, p[f])

        self.db.commit()
        return AgentResult.ok(f"کاربر '{user.username}' به‌روز شد", self._serialize(user))

    def _delete_user(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        username = user.username
        self.db.delete(user)
        self.db.commit()
        return AgentResult.ok(f"کاربر '{username}' حذف شد")

    def _ban_user(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        if user.status == "BANNED":
            return AgentResult.fail("کاربر از قبل مسدود است", "ALREADY_BANNED")
        user.status = "BANNED"
        self.db.commit()
        return AgentResult.ok(f"کاربر '{user.username}' مسدود شد")

    def _unban_user(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        user.status = "ACTIVE"
        self.db.commit()
        return AgentResult.ok(f"کاربر '{user.username}' رفع مسدودیت شد")

    def _reset_password(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        new_pass = p.get("new_password", "").strip()
        if len(new_pass) < 4:
            return AgentResult.fail("رمز جدید باید حداقل ۴ کاراکتر باشد", "VALIDATION_ERROR")
        user.password_hash = hash_password(new_pass)
        self.db.commit()
        return AgentResult.ok(f"رمز عبور '{user.username}' تغییر کرد")

    def _extend_plan(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")

        add_gb   = float(p.get("add_gb",   0))
        add_days = int(p.get("add_days",   0))

        if add_gb > 0:
            user.remaining_gb   += add_gb
            user.total_quota_gb += add_gb
        if add_days > 0:
            user.remaining_days += add_days
            user.expiry_date = self._calc_expiry(user.remaining_days)

        # اگه EXPIRED بود و حالا حجم/روز گرفت، ACTIVE کن
        if user.status == "EXPIRED" and (user.remaining_gb > 0 or user.remaining_days > 0):
            user.status = "ACTIVE"

        self.db.commit()
        return AgentResult.ok(
            f"اشتراک '{user.username}' تمدید شد (+{add_gb}GB +{add_days}days)",
            self._serialize(user)
        )

    def _reset_devices(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        user.active_devices = 0
        self.db.commit()
        return AgentResult.ok(f"دستگاه‌های فعال '{user.username}' ریست شد")

    def _bulk_create(self, p: Dict) -> AgentResult:
        users_data: List[Dict] = p.get("users", [])
        if not users_data:
            return AgentResult.fail("لیست کاربران خالی است", "VALIDATION_ERROR")

        created, skipped = [], []
        for ud in users_data:
            username = ud.get("username", "").strip()
            if not username or self.db.query(User).filter(User.username == username).first():
                skipped.append(username)
                continue
            quota = float(ud.get("total_quota_gb", 10.0))
            days  = int(ud.get("remaining_days", 30))
            user  = User(
                username=username,
                password_hash=hash_password(ud.get("password", "changeme")),
                plan_type=ud.get("plan_type", "MULTI_USER"),
                status="ACTIVE",
                remaining_gb=quota,
                used_gb=0.0,
                total_quota_gb=quota,
                remaining_days=days,
                total_days=days,
                expiry_date=self._calc_expiry(days),
                max_devices=int(ud.get("max_devices", 2)),
                active_devices=0,
            )
            self.db.add(user)
            created.append(username)

        self.db.commit()
        return AgentResult.ok(
            f"{len(created)} کاربر ساخته شد، {len(skipped)} رد شد",
            {"created": created, "skipped": skipped}
        )

    def _get_user(self, p: Dict) -> AgentResult:
        user = self._find(p.get("username", ""))
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")
        return AgentResult.ok("اطلاعات کاربر", self._serialize(user))

    def _list_users(self, p: Dict) -> AgentResult:
        status_filter = p.get("status")
        query = self.db.query(User)
        if status_filter:
            query = query.filter(User.status == status_filter)
        users = query.order_by(User.created_at.desc()).all()
        return AgentResult.ok(f"{len(users)} کاربر یافت شد", [self._serialize(u) for u in users])

    # ─────────────────── Helpers ────────────────────────────

    def _find(self, username: str):
        return self.db.query(User).filter(User.username == username.strip()).first()

    def _calc_expiry(self, days: int) -> str:
        return (datetime.utcnow() + timedelta(days=days)).strftime("%Y-%m-%d")

    def _serialize(self, u: User) -> Dict:
        return {
            "username":       u.username,
            "plan_type":      u.plan_type,
            "status":         u.status,
            "remaining_gb":   u.remaining_gb,
            "used_gb":        u.used_gb,
            "total_quota_gb": u.total_quota_gb,
            "remaining_days": u.remaining_days,
            "total_days":     u.total_days,
            "expiry_date":    u.expiry_date,
            "max_devices":    u.max_devices,
            "active_devices": u.active_devices,
        }
