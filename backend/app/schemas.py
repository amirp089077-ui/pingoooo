"""
Alpha VPN - Pydantic Schemas (Request / Response models)
"""

from typing import Optional, List
from pydantic import BaseModel, Field


# ─────────────── Auth ───────────────────────────

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


# ─────────────── User ───────────────────────────

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


# ─────────────── Server ─────────────────────────

class ServerItem(BaseModel):
    id:         str
    country:    str
    city:       str
    flag:       str
    host:       str
    port:       int
    badge:      Optional[str] = None
    emoji:      str = ""
    is_pro:     bool = False
    config_uri: str = ""
    ping:       int = 50


class ServersResponse(BaseModel):
    servers: List[ServerItem]


# ─────────────── App Config ─────────────────────

class AppConfigResponse(BaseModel):
    telegram_support: str
    channel_url:      str
    announcement:     str
    app_version:      str
    force_update:     bool


# ─────────────── Gift Code ──────────────────────

class RedeemGiftCodeRequest(BaseModel):
    code: str = Field(..., min_length=1, max_length=64)


class GiftCodeResponse(BaseModel):
    code:        str
    bonus_gb:    float
    bonus_days:  int
    description: str


# ─────────────── Admin ──────────────────────────

class CreateUserRequest(BaseModel):
    username:      str   = Field(..., min_length=1, max_length=64)
    password:      str   = Field(..., min_length=4, max_length=128)
    plan_type:     str   = "MULTI_USER"
    total_quota_gb: float = 10.0
    remaining_days: int  = 30
    max_devices:   int   = 2
    expiry_date:   str   = ""


class UpdateUserRequest(BaseModel):
    plan_type:      Optional[str]   = None
    status:         Optional[str]   = None
    remaining_gb:   Optional[float] = None
    used_gb:        Optional[float] = None
    total_quota_gb: Optional[float] = None
    remaining_days: Optional[int]   = None
    max_devices:    Optional[int]   = None
    expiry_date:    Optional[str]   = None


class CreateServerRequest(BaseModel):
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
    order_idx:  int   = 0


class CreateGiftCodeRequest(BaseModel):
    code:        str
    bonus_gb:    float = 5.0
    bonus_days:  int   = 0
    description: str   = "کد هدیه"
    max_uses:    int   = -1


class GenericResponse(BaseModel):
    success: bool
    message: str
