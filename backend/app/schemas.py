"""
Alpha VPN - Pydantic Schemas (Request / Response models)
v2: اضافه شدن schema های کامل ادمین برای همه ایجنت‌ها
"""

from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


# ══════════════════════════════════════════════
#  Auth
# ══════════════════════════════════════════════

class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, max_length=64)
    password: str = Field(..., min_length=1, max_length=128)


class LoginResponse(BaseModel):
    token:          str
    username:       str
    plan_type:      str
    remaining_gb:   float
    used_gb:        float
    total_quota_gb: float
    remaining_days: int
    total_days:     int
    expiry_date:    str
    max_devices:    int
    active_devices: int
    status:         str


# ══════════════════════════════════════════════
#  User (public)
# ══════════════════════════════════════════════

class UserProfile(BaseModel):
    username:       str
    plan_type:      str
    status:         str
    remaining_gb:   float
    used_gb:        float
    total_quota_gb: float
    remaining_days: int
    total_days:     int
    expiry_date:    str
    max_devices:    int
    active_devices: int


class UpdateUsageRequest(BaseModel):
    used_gb:      float
    remaining_gb: float


class UpdateDevicesRequest(BaseModel):
    active_devices: int


# ══════════════════════════════════════════════
#  Server (public)
# ══════════════════════════════════════════════

class ServerItem(BaseModel):
    id:         str
    country:    str
    city:       str
    flag:       str
    host:       str
    port:       int
    badge:      Optional[str] = None
    emoji:      str  = ""
    is_pro:     bool = False
    config_uri: str  = ""
    ping:       int  = 50


class ServersResponse(BaseModel):
    servers: List[ServerItem]


# ══════════════════════════════════════════════
#  App Config (public)
# ══════════════════════════════════════════════

class AppConfigResponse(BaseModel):
    telegram_support:  str
    channel_url:       str
    announcement:      str
    app_version:       str
    force_update:      bool
    maintenance_mode:  bool  = False
    maintenance_message: str = ""
    broadcast_enabled: bool  = False
    broadcast_message: str   = ""
    broadcast_type:    str   = "info"


# ══════════════════════════════════════════════
#  Gift Code (public)
# ══════════════════════════════════════════════

class RedeemGiftCodeRequest(BaseModel):
    code: str = Field(..., min_length=1, max_length=64)


# ══════════════════════════════════════════════
#  Generic
# ══════════════════════════════════════════════

class GenericResponse(BaseModel):
    success: bool
    message: str
    data:    Optional[Any] = None


# ══════════════════════════════════════════════
#  Admin — User Management (Agent #1)
# ══════════════════════════════════════════════

class AdminCreateUserRequest(BaseModel):
    username:       str   = Field(..., min_length=1, max_length=64)
    password:       str   = Field(..., min_length=4, max_length=128)
    plan_type:      str   = "MULTI_USER"
    total_quota_gb: float = 10.0
    remaining_days: int   = 30
    max_devices:    int   = 2
    expiry_date:    str   = ""


class AdminUpdateUserRequest(BaseModel):
    plan_type:      Optional[str]   = None
    status:         Optional[str]   = None
    remaining_gb:   Optional[float] = None
    used_gb:        Optional[float] = None
    total_quota_gb: Optional[float] = None
    remaining_days: Optional[int]   = None
    max_devices:    Optional[int]   = None
    expiry_date:    Optional[str]   = None


class AdminResetPasswordRequest(BaseModel):
    new_password: str = Field(..., min_length=4, max_length=128)


class AdminExtendPlanRequest(BaseModel):
    add_gb:   float = 0.0
    add_days: int   = 0


class AdminBulkCreateRequest(BaseModel):
    users: List[AdminCreateUserRequest]


# ══════════════════════════════════════════════
#  Admin — Server Management (Agent #2)
# ══════════════════════════════════════════════

class AdminCreateServerRequest(BaseModel):
    id:         str
    country:    str
    city:       str
    flag:       str   = "🌐"
    host:       str
    port:       int   = 443
    badge:      Optional[str] = None
    emoji:      str   = ""
    is_pro:     bool  = False
    config_uri: str   = ""
    ping:       int   = 50
    order_idx:  int   = 0


class AdminUpdateServerRequest(BaseModel):
    country:    Optional[str]  = None
    city:       Optional[str]  = None
    flag:       Optional[str]  = None
    host:       Optional[str]  = None
    port:       Optional[int]  = None
    badge:      Optional[str]  = None
    emoji:      Optional[str]  = None
    is_pro:     Optional[bool] = None
    config_uri: Optional[str]  = None
    ping:       Optional[int]  = None
    order_idx:  Optional[int]  = None


class AdminToggleServerRequest(BaseModel):
    is_active: Optional[bool] = None    # None = toggle


class AdminUpdatePingRequest(BaseModel):
    ping: int


class AdminReorderServersRequest(BaseModel):
    order: List[str]    # لیست server id به ترتیب جدید


class AdminBulkImportServersRequest(BaseModel):
    servers: List[AdminCreateServerRequest]


# ══════════════════════════════════════════════
#  Admin — Gift Code (Agent #3)
# ══════════════════════════════════════════════

class AdminCreateGiftCodeRequest(BaseModel):
    code:        Optional[str]  = None   # اگه خالی بذاری اتوماتیک تولید می‌شه
    add_gb:      float          = 0.0
    add_days:    int            = 0
    max_uses:    int            = 1      # 0 = unlimited
    description: str            = ""
    expires_at:  Optional[str]  = None


class AdminBulkCreateGiftCodesRequest(BaseModel):
    count:       int            = Field(..., ge=1, le=500)
    add_gb:      float          = 0.0
    add_days:    int            = 0
    max_uses:    int            = 1
    prefix:      str            = "ALPHA"
    description: str            = ""
    expires_at:  Optional[str]  = None


class AdminRedeemForUserRequest(BaseModel):
    username: str
    code:     str


class AdminRevokeRedemptionRequest(BaseModel):
    username: str


# ══════════════════════════════════════════════
#  Admin — Notification (Agent #5)
# ══════════════════════════════════════════════

class AdminSendTelegramRequest(BaseModel):
    text:       str
    chat_id:    Optional[str] = None
    parse_mode: str           = "HTML"


class AdminBroadcastRequest(BaseModel):
    message:    str
    type:       str           = "info"   # info | warning | success | error
    expires_at: Optional[str] = None


class AdminSetConfigRequest(BaseModel):
    configs: Dict[str, str]


class AdminMaintenanceRequest(BaseModel):
    enabled: bool
    reason:  str = "سرویس موقتاً در دسترس نیست"


class AdminUpdateNoticeRequest(BaseModel):
    version:      str
    force_update: bool          = False
    update_url:   str           = ""
    changelog:    str           = ""


class AdminAddAnnouncementRequest(BaseModel):
    title:      str
    body:       str
    type:       str           = "info"
    expires_at: Optional[str] = None


# backward compat — اسم قدیمی هنوز import می‌شه
CreateUserRequest   = AdminCreateUserRequest
UpdateUserRequest   = AdminUpdateUserRequest
CreateServerRequest = AdminCreateServerRequest
CreateGiftCodeRequest = AdminCreateGiftCodeRequest
