"""
Alpha VPN - Admin RBAC Agent #6: SessionAgent
مسئولیت: مدیریت session های فعال ادمین‌ها
عملیات: list_active, list_all, revoke, revoke_all, cleanup, stats
"""

from typing import Dict
from datetime import datetime, timedelta

from app.agents.base import BaseAgent, AgentResult
from app.database import AdminSession, Admin, AuditLog


class SessionAgent(BaseAgent):
    name = "SessionAgent"
    description = "مدیریت session های ادمین‌ها — مشاهده، باطل کردن، پاکسازی"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "list_active": self._list_active,
            "list_all":    self._list_all,
            "revoke":      self._revoke,
            "revoke_all":  self._revoke_all,
            "cleanup":     self._cleanup,
            "stats":       self._stats,
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

    def _list_active(self, p: Dict) -> AgentResult:
        admin_id = p.get("admin_id")
        now      = datetime.utcnow()
        query    = self.db.query(AdminSession).filter(
            AdminSession.is_revoked == False,
            AdminSession.expires_at > now,
        )
        if admin_id:
            query = query.filter(AdminSession.admin_id == admin_id)
        sessions = query.order_by(AdminSession.created_at.desc()).all()
        return AgentResult.ok(f"{len(sessions)} session فعال", [self._serialize(s) for s in sessions])

    def _list_all(self, p: Dict) -> AgentResult:
        admin_id = p.get("admin_id")
        page     = int(p.get("page", 1))
        per_page = min(int(p.get("per_page", 50)), 200)

        query = self.db.query(AdminSession)
        if admin_id:
            query = query.filter(AdminSession.admin_id == admin_id)

        total    = query.count()
        sessions = (
            query
            .order_by(AdminSession.created_at.desc())
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )
        return AgentResult.ok(f"{total} session", {
            "total": total,
            "page":  page,
            "items": [self._serialize(s) for s in sessions],
        })

    def _revoke(self, p: Dict) -> AgentResult:
        session_id = p.get("session_id")
        if not session_id:
            return AgentResult.fail("session_id الزامی است", "VALIDATION_ERROR")

        session = self.db.query(AdminSession).filter(AdminSession.id == int(session_id)).first()
        if not session:
            return AgentResult.fail("session یافت نشد", "NOT_FOUND")
        if session.is_revoked:
            return AgentResult.fail("session از قبل باطل شده", "ALREADY_REVOKED")

        session.is_revoked = True
        self.db.add(AuditLog(
            admin_id=p.get("revoked_by"),
            admin_name=p.get("admin_name", ""),
            action="session.revoke",
            resource="sessions",
            target_id=str(session_id),
            detail=f"revoked session of admin_id={session.admin_id}",
            ip_address=p.get("ip_address", ""),
            status="success",
        ))
        self.db.commit()
        return AgentResult.ok(f"session #{session_id} باطل شد")

    def _revoke_all(self, p: Dict) -> AgentResult:
        admin_id = p.get("admin_id")
        if not admin_id:
            return AgentResult.fail("admin_id الزامی است", "VALIDATION_ERROR")

        count = (
            self.db.query(AdminSession)
            .filter(
                AdminSession.admin_id == admin_id,
                AdminSession.is_revoked == False,
            )
            .update({"is_revoked": True})
        )
        self.db.add(AuditLog(
            admin_id=p.get("revoked_by"),
            admin_name=p.get("admin_name", ""),
            action="session.revoke_all",
            resource="sessions",
            target_id=str(admin_id),
            detail=f"revoked {count} sessions for admin_id={admin_id}",
            ip_address=p.get("ip_address", ""),
            status="success",
        ))
        self.db.commit()
        return AgentResult.ok(f"{count} session ادمین #{admin_id} باطل شد")

    def _cleanup(self, p: Dict) -> AgentResult:
        days_old = int(p.get("days_old", 7))
        cutoff   = datetime.utcnow() - timedelta(days=days_old)
        deleted  = (
            self.db.query(AdminSession)
            .filter(AdminSession.expires_at < cutoff)
            .delete()
        )
        self.db.commit()
        return AgentResult.ok(f"{deleted} session قدیمی حذف شد")

    def _stats(self, p: Dict) -> AgentResult:
        now = datetime.utcnow()

        active  = self.db.query(AdminSession).filter(
            AdminSession.is_revoked == False,
            AdminSession.expires_at > now,
        ).count()
        revoked = self.db.query(AdminSession).filter(AdminSession.is_revoked == True).count()
        expired = self.db.query(AdminSession).filter(
            AdminSession.is_revoked == False,
            AdminSession.expires_at <= now,
        ).count()
        total   = self.db.query(AdminSession).count()
        online_admins = (
            self.db.query(AdminSession.admin_id)
            .filter(AdminSession.is_revoked == False, AdminSession.expires_at > now)
            .distinct().count()
        )
        return AgentResult.ok("آمار session ها", {
            "total":         total,
            "active":        active,
            "revoked":       revoked,
            "expired":       expired,
            "online_admins": online_admins,
        })

    def _serialize(self, s: AdminSession) -> Dict:
        admin = self.db.query(Admin).filter(Admin.id == s.admin_id).first()
        now   = datetime.utcnow()
        return {
            "id":         s.id,
            "admin_id":   s.admin_id,
            "admin_name": admin.username if admin else "",
            "ip_address": s.ip_address,
            "user_agent": s.user_agent,
            "created_at": s.created_at.isoformat() if s.created_at else "",
            "expires_at": s.expires_at.isoformat() if s.expires_at else "",
            "is_revoked": s.is_revoked,
            "is_active":  not s.is_revoked and s.expires_at > now,
        }
