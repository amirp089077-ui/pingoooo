"""
Alpha VPN - Agent #4: AnalyticsAgent
مسئولیت: آمار و گزارش‌گیری کامل سیستم
عملیات: dashboard, user_stats, server_stats, traffic_report,
         expiring_users, top_users, daily_signups, giftcode_report
"""

from typing import Dict, List
from datetime import datetime, timedelta

from sqlalchemy import func

from app.agents.base import BaseAgent, AgentResult
from app.database import User, Server, GiftCode, GiftCodeRedemption


class AnalyticsAgent(BaseAgent):
    name = "AnalyticsAgent"
    description = "آمار و گزارش‌گیری — داشبورد، ترافیک، کاربران در حال انقضا، گزارش کدهای هدیه"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "dashboard":       self._dashboard,
            "user_stats":      self._user_stats,
            "server_stats":    self._server_stats,
            "traffic_report":  self._traffic_report,
            "expiring_users":  self._expiring_users,
            "top_users":       self._top_users,
            "daily_signups":   self._daily_signups,
            "giftcode_report": self._giftcode_report,
        }
        handler = dispatch.get(action)
        if not handler:
            return AgentResult.fail(f"عملیات '{action}' پشتیبانی نمی‌شود", "UNKNOWN_ACTION")
        try:
            return handler(payload)
        except Exception as exc:
            self.log(f"خطا در {action}: {exc}", "error")
            return AgentResult.fail(f"خطای داخلی: {exc}", "INTERNAL_ERROR")

    # ─────────────────── Handlers ───────────────────────────

    def _dashboard(self, p: Dict) -> AgentResult:
        """داشبورد اصلی ادمین — یه نگاه کلی به همه چیز"""
        now = datetime.utcnow()

        # ─── کاربران ───
        total_users   = self.db.query(User).count()
        active_users  = self.db.query(User).filter(User.status == "ACTIVE").count()
        banned_users  = self.db.query(User).filter(User.status == "BANNED").count()
        expired_users = self.db.query(User).filter(User.status == "EXPIRED").count()

        # کاربران که ظرف ۷ روز آینده منقضی می‌شن
        expiry_threshold = (now + timedelta(days=7)).strftime("%Y-%m-%d")
        expiring_soon = self.db.query(User).filter(
            User.status == "ACTIVE",
            User.expiry_date <= expiry_threshold,
        ).count()

        # ─── ترافیک ───
        total_used_gb = self.db.query(func.sum(User.used_gb)).scalar() or 0.0
        total_quota   = self.db.query(func.sum(User.total_quota_gb)).scalar() or 0.0

        # ─── سرورها ───
        total_servers  = self.db.query(Server).count()
        active_servers = self.db.query(Server).filter(Server.is_active == True).count()

        # ─── کدهای هدیه ───
        total_codes      = self.db.query(GiftCode).count()
        active_codes     = self.db.query(GiftCode).filter(GiftCode.is_active == True).count()
        total_redemptions = self.db.query(GiftCodeRedemption).count()

        return AgentResult.ok("داشبورد ادمین", {
            "users": {
                "total":         total_users,
                "active":        active_users,
                "banned":        banned_users,
                "expired":       expired_users,
                "expiring_soon": expiring_soon,
            },
            "traffic": {
                "total_used_gb":   round(total_used_gb, 2),
                "total_quota_gb":  round(total_quota, 2),
                "usage_percent":   round((total_used_gb / total_quota * 100) if total_quota > 0 else 0, 1),
            },
            "servers": {
                "total":    total_servers,
                "active":   active_servers,
                "inactive": total_servers - active_servers,
            },
            "gift_codes": {
                "total":       total_codes,
                "active":      active_codes,
                "redemptions": total_redemptions,
            },
            "generated_at": now.isoformat(),
        })

    def _user_stats(self, p: Dict) -> AgentResult:
        """آمار تفکیکی کاربران"""
        by_status = (
            self.db.query(User.status, func.count(User.username))
            .group_by(User.status)
            .all()
        )
        by_plan = (
            self.db.query(User.plan_type, func.count(User.username))
            .group_by(User.plan_type)
            .all()
        )

        avg_remaining_gb = self.db.query(func.avg(User.remaining_gb)).scalar() or 0
        avg_remaining_days = self.db.query(func.avg(User.remaining_days)).scalar() or 0
        zero_gb_users = self.db.query(User).filter(User.remaining_gb <= 0).count()

        return AgentResult.ok("آمار کاربران", {
            "by_status":          {s: c for s, c in by_status},
            "by_plan":            {pl: c for pl, c in by_plan},
            "avg_remaining_gb":   round(avg_remaining_gb, 2),
            "avg_remaining_days": round(avg_remaining_days, 1),
            "zero_gb_users":      zero_gb_users,
        })

    def _server_stats(self, p: Dict) -> AgentResult:
        """آمار سرورها"""
        servers = self.db.query(Server).order_by(Server.order_idx).all()

        by_country = {}
        for s in servers:
            by_country[s.country] = by_country.get(s.country, 0) + 1

        pro_count  = sum(1 for s in servers if s.is_pro)
        free_count = sum(1 for s in servers if not s.is_pro)
        avg_ping   = round(
            sum(s.ping for s in servers) / len(servers), 1
        ) if servers else 0

        # سرور با بهترین ping
        best_server = min(servers, key=lambda s: s.ping) if servers else None

        return AgentResult.ok("آمار سرورها", {
            "total":       len(servers),
            "active":      sum(1 for s in servers if s.is_active),
            "inactive":    sum(1 for s in servers if not s.is_active),
            "pro_servers": pro_count,
            "free_servers":free_count,
            "avg_ping_ms": avg_ping,
            "by_country":  by_country,
            "best_ping_server": {
                "id":   best_server.id,
                "city": best_server.city,
                "ping": best_server.ping,
            } if best_server else None,
        })

    def _traffic_report(self, p: Dict) -> AgentResult:
        """گزارش مصرف ترافیک کلی"""
        users = self.db.query(User).all()
        if not users:
            return AgentResult.ok("گزارش ترافیک", {"total_users": 0})

        total_used  = sum(u.used_gb for u in users)
        total_quota = sum(u.total_quota_gb for u in users)
        total_left  = sum(u.remaining_gb for u in users)

        # بازه‌های مصرف
        buckets = {
            "0_to_1gb":   sum(1 for u in users if u.used_gb < 1),
            "1_to_5gb":   sum(1 for u in users if 1 <= u.used_gb < 5),
            "5_to_20gb":  sum(1 for u in users if 5 <= u.used_gb < 20),
            "20gb_plus":  sum(1 for u in users if u.used_gb >= 20),
        }

        return AgentResult.ok("گزارش ترافیک", {
            "total_users":     len(users),
            "total_used_gb":   round(total_used, 2),
            "total_quota_gb":  round(total_quota, 2),
            "total_left_gb":   round(total_left, 2),
            "overall_usage_%": round((total_used / total_quota * 100) if total_quota > 0 else 0, 1),
            "usage_buckets":   buckets,
        })

    def _expiring_users(self, p: Dict) -> AgentResult:
        """کاربرانی که به‌زودی منقضی می‌شن"""
        days = int(p.get("days", 7))
        threshold = (datetime.utcnow() + timedelta(days=days)).strftime("%Y-%m-%d")

        users = (
            self.db.query(User)
            .filter(
                User.status == "ACTIVE",
                User.expiry_date <= threshold,
            )
            .order_by(User.expiry_date.asc())
            .all()
        )

        return AgentResult.ok(
            f"{len(users)} کاربر در {days} روز آینده منقضی می‌شوند",
            [
                {
                    "username":       u.username,
                    "expiry_date":    u.expiry_date,
                    "remaining_days": u.remaining_days,
                    "remaining_gb":   u.remaining_gb,
                }
                for u in users
            ]
        )

    def _top_users(self, p: Dict) -> AgentResult:
        """کاربران با بیشترین مصرف"""
        limit = int(p.get("limit", 10))
        users = (
            self.db.query(User)
            .order_by(User.used_gb.desc())
            .limit(limit)
            .all()
        )
        return AgentResult.ok(
            f"top {limit} کاربر پرمصرف",
            [
                {
                    "rank":          i + 1,
                    "username":      u.username,
                    "used_gb":       round(u.used_gb, 2),
                    "total_quota":   u.total_quota_gb,
                    "usage_percent": round((u.used_gb / u.total_quota_gb * 100) if u.total_quota_gb > 0 else 0, 1),
                    "status":        u.status,
                }
                for i, u in enumerate(users)
            ]
        )

    def _daily_signups(self, p: Dict) -> AgentResult:
        """
        آمار ثبت‌نام روزانه.
        نیاز به فیلد created_at در مدل User دارد.
        اگه ستون created_at نداشت، نتیجه خالی برمی‌گرده.
        """
        days = int(p.get("days", 30))
        try:
            cutoff = datetime.utcnow() - timedelta(days=days)
            rows = (
                self.db.query(
                    func.date(User.created_at).label("day"),
                    func.count(User.username).label("count")
                )
                .filter(User.created_at >= cutoff)
                .group_by(func.date(User.created_at))
                .order_by(func.date(User.created_at).asc())
                .all()
            )
            data = [{"date": str(r.day), "signups": r.count} for r in rows]
            return AgentResult.ok(f"ثبت‌نام‌های {days} روز اخیر", data)
        except Exception:
            return AgentResult.ok("آمار ثبت‌نام روزانه در دسترس نیست (created_at موجود نیست)", [])

    def _giftcode_report(self, p: Dict) -> AgentResult:
        """گزارش کامل کدهای هدیه"""
        codes = self.db.query(GiftCode).all()
        redemptions = self.db.query(GiftCodeRedemption).all()

        total_gb_given   = 0.0
        total_days_given = 0
        code_map = {c.code: c for c in codes}

        for r in redemptions:
            gc = code_map.get(r.gift_code)
            if gc:
                total_gb_given   += gc.add_gb
                total_days_given += gc.add_days

        exhausted = [c for c in codes if c.max_uses > 0 and c.used_count >= c.max_uses]
        active    = [c for c in codes if c.is_active and (c.max_uses == 0 or c.used_count < c.max_uses)]

        return AgentResult.ok("گزارش کدهای هدیه", {
            "total_codes":       len(codes),
            "active_codes":      len(active),
            "exhausted_codes":   len(exhausted),
            "total_redemptions": len(redemptions),
            "total_gb_given":    round(total_gb_given, 2),
            "total_days_given":  total_days_given,
        })
