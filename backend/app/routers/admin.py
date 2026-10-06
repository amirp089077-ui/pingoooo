"""
Alpha VPN - Admin Router (v2)
ادغام کامل با ۵ ایجنت تخصصی
محافظت‌شده با X-Admin-Key در header

ایجنت‌ها:
  #1 UserManagementAgent  → /api/admin/users/*
  #2 ServerManagementAgent → /api/admin/servers/*
  #3 GiftCodeAgent         → /api/admin/giftcodes/*
  #4 AnalyticsAgent        → /api/admin/analytics/*
  #5 NotificationAgent     → /api/admin/notifications/*
"""

import os
from fastapi import APIRouter, Depends, HTTPException, Header, status
from sqlalchemy.orm import Session
from typing import List, Optional

from app.database import get_db
from app.agents import (
    UserManagementAgent,
    ServerManagementAgent,
    GiftCodeAgent,
    AnalyticsAgent,
    NotificationAgent,
)
from app.schemas import (
    GenericResponse,
    # User
    AdminCreateUserRequest, AdminUpdateUserRequest,
    AdminResetPasswordRequest, AdminExtendPlanRequest,
    AdminBulkCreateRequest,
    # Server
    AdminCreateServerRequest, AdminUpdateServerRequest,
    AdminToggleServerRequest, AdminUpdatePingRequest,
    AdminReorderServersRequest, AdminBulkImportServersRequest,
    # Gift Code
    AdminCreateGiftCodeRequest, AdminBulkCreateGiftCodesRequest,
    AdminRedeemForUserRequest, AdminRevokeRedemptionRequest,
    # Notification
    AdminSendTelegramRequest, AdminBroadcastRequest,
    AdminSetConfigRequest, AdminMaintenanceRequest,
    AdminUpdateNoticeRequest, AdminAddAnnouncementRequest,
)

router = APIRouter(prefix="/api/admin", tags=["Admin"])

ADMIN_KEY = os.getenv("ADMIN_KEY", "change-admin-key-in-production")


# ─── Auth dependency ────────────────────────────────────────────────────

def require_admin(x_admin_key: str = Header(...)):
    if x_admin_key != ADMIN_KEY:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="دسترسی ادمین نیاز دارد"
        )


def _resp(result) -> GenericResponse:
    """تبدیل AgentResult به GenericResponse"""
    if not result.success:
        raise HTTPException(
            status_code=400,
            detail=result.message
        )
    return GenericResponse(success=True, message=result.message, data=result.data)


# ════════════════════════════════════════════════════════════════════════
#  Agent #1 — UserManagementAgent
#  Prefix: /api/admin/users
# ════════════════════════════════════════════════════════════════════════

@router.get("/users", dependencies=[Depends(require_admin)])
def list_users(
    status_filter: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """لیست همه کاربران — فیلتر اختیاری: ?status_filter=ACTIVE"""
    agent = UserManagementAgent(db)
    result = agent.execute("list", {"status": status_filter})
    return _resp(result)


@router.get("/users/{username}", dependencies=[Depends(require_admin)])
def get_user(username: str, db: Session = Depends(get_db)):
    """اطلاعات یک کاربر"""
    result = UserManagementAgent(db).execute("get", {"username": username})
    return _resp(result)


@router.post("/users", dependencies=[Depends(require_admin)])
def create_user(body: AdminCreateUserRequest, db: Session = Depends(get_db)):
    """ایجاد کاربر جدید"""
    result = UserManagementAgent(db).execute("create", body.model_dump())
    return _resp(result)


@router.post("/users/bulk", dependencies=[Depends(require_admin)])
def bulk_create_users(body: AdminBulkCreateRequest, db: Session = Depends(get_db)):
    """ایجاد دسته‌جمعی کاربران"""
    result = UserManagementAgent(db).execute("bulk_create", {
        "users": [u.model_dump() for u in body.users]
    })
    return _resp(result)


@router.patch("/users/{username}", dependencies=[Depends(require_admin)])
def update_user(username: str, body: AdminUpdateUserRequest, db: Session = Depends(get_db)):
    """ویرایش اطلاعات کاربر"""
    payload = body.model_dump(exclude_none=True)
    payload["username"] = username
    result = UserManagementAgent(db).execute("update", payload)
    return _resp(result)


@router.delete("/users/{username}", dependencies=[Depends(require_admin)])
def delete_user(username: str, db: Session = Depends(get_db)):
    """حذف کاربر"""
    result = UserManagementAgent(db).execute("delete", {"username": username})
    return _resp(result)


@router.post("/users/{username}/ban", dependencies=[Depends(require_admin)])
def ban_user(username: str, db: Session = Depends(get_db)):
    """مسدود کردن کاربر"""
    result = UserManagementAgent(db).execute("ban", {"username": username})
    return _resp(result)


@router.post("/users/{username}/unban", dependencies=[Depends(require_admin)])
def unban_user(username: str, db: Session = Depends(get_db)):
    """رفع مسدودیت کاربر"""
    result = UserManagementAgent(db).execute("unban", {"username": username})
    return _resp(result)


@router.post("/users/{username}/reset-password", dependencies=[Depends(require_admin)])
def reset_password(username: str, body: AdminResetPasswordRequest, db: Session = Depends(get_db)):
    """تغییر رمز عبور کاربر"""
    result = UserManagementAgent(db).execute("reset_password", {
        "username":     username,
        "new_password": body.new_password,
    })
    return _resp(result)


@router.post("/users/{username}/extend", dependencies=[Depends(require_admin)])
def extend_plan(username: str, body: AdminExtendPlanRequest, db: Session = Depends(get_db)):
    """تمدید اشتراک کاربر (+GB یا +روز)"""
    result = UserManagementAgent(db).execute("extend_plan", {
        "username": username,
        "add_gb":   body.add_gb,
        "add_days": body.add_days,
    })
    return _resp(result)


@router.post("/users/{username}/reset-devices", dependencies=[Depends(require_admin)])
def reset_devices(username: str, db: Session = Depends(get_db)):
    """ریست دستگاه‌های فعال کاربر"""
    result = UserManagementAgent(db).execute("reset_devices", {"username": username})
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  Agent #2 — ServerManagementAgent
#  Prefix: /api/admin/servers
# ════════════════════════════════════════════════════════════════════════

@router.get("/servers", dependencies=[Depends(require_admin)])
def list_servers(db: Session = Depends(get_db)):
    """لیست سرورهای فعال"""
    result = ServerManagementAgent(db).execute("list", {})
    return _resp(result)


@router.get("/servers/inactive", dependencies=[Depends(require_admin)])
def list_inactive_servers(db: Session = Depends(get_db)):
    """لیست سرورهای غیرفعال"""
    result = ServerManagementAgent(db).execute("list_inactive", {})
    return _resp(result)


@router.get("/servers/{server_id}", dependencies=[Depends(require_admin)])
def get_server(server_id: str, db: Session = Depends(get_db)):
    """اطلاعات یک سرور"""
    result = ServerManagementAgent(db).execute("get", {"id": server_id})
    return _resp(result)


@router.post("/servers", dependencies=[Depends(require_admin)])
def create_server(body: AdminCreateServerRequest, db: Session = Depends(get_db)):
    """ایجاد سرور جدید"""
    result = ServerManagementAgent(db).execute("create", body.model_dump())
    return _resp(result)


@router.post("/servers/bulk-import", dependencies=[Depends(require_admin)])
def bulk_import_servers(body: AdminBulkImportServersRequest, db: Session = Depends(get_db)):
    """درج دسته‌جمعی سرورها"""
    result = ServerManagementAgent(db).execute("bulk_import", {
        "servers": [s.model_dump() for s in body.servers]
    })
    return _resp(result)


@router.patch("/servers/{server_id}", dependencies=[Depends(require_admin)])
def update_server(server_id: str, body: AdminUpdateServerRequest, db: Session = Depends(get_db)):
    """ویرایش اطلاعات سرور"""
    payload = body.model_dump(exclude_none=True)
    payload["id"] = server_id
    result = ServerManagementAgent(db).execute("update", payload)
    return _resp(result)


@router.delete("/servers/{server_id}", dependencies=[Depends(require_admin)])
def delete_server(server_id: str, db: Session = Depends(get_db)):
    """حذف سرور"""
    result = ServerManagementAgent(db).execute("delete", {"id": server_id})
    return _resp(result)


@router.post("/servers/{server_id}/toggle", dependencies=[Depends(require_admin)])
def toggle_server(server_id: str, body: AdminToggleServerRequest, db: Session = Depends(get_db)):
    """فعال/غیرفعال کردن سرور"""
    payload = {"id": server_id}
    if body.is_active is not None:
        payload["is_active"] = body.is_active
    result = ServerManagementAgent(db).execute("toggle_active", payload)
    return _resp(result)


@router.patch("/servers/{server_id}/ping", dependencies=[Depends(require_admin)])
def update_server_ping(server_id: str, body: AdminUpdatePingRequest, db: Session = Depends(get_db)):
    """آپدیت ping سرور"""
    result = ServerManagementAgent(db).execute("update_ping", {
        "id": server_id, "ping": body.ping
    })
    return _resp(result)


@router.post("/servers/reorder", dependencies=[Depends(require_admin)])
def reorder_servers(body: AdminReorderServersRequest, db: Session = Depends(get_db)):
    """مرتب‌سازی سرورها"""
    result = ServerManagementAgent(db).execute("reorder", {"order": body.order})
    return _resp(result)


# ─── Config endpoints ────────────────────────────────────────────────────

class AdminServerConfigRequest(BaseModel):
    config_uri: Optional[str] = None   # کانفیگ URI مستقیم (vless://... یا vmess://...)
    config: Optional[dict]    = None   # جزئیات کانفیگ ساختاریافته


from pydantic import BaseModel as _BM

class AdminServerConfigRequest(_BM):
    config_uri:          Optional[str] = None
    protocol:            Optional[str] = None
    network:             Optional[str] = None
    security:            Optional[str] = None
    sni:                 Optional[str] = None
    alpn:                Optional[str] = None
    fingerprint:         Optional[str] = None
    public_key:          Optional[str] = None
    short_id:            Optional[str] = None
    path:                Optional[str] = None
    grpc_service_name:   Optional[str] = None
    obfs_type:           Optional[str] = None
    obfs_password:       Optional[str] = None
    vmess_id:            Optional[str] = None
    vless_id:            Optional[str] = None
    trojan_password:     Optional[str] = None
    ss_method:           Optional[str] = None
    ss_password:         Optional[str] = None
    extra_params:        Optional[str] = None
    raw_config:          Optional[str] = None


@router.get("/servers/{server_id}/config", dependencies=[Depends(require_admin)])
def get_server_config(server_id: str, db: Session = Depends(get_db)):
    """خواندن کانفیگ کامل یه سرور"""
    result = ServerManagementAgent(db).execute("get_config", {"id": server_id})
    return _resp(result)


@router.put("/servers/{server_id}/config", dependencies=[Depends(require_admin)])
def update_server_config(
    server_id: str,
    body: AdminServerConfigRequest,
    db: Session = Depends(get_db),
):
    """
    آپدیت کانفیگ اتصال VPN یه سرور.
    می‌تونه config_uri مستقیم بدی یا فیلدهای جزئیات کانفیگ رو پر کنی.
    """
    payload = body.model_dump(exclude_none=True)
    payload["id"] = server_id

    # فیلدهای config رو جدا کن
    config_fields = [
        "protocol", "network", "security", "sni", "alpn", "fingerprint",
        "public_key", "short_id", "path", "grpc_service_name",
        "obfs_type", "obfs_password", "vmess_id", "vless_id",
        "trojan_password", "ss_method", "ss_password",
        "extra_params", "raw_config",
    ]
    config_data = {k: payload.pop(k) for k in config_fields if k in payload}
    if config_data:
        payload["config"] = config_data

    result = ServerManagementAgent(db).execute("update_config", payload)
    return _resp(result)


@router.get("/servers/all", dependencies=[Depends(require_admin)])
def list_all_servers(db: Session = Depends(get_db)):
    """لیست همه سرورها — فعال و غیرفعال"""
    result = ServerManagementAgent(db).execute("list_all", {})
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  Agent #3 — GiftCodeAgent
#  Prefix: /api/admin/giftcodes
# ════════════════════════════════════════════════════════════════════════

@router.get("/giftcodes", dependencies=[Depends(require_admin)])
def list_giftcodes(active_only: bool = False, db: Session = Depends(get_db)):
    """لیست کدهای هدیه"""
    result = GiftCodeAgent(db).execute("list", {"active_only": active_only})
    return _resp(result)


@router.get("/giftcodes/stats", dependencies=[Depends(require_admin)])
def giftcode_stats(db: Session = Depends(get_db)):
    """آمار کدهای هدیه"""
    result = GiftCodeAgent(db).execute("stats", {})
    return _resp(result)


@router.get("/giftcodes/{code}", dependencies=[Depends(require_admin)])
def get_giftcode(code: str, db: Session = Depends(get_db)):
    """اطلاعات کامل یک کد هدیه + سابقه استفاده"""
    result = GiftCodeAgent(db).execute("get", {"code": code})
    return _resp(result)


@router.post("/giftcodes", dependencies=[Depends(require_admin)])
def create_giftcode(body: AdminCreateGiftCodeRequest, db: Session = Depends(get_db)):
    """ایجاد کد هدیه"""
    result = GiftCodeAgent(db).execute("create", body.model_dump())
    return _resp(result)


@router.post("/giftcodes/bulk", dependencies=[Depends(require_admin)])
def bulk_create_giftcodes(body: AdminBulkCreateGiftCodesRequest, db: Session = Depends(get_db)):
    """ایجاد دسته‌جمعی کدهای هدیه"""
    result = GiftCodeAgent(db).execute("bulk_create", body.model_dump())
    return _resp(result)


@router.delete("/giftcodes/{code}", dependencies=[Depends(require_admin)])
def delete_giftcode(code: str, db: Session = Depends(get_db)):
    """حذف کد هدیه"""
    result = GiftCodeAgent(db).execute("delete", {"code": code})
    return _resp(result)


@router.post("/giftcodes/{code}/deactivate", dependencies=[Depends(require_admin)])
def deactivate_giftcode(code: str, db: Session = Depends(get_db)):
    """غیرفعال کردن کد هدیه"""
    result = GiftCodeAgent(db).execute("deactivate", {"code": code})
    return _resp(result)


@router.post("/giftcodes/{code}/redeem-for-user", dependencies=[Depends(require_admin)])
def admin_redeem_for_user(code: str, body: AdminRedeemForUserRequest, db: Session = Depends(get_db)):
    """اعمال کد هدیه برای یک کاربر توسط ادمین"""
    result = GiftCodeAgent(db).execute("redeem", {
        "code": code, "username": body.username
    })
    return _resp(result)


@router.delete("/giftcodes/{code}/redemptions/{username}", dependencies=[Depends(require_admin)])
def revoke_redemption(code: str, username: str, db: Session = Depends(get_db)):
    """لغو استفاده یه کاربر از کد و برگرداندن هدیه"""
    result = GiftCodeAgent(db).execute("revoke_redemption", {
        "code": code, "username": username
    })
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  Agent #4 — AnalyticsAgent
#  Prefix: /api/admin/analytics
# ════════════════════════════════════════════════════════════════════════

@router.get("/analytics/dashboard", dependencies=[Depends(require_admin)])
def analytics_dashboard(db: Session = Depends(get_db)):
    """داشبورد اصلی ادمین"""
    result = AnalyticsAgent(db).execute("dashboard", {})
    return _resp(result)


@router.get("/analytics/users", dependencies=[Depends(require_admin)])
def analytics_users(db: Session = Depends(get_db)):
    """آمار تفکیکی کاربران"""
    result = AnalyticsAgent(db).execute("user_stats", {})
    return _resp(result)


@router.get("/analytics/servers", dependencies=[Depends(require_admin)])
def analytics_servers(db: Session = Depends(get_db)):
    """آمار سرورها"""
    result = AnalyticsAgent(db).execute("server_stats", {})
    return _resp(result)


@router.get("/analytics/traffic", dependencies=[Depends(require_admin)])
def analytics_traffic(db: Session = Depends(get_db)):
    """گزارش مصرف ترافیک"""
    result = AnalyticsAgent(db).execute("traffic_report", {})
    return _resp(result)


@router.get("/analytics/expiring-users", dependencies=[Depends(require_admin)])
def analytics_expiring_users(days: int = 7, db: Session = Depends(get_db)):
    """کاربرانی که به‌زودی منقضی می‌شن — پیش‌فرض: ۷ روز آینده"""
    result = AnalyticsAgent(db).execute("expiring_users", {"days": days})
    return _resp(result)


@router.get("/analytics/top-users", dependencies=[Depends(require_admin)])
def analytics_top_users(limit: int = 10, db: Session = Depends(get_db)):
    """کاربران با بیشترین مصرف"""
    result = AnalyticsAgent(db).execute("top_users", {"limit": limit})
    return _resp(result)


@router.get("/analytics/daily-signups", dependencies=[Depends(require_admin)])
def analytics_daily_signups(days: int = 30, db: Session = Depends(get_db)):
    """ثبت‌نام روزانه"""
    result = AnalyticsAgent(db).execute("daily_signups", {"days": days})
    return _resp(result)


@router.get("/analytics/giftcodes", dependencies=[Depends(require_admin)])
def analytics_giftcodes(db: Session = Depends(get_db)):
    """گزارش کامل کدهای هدیه"""
    result = AnalyticsAgent(db).execute("giftcode_report", {})
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  Agent #5 — NotificationAgent
#  Prefix: /api/admin/notifications
# ════════════════════════════════════════════════════════════════════════

@router.post("/notifications/telegram", dependencies=[Depends(require_admin)])
def send_telegram(body: AdminSendTelegramRequest, db: Session = Depends(get_db)):
    """ارسال پیام تلگرام"""
    result = NotificationAgent(db).execute("send_telegram", body.model_dump())
    return _resp(result)


@router.post("/notifications/broadcast", dependencies=[Depends(require_admin)])
def set_broadcast(body: AdminBroadcastRequest, db: Session = Depends(get_db)):
    """تنظیم اطلاعیه عمومی برای نمایش در اپ"""
    result = NotificationAgent(db).execute("broadcast", body.model_dump())
    return _resp(result)


@router.delete("/notifications/broadcast", dependencies=[Depends(require_admin)])
def clear_broadcast(db: Session = Depends(get_db)):
    """پاک کردن اطلاعیه عمومی"""
    result = NotificationAgent(db).execute("set_app_config", {
        "configs": {"broadcast_enabled": "false", "broadcast_message": ""}
    })
    return _resp(result)


@router.get("/notifications/config", dependencies=[Depends(require_admin)])
def get_app_config(key: Optional[str] = None, db: Session = Depends(get_db)):
    """خواندن تنظیمات اپ"""
    result = NotificationAgent(db).execute("get_app_config", {"key": key or ""})
    return _resp(result)


@router.patch("/notifications/config", dependencies=[Depends(require_admin)])
def set_app_config(body: AdminSetConfigRequest, db: Session = Depends(get_db)):
    """آپدیت یک یا چند تنظیم اپ"""
    result = NotificationAgent(db).execute("set_app_config", body.model_dump())
    return _resp(result)


@router.post("/notifications/maintenance", dependencies=[Depends(require_admin)])
def set_maintenance(body: AdminMaintenanceRequest, db: Session = Depends(get_db)):
    """روشن/خاموش کردن حالت تعمیر"""
    result = NotificationAgent(db).execute("set_maintenance", body.model_dump())
    return _resp(result)


@router.post("/notifications/update-notice", dependencies=[Depends(require_admin)])
def set_update_notice(body: AdminUpdateNoticeRequest, db: Session = Depends(get_db)):
    """اعلان آپدیت جدید اپ"""
    result = NotificationAgent(db).execute("set_update_notice", body.model_dump())
    return _resp(result)


@router.get("/notifications/announcements", dependencies=[Depends(require_admin)])
def list_announcements(active_only: bool = False, db: Session = Depends(get_db)):
    """لیست اطلاعیه‌ها"""
    result = NotificationAgent(db).execute("list_announcements", {"active_only": active_only})
    return _resp(result)


@router.post("/notifications/announcements", dependencies=[Depends(require_admin)])
def add_announcement(body: AdminAddAnnouncementRequest, db: Session = Depends(get_db)):
    """افزودن اطلاعیه جدید"""
    result = NotificationAgent(db).execute("add_announcement", body.model_dump())
    return _resp(result)


@router.delete("/notifications/announcements/{ann_id}", dependencies=[Depends(require_admin)])
def delete_announcement(ann_id: int, db: Session = Depends(get_db)):
    """حذف اطلاعیه"""
    result = NotificationAgent(db).execute("delete_announcement", {"id": ann_id})
    return _resp(result)
