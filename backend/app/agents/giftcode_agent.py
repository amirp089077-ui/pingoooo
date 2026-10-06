"""
Alpha VPN - Agent #3: GiftCodeAgent
مسئولیت: مدیریت کامل کدهای هدیه
عملیات: create, delete, redeem, revoke_redemption,
         list, get, stats, bulk_create, deactivate
"""

import random
import string
from typing import Dict, List
from datetime import datetime

from app.agents.base import BaseAgent, AgentResult
from app.database import GiftCode, GiftCodeRedemption, User


class GiftCodeAgent(BaseAgent):
    name = "GiftCodeAgent"
    description = "مدیریت کدهای هدیه — ایجاد، استفاده، لغو، آمار"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action} payload_keys={list(payload.keys())}")
        dispatch = {
            "create":            self._create_code,
            "delete":            self._delete_code,
            "redeem":            self._redeem_code,
            "revoke_redemption": self._revoke_redemption,
            "list":              self._list_codes,
            "get":               self._get_code,
            "stats":             self._stats,
            "bulk_create":       self._bulk_create,
            "deactivate":        self._deactivate,
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

    def _create_code(self, p: Dict) -> AgentResult:
        code = p.get("code", "").strip().upper()
        if not code:
            code = self._generate_code()

        if self.db.query(GiftCode).filter(GiftCode.code == code).first():
            return AgentResult.fail(f"کد '{code}' از قبل وجود دارد", "DUPLICATE")

        add_gb   = float(p.get("add_gb",   0))
        add_days = int(p.get("add_days",   0))
        if add_gb <= 0 and add_days <= 0:
            return AgentResult.fail("حداقل یکی از add_gb یا add_days باید مثبت باشد", "VALIDATION_ERROR")

        gc = GiftCode(
            code=code,
            add_gb=add_gb,
            add_days=add_days,
            max_uses=int(p.get("max_uses", 1)),
            used_count=0,
            is_active=True,
            description=p.get("description", ""),
            expires_at=p.get("expires_at"),
        )
        self.db.add(gc)
        self.db.commit()
        self.log(f"کد هدیه {code} ساخته شد")
        return AgentResult.ok(f"کد هدیه '{code}' ساخته شد", self._serialize(gc))

    def _delete_code(self, p: Dict) -> AgentResult:
        gc = self._find(p.get("code", ""))
        if not gc:
            return AgentResult.fail("کد هدیه یافت نشد", "NOT_FOUND")

        # اول redemption ها رو حذف کن
        self.db.query(GiftCodeRedemption).filter(
            GiftCodeRedemption.gift_code == gc.code
        ).delete()
        self.db.delete(gc)
        self.db.commit()
        return AgentResult.ok(f"کد هدیه '{gc.code}' و سوابق آن حذف شد")

    def _redeem_code(self, p: Dict) -> AgentResult:
        """یوزر یه کد رو استفاده می‌کنه"""
        username = p.get("username", "").strip()
        code_str = p.get("code", "").strip().upper()

        user = self.db.query(User).filter(User.username == username).first()
        if not user:
            return AgentResult.fail("کاربر یافت نشد", "NOT_FOUND")

        gc = self._find(code_str)
        if not gc:
            return AgentResult.fail("کد هدیه نامعتبر است", "INVALID_CODE")

        if not gc.is_active:
            return AgentResult.fail("این کد هدیه غیرفعال شده است", "CODE_INACTIVE")

        if gc.max_uses > 0 and gc.used_count >= gc.max_uses:
            return AgentResult.fail("ظرفیت این کد هدیه تمام شده است", "CODE_EXHAUSTED")

        if gc.expires_at and datetime.utcnow() > datetime.fromisoformat(gc.expires_at):
            return AgentResult.fail("کد هدیه منقضی شده است", "CODE_EXPIRED")

        already = self.db.query(GiftCodeRedemption).filter(
            GiftCodeRedemption.gift_code == gc.code,
            GiftCodeRedemption.username == username
        ).first()
        if already:
            return AgentResult.fail("این کد قبلاً توسط شما استفاده شده است", "ALREADY_REDEEMED")

        # اعمال هدیه روی کاربر
        if gc.add_gb > 0:
            user.remaining_gb   += gc.add_gb
            user.total_quota_gb += gc.add_gb
        if gc.add_days > 0:
            user.remaining_days += gc.add_days

        if user.status == "EXPIRED":
            user.status = "ACTIVE"

        gc.used_count += 1

        redemption = GiftCodeRedemption(
            gift_code=gc.code,
            username=username,
            redeemed_at=datetime.utcnow().isoformat(),
        )
        self.db.add(redemption)
        self.db.commit()

        return AgentResult.ok(
            f"کد هدیه با موفقیت اعمال شد (+{gc.add_gb}GB +{gc.add_days}days)",
            {"add_gb": gc.add_gb, "add_days": gc.add_days}
        )

    def _revoke_redemption(self, p: Dict) -> AgentResult:
        """لغو استفاده یه یوزر از یه کد و برگرداندن هدیه"""
        username = p.get("username", "").strip()
        code_str = p.get("code", "").strip().upper()

        redemption = self.db.query(GiftCodeRedemption).filter(
            GiftCodeRedemption.gift_code == code_str,
            GiftCodeRedemption.username == username
        ).first()
        if not redemption:
            return AgentResult.fail("سابقه استفاده یافت نشد", "NOT_FOUND")

        gc   = self._find(code_str)
        user = self.db.query(User).filter(User.username == username).first()

        if gc and user:
            if gc.add_gb > 0:
                user.remaining_gb   = max(0, user.remaining_gb   - gc.add_gb)
                user.total_quota_gb = max(0, user.total_quota_gb - gc.add_gb)
            if gc.add_days > 0:
                user.remaining_days = max(0, user.remaining_days - gc.add_days)
            gc.used_count = max(0, gc.used_count - 1)

        self.db.delete(redemption)
        self.db.commit()
        return AgentResult.ok(f"استفاده '{username}' از کد '{code_str}' لغو و هدیه بازگردانده شد")

    def _list_codes(self, p: Dict) -> AgentResult:
        active_only = p.get("active_only", False)
        query = self.db.query(GiftCode)
        if active_only:
            query = query.filter(GiftCode.is_active == True)
        codes = query.order_by(GiftCode.id.desc()).all()
        return AgentResult.ok(f"{len(codes)} کد هدیه", [self._serialize(c) for c in codes])

    def _get_code(self, p: Dict) -> AgentResult:
        gc = self._find(p.get("code", ""))
        if not gc:
            return AgentResult.fail("کد هدیه یافت نشد", "NOT_FOUND")

        redemptions = self.db.query(GiftCodeRedemption).filter(
            GiftCodeRedemption.gift_code == gc.code
        ).all()
        data = self._serialize(gc)
        data["redemptions"] = [
            {"username": r.username, "redeemed_at": r.redeemed_at}
            for r in redemptions
        ]
        return AgentResult.ok("اطلاعات کد هدیه", data)

    def _stats(self, p: Dict) -> AgentResult:
        total_codes  = self.db.query(GiftCode).count()
        active_codes = self.db.query(GiftCode).filter(GiftCode.is_active == True).count()
        total_redeem = self.db.query(GiftCodeRedemption).count()

        total_gb_given   = sum(
            r.gift_code and self._find(r.gift_code) and self._find(r.gift_code).add_gb or 0
            for r in self.db.query(GiftCodeRedemption).all()
        )
        total_days_given = sum(
            r.gift_code and self._find(r.gift_code) and self._find(r.gift_code).add_days or 0
            for r in self.db.query(GiftCodeRedemption).all()
        )

        return AgentResult.ok("آمار کدهای هدیه", {
            "total_codes":    total_codes,
            "active_codes":   active_codes,
            "inactive_codes": total_codes - active_codes,
            "total_redeemed": total_redeem,
            "total_gb_given": total_gb_given,
            "total_days_given": total_days_given,
        })

    def _bulk_create(self, p: Dict) -> AgentResult:
        count    = int(p.get("count", 1))
        add_gb   = float(p.get("add_gb",   0))
        add_days = int(p.get("add_days",   0))
        max_uses = int(p.get("max_uses",   1))
        prefix   = p.get("prefix", "ALPHA").upper()

        if add_gb <= 0 and add_days <= 0:
            return AgentResult.fail("حداقل یکی از add_gb یا add_days باید مثبت باشد", "VALIDATION_ERROR")
        if count < 1 or count > 500:
            return AgentResult.fail("تعداد باید بین ۱ تا ۵۰۰ باشد", "VALIDATION_ERROR")

        created = []
        attempts = 0
        while len(created) < count and attempts < count * 5:
            attempts += 1
            code = f"{prefix}-{self._generate_code(6)}"
            if self.db.query(GiftCode).filter(GiftCode.code == code).first():
                continue
            self.db.add(GiftCode(
                code=code,
                add_gb=add_gb,
                add_days=add_days,
                max_uses=max_uses,
                used_count=0,
                is_active=True,
                description=p.get("description", ""),
                expires_at=p.get("expires_at"),
            ))
            created.append(code)

        self.db.commit()
        return AgentResult.ok(f"{len(created)} کد هدیه ساخته شد", {"codes": created})

    def _deactivate(self, p: Dict) -> AgentResult:
        gc = self._find(p.get("code", ""))
        if not gc:
            return AgentResult.fail("کد هدیه یافت نشد", "NOT_FOUND")
        gc.is_active = False
        self.db.commit()
        return AgentResult.ok(f"کد هدیه '{gc.code}' غیرفعال شد")

    # ─────────────────── Helpers ────────────────────────────

    def _find(self, code: str):
        return self.db.query(GiftCode).filter(GiftCode.code == code.strip().upper()).first()

    def _generate_code(self, length: int = 8) -> str:
        chars = string.ascii_uppercase + string.digits
        return "".join(random.choices(chars, k=length))

    def _serialize(self, gc: GiftCode) -> Dict:
        return {
            "code":        gc.code,
            "add_gb":      gc.add_gb,
            "add_days":    gc.add_days,
            "max_uses":    gc.max_uses,
            "used_count":  gc.used_count,
            "is_active":   gc.is_active,
            "description": gc.description,
            "expires_at":  gc.expires_at,
        }
