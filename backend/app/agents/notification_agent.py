"""
Alpha VPN - Agent #5: NotificationAgent
مسئولیت: مدیریت اعلان‌ها و پیام‌های سیستمی
عملیات: send_telegram, broadcast, set_app_config,
         get_app_config, set_maintenance, set_update_notice,
         list_announcements, add_announcement, delete_announcement
"""

import httpx
from typing import Dict, List
from datetime import datetime

from app.agents.base import BaseAgent, AgentResult
from app.database import AppConfig, Announcement


class NotificationAgent(BaseAgent):
    name = "NotificationAgent"
    description = "مدیریت اعلان‌ها — پیام تلگرام، اطلاعیه درون‌اپ، حالت تعمیر، اعلان آپدیت"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action}")
        dispatch = {
            "send_telegram":       self._send_telegram,
            "broadcast":           self._broadcast,
            "set_app_config":      self._set_app_config,
            "get_app_config":      self._get_app_config,
            "set_maintenance":     self._set_maintenance,
            "set_update_notice":   self._set_update_notice,
            "list_announcements":  self._list_announcements,
            "add_announcement":    self._add_announcement,
            "delete_announcement": self._delete_announcement,
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

    def _send_telegram(self, p: Dict) -> AgentResult:
        """ارسال پیام مستقیم به یه chat_id از طریق Bot API"""
        bot_token = self._cfg("telegram_bot_token")
        chat_id   = p.get("chat_id") or self._cfg("telegram_admin_chat_id")
        text      = p.get("text", "").strip()

        if not bot_token:
            return AgentResult.fail("telegram_bot_token در تنظیمات تنظیم نشده", "CONFIG_MISSING")
        if not chat_id:
            return AgentResult.fail("chat_id مشخص نشده", "VALIDATION_ERROR")
        if not text:
            return AgentResult.fail("متن پیام خالی است", "VALIDATION_ERROR")

        url  = f"https://api.telegram.org/bot{bot_token}/sendMessage"
        data = {
            "chat_id":    chat_id,
            "text":       text,
            "parse_mode": p.get("parse_mode", "HTML"),
        }
        try:
            resp = httpx.post(url, json=data, timeout=10)
            if resp.status_code == 200:
                return AgentResult.ok("پیام تلگرام ارسال شد", {"message_id": resp.json().get("result", {}).get("message_id")})
            return AgentResult.fail(f"خطای تلگرام: {resp.text}", "TELEGRAM_ERROR")
        except httpx.RequestError as e:
            return AgentResult.fail(f"خطای شبکه: {e}", "NETWORK_ERROR")

    def _broadcast(self, p: Dict) -> AgentResult:
        """
        ذخیره یه پیام broadcast در AppConfig برای نمایش به همه کاربران.
        اپ موقع /api/config این پیام رو می‌خونه.
        """
        message  = p.get("message", "").strip()
        msg_type = p.get("type", "info")   # info | warning | success | error
        expires  = p.get("expires_at", "")  # ISO datetime یا خالی = دائمی

        if not message:
            return AgentResult.fail("متن اطلاعیه خالی است", "VALIDATION_ERROR")

        self._set_cfg("broadcast_message",  message)
        self._set_cfg("broadcast_type",     msg_type)
        self._set_cfg("broadcast_expires",  expires)
        self._set_cfg("broadcast_enabled",  "true")

        return AgentResult.ok("اطلاعیه عمومی تنظیم شد", {
            "message":    message,
            "type":       msg_type,
            "expires_at": expires,
        })

    def _set_app_config(self, p: Dict) -> AgentResult:
        """آپدیت یک یا چند کلید در AppConfig"""
        updates: Dict = p.get("configs", {})
        if not updates:
            # حالت تک‌کلیدی
            key = p.get("key", "").strip()
            val = p.get("value", "")
            if not key:
                return AgentResult.fail("key مشخص نشده", "VALIDATION_ERROR")
            updates = {key: val}

        changed = []
        for key, value in updates.items():
            self._set_cfg(key, str(value))
            changed.append(key)

        return AgentResult.ok(f"{len(changed)} تنظیم ذخیره شد", {"updated_keys": changed})

    def _get_app_config(self, p: Dict) -> AgentResult:
        """خواندن همه یا یک کلید از AppConfig"""
        key = p.get("key", "").strip()
        if key:
            val = self._cfg(key)
            if val is None:
                return AgentResult.fail(f"کلید '{key}' یافت نشد", "NOT_FOUND")
            return AgentResult.ok(f"مقدار '{key}'", {key: val})

        all_cfg = self.db.query(AppConfig).all()
        return AgentResult.ok("همه تنظیمات", {c.key: c.value for c in all_cfg})

    def _set_maintenance(self, p: Dict) -> AgentResult:
        """روشن/خاموش کردن حالت تعمیر"""
        enabled = bool(p.get("enabled", False))
        reason  = p.get("reason", "سرویس موقتاً در دسترس نیست").strip()

        self._set_cfg("maintenance_mode",    "true" if enabled else "false")
        self._set_cfg("maintenance_message", reason)
        self._set_cfg("maintenance_since",   datetime.utcnow().isoformat() if enabled else "")

        state = "فعال" if enabled else "غیرفعال"
        return AgentResult.ok(f"حالت تعمیر {state} شد", {
            "maintenance_mode":    enabled,
            "maintenance_message": reason,
        })

    def _set_update_notice(self, p: Dict) -> AgentResult:
        """اعلان آپدیت جدید اپ"""
        version      = p.get("version", "").strip()
        force_update = bool(p.get("force_update", False))
        update_url   = p.get("update_url", "").strip()
        changelog    = p.get("changelog", "").strip()

        if not version:
            return AgentResult.fail("نسخه جدید مشخص نشده", "VALIDATION_ERROR")

        self._set_cfg("latest_version",   version)
        self._set_cfg("force_update",     "true" if force_update else "false")
        self._set_cfg("update_url",       update_url)
        self._set_cfg("update_changelog", changelog)

        return AgentResult.ok(f"اعلان آپدیت نسخه {version} تنظیم شد", {
            "version":      version,
            "force_update": force_update,
            "update_url":   update_url,
        })

    def _list_announcements(self, p: Dict) -> AgentResult:
        """لیست همه اطلاعیه‌های ذخیره‌شده"""
        active_only = bool(p.get("active_only", False))
        query = self.db.query(Announcement)
        if active_only:
            query = query.filter(Announcement.is_active == True)
        items = query.order_by(Announcement.id.desc()).all()
        return AgentResult.ok(f"{len(items)} اطلاعیه", [self._ser_ann(a) for a in items])

    def _add_announcement(self, p: Dict) -> AgentResult:
        """افزودن یه اطلاعیه جدید"""
        title   = p.get("title", "").strip()
        body    = p.get("body",  "").strip()
        ann_type = p.get("type", "info")

        if not title or not body:
            return AgentResult.fail("عنوان و متن اطلاعیه الزامی است", "VALIDATION_ERROR")

        ann = Announcement(
            title=title,
            body=body,
            type=ann_type,
            is_active=True,
            created_at=datetime.utcnow().isoformat(),
            expires_at=p.get("expires_at"),
        )
        self.db.add(ann)
        self.db.commit()
        return AgentResult.ok(f"اطلاعیه '{title}' ذخیره شد", self._ser_ann(ann))

    def _delete_announcement(self, p: Dict) -> AgentResult:
        """حذف یه اطلاعیه با id"""
        ann_id = p.get("id")
        if ann_id is None:
            return AgentResult.fail("id اطلاعیه مشخص نشده", "VALIDATION_ERROR")

        ann = self.db.query(Announcement).filter(Announcement.id == int(ann_id)).first()
        if not ann:
            return AgentResult.fail("اطلاعیه یافت نشد", "NOT_FOUND")

        self.db.delete(ann)
        self.db.commit()
        return AgentResult.ok(f"اطلاعیه #{ann_id} حذف شد")

    # ─────────────────── Helpers ────────────────────────────

    def _cfg(self, key: str) -> str | None:
        row = self.db.query(AppConfig).filter(AppConfig.key == key).first()
        return row.value if row else None

    def _set_cfg(self, key: str, value: str):
        row = self.db.query(AppConfig).filter(AppConfig.key == key).first()
        if row:
            row.value = value
        else:
            self.db.add(AppConfig(key=key, value=value))
        self.db.commit()

    def _ser_ann(self, a: Announcement) -> Dict:
        return {
            "id":         a.id,
            "title":      a.title,
            "body":       a.body,
            "type":       a.type,
            "is_active":  a.is_active,
            "created_at": a.created_at,
            "expires_at": a.expires_at,
        }
