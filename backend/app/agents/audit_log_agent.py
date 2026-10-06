"""
Alpha VPN - Admin RBAC Agent #4: AuditLogAgent
مسئولیت: ثبت و خواندن لاگ کامل فعالیت‌های ادمین‌ها
عملیات: log, list, get, search, stats, purge
"""

import json
from typing import Dict, Any, Optional
from datetime import datetime, timedelta

from sqlalchemy import desc, func

from app.agents.base import BaseAgent, AgentResult
from app.database import AuditLog, Admin


class AuditLogAgent(BaseAgent):
    name = "AuditLogAgent"
    description = "لاگ فعالیت‌های ادمین‌ها — ثبت، جستجو، آمار"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "log":    self._log,
            "list":   self._list,
            "get":    self._get,
            "search": self._search,
            "stats":  self._stats,
            "purge":  self._purge,
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

    def _log(self, p: Dict) -> AgentResult:
        """ثبت یه رویداد جدید"""
        action_str = p.get("action", "").strip()
        if not action_str:
            return AgentResult.fail("action الزامی است", "VALIDATION_ERROR")

        detail = p.get("detail", "")
        if isinstance(detail, dict):
            detail = json.dumps(detail, ensure_ascii=False)

        entry = AuditLog(
            admin_id=p.get("admin_id"),
            admin_name=p.get("admin_name", ""),
            action=action_str,
            resource=p.get("resource", ""),
            target_id=str(p.get("target_id", "")),
            detail=detail,
            ip_address=p.get("ip_address", ""),
            status=p.get("status", "success"),
        )
        self.db.add(entry)
        self.db.commit()
        return AgentResult.ok("لاگ ثبت شد", {"log_id": entry.id})

    def _list(self, p: Dict) -> AgentResult:
        """لیست لاگ‌ها با فیلتر و صفحه‌بندی"""
        page      = int(p.get("page", 1))
        per_page  = min(int(p.get("per_page", 50)), 200)
        admin_id  = p.get("admin_id")
        resource  = p.get("resource")
        status    = p.get("status")
        days      = p.get("days")

        query = self.db.query(AuditLog)

        if admin_id:
            query = query.filter(AuditLog.admin_id == admin_id)
        if resource:
            query = query.filter(AuditLog.resource == resource)
        if status:
            query = query.filter(AuditLog.status == status)
        if days:
            cutoff = datetime.utcnow() - timedelta(days=int(days))
            query = query.filter(AuditLog.created_at >= cutoff)

        total = query.count()
        logs  = (
            query
            .order_by(desc(AuditLog.created_at))
            .offset((page - 1) * per_page)
            .limit(per_page)
            .all()
        )

        return AgentResult.ok(f"{total} لاگ", {
            "total":    total,
            "page":     page,
            "per_page": per_page,
            "pages":    (total + per_page - 1) // per_page,
            "items":    [self._serialize(l) for l in logs],
        })

    def _get(self, p: Dict) -> AgentResult:
        log_id = p.get("log_id")
        if not log_id:
            return AgentResult.fail("log_id الزامی است", "VALIDATION_ERROR")
        entry = self.db.query(AuditLog).filter(AuditLog.id == int(log_id)).first()
        if not entry:
            return AgentResult.fail("لاگ یافت نشد", "NOT_FOUND")
        return AgentResult.ok("لاگ", self._serialize(entry))

    def _search(self, p: Dict) -> AgentResult:
        """جستجو در لاگ‌ها"""
        keyword = p.get("keyword", "").strip()
        if not keyword:
            return AgentResult.fail("keyword الزامی است", "VALIDATION_ERROR")

        per_page = min(int(p.get("per_page", 50)), 200)

        logs = (
            self.db.query(AuditLog)
            .filter(
                AuditLog.detail.contains(keyword) |
                AuditLog.action.contains(keyword) |
                AuditLog.target_id.contains(keyword) |
                AuditLog.admin_name.contains(keyword)
            )
            .order_by(desc(AuditLog.created_at))
            .limit(per_page)
            .all()
        )
        return AgentResult.ok(f"{len(logs)} نتیجه", [self._serialize(l) for l in logs])

    def _stats(self, p: Dict) -> AgentResult:
        """آمار فعالیت‌های ادمین‌ها"""
        days = int(p.get("days", 30))
        cutoff = datetime.utcnow() - timedelta(days=days)

        total       = self.db.query(AuditLog).filter(AuditLog.created_at >= cutoff).count()
        failed      = self.db.query(AuditLog).filter(AuditLog.created_at >= cutoff, AuditLog.status == "failed").count()
        by_resource = (
            self.db.query(AuditLog.resource, func.count(AuditLog.id))
            .filter(AuditLog.created_at >= cutoff)
            .group_by(AuditLog.resource)
            .all()
        )
        by_admin = (
            self.db.query(AuditLog.admin_name, func.count(AuditLog.id))
            .filter(AuditLog.created_at >= cutoff)
            .group_by(AuditLog.admin_name)
            .order_by(func.count(AuditLog.id).desc())
            .limit(10)
            .all()
        )
        by_action = (
            self.db.query(AuditLog.action, func.count(AuditLog.id))
            .filter(AuditLog.created_at >= cutoff)
            .group_by(AuditLog.action)
            .order_by(func.count(AuditLog.id).desc())
            .limit(10)
            .all()
        )

        return AgentResult.ok(f"آمار {days} روز اخیر", {
            "total":       total,
            "failed":      failed,
            "success":     total - failed,
            "by_resource": {r: c for r, c in by_resource},
            "by_admin":    {a: c for a, c in by_admin},
            "by_action":   {a: c for a, c in by_action},
        })

    def _purge(self, p: Dict) -> AgentResult:
        """حذف لاگ‌های قدیمی (فقط SUPER_ADMIN)"""
        older_than_days = int(p.get("older_than_days", 90))
        if older_than_days < 30:
            return AgentResult.fail("حداقل ۳۰ روز باید گذشته باشد", "VALIDATION_ERROR")

        cutoff  = datetime.utcnow() - timedelta(days=older_than_days)
        deleted = self.db.query(AuditLog).filter(AuditLog.created_at < cutoff).delete()
        self.db.commit()

        # لاگ خود این عملیات
        self.db.add(AuditLog(
            admin_id=p.get("admin_id"),
            admin_name=p.get("admin_name", ""),
            action="audit.purge",
            resource="audit",
            target_id="",
            detail=f"purged {deleted} logs older than {older_than_days} days",
            ip_address=p.get("ip_address", ""),
            status="success",
        ))
        self.db.commit()
        return AgentResult.ok(f"{deleted} لاگ قدیمی حذف شد")

    def _serialize(self, l: AuditLog) -> Dict:
        return {
            "id":          l.id,
            "admin_id":    l.admin_id,
            "admin_name":  l.admin_name,
            "action":      l.action,
            "resource":    l.resource,
            "target_id":   l.target_id,
            "detail":      l.detail,
            "ip_address":  l.ip_address,
            "status":      l.status,
            "created_at":  l.created_at.isoformat() if l.created_at else "",
        }
