"""
Alpha VPN - Admin Router
مدیریت کاربران، سرورها، کدهای هدیه
محافظت‌شده با ADMIN_KEY در header
"""

import os
from fastapi import APIRouter, Depends, HTTPException, Header, status
from sqlalchemy.orm import Session
from typing import List, Optional

from app.database import get_db, User, Server, GiftCode, AppConfig
from app.auth import hash_password
from app.schemas import (
    CreateUserRequest, UpdateUserRequest,
    CreateServerRequest, CreateGiftCodeRequest,
    UserProfile, ServerItem, GenericResponse
)

router = APIRouter(prefix="/api/admin", tags=["Admin"])

ADMIN_KEY = os.getenv("ADMIN_KEY", "change-admin-key-in-production")


def require_admin(x_admin_key: str = Header(...)):
    if x_admin_key != ADMIN_KEY:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="دسترسی ادمین نیاز دارد"
        )


# ────────────────── Users ─────────────────────────

@router.get("/users", dependencies=[Depends(require_admin)])
def list_users(db: Session = Depends(get_db)):
    users = db.query(User).all()
    return [
        {
            "username":       u.username,
            "plan_type":      u.plan_type,
            "status":         u.status,
            "remaining_gb":   u.remaining_gb,
            "used_gb":        u.used_gb,
            "total_quota_gb": u.total_quota_gb,
            "remaining_days": u.remaining_days,
            "max_devices":    u.max_devices,
            "active_devices": u.active_devices,
            "expiry_date":    u.expiry_date,
        }
        for u in users
    ]


@router.post("/users", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def create_user(body: CreateUserRequest, db: Session = Depends(get_db)):
    if db.query(User).filter(User.username == body.username).first():
        raise HTTPException(status_code=409, detail="این نام کاربری قبلاً ثبت شده")

    user = User(
        username=body.username,
        password_hash=hash_password(body.password),
        plan_type=body.plan_type,
        status="ACTIVE",
        remaining_gb=body.total_quota_gb,
        used_gb=0.0,
        total_quota_gb=body.total_quota_gb,
        remaining_days=body.remaining_days,
        total_days=body.remaining_days,
        expiry_date=body.expiry_date,
        max_devices=body.max_devices,
        active_devices=0,
    )
    db.add(user)
    db.commit()
    return GenericResponse(success=True, message=f"کاربر {body.username} ساخته شد")


@router.patch("/users/{username}", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def update_user(username: str, body: UpdateUserRequest, db: Session = Depends(get_db)):
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=404, detail="کاربر یافت نشد")

    if body.plan_type      is not None: user.plan_type      = body.plan_type
    if body.status         is not None: user.status         = body.status
    if body.remaining_gb   is not None: user.remaining_gb   = body.remaining_gb
    if body.used_gb        is not None: user.used_gb        = body.used_gb
    if body.total_quota_gb is not None: user.total_quota_gb = body.total_quota_gb
    if body.remaining_days is not None: user.remaining_days = body.remaining_days
    if body.max_devices    is not None: user.max_devices    = body.max_devices
    if body.expiry_date    is not None: user.expiry_date    = body.expiry_date

    db.commit()
    return GenericResponse(success=True, message=f"کاربر {username} به‌روز شد")


@router.delete("/users/{username}", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def delete_user(username: str, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=404, detail="کاربر یافت نشد")
    db.delete(user)
    db.commit()
    return GenericResponse(success=True, message=f"کاربر {username} حذف شد")


# ────────────────── Servers ─────────────────────

@router.post("/servers", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def create_server(body: CreateServerRequest, db: Session = Depends(get_db)):
    if db.query(Server).filter(Server.id == body.id).first():
        raise HTTPException(status_code=409, detail="این ID سرور قبلاً وجود دارد")

    server = Server(
        id=body.id, country=body.country, city=body.city,
        flag=body.flag, host=body.host, port=body.port,
        badge=body.badge, emoji=body.emoji, is_pro=body.is_pro,
        config_uri=body.config_uri, order_idx=body.order_idx,
        is_active=True, ping=50,
    )
    db.add(server)
    db.commit()
    return GenericResponse(success=True, message=f"سرور {body.id} ساخته شد")


@router.delete("/servers/{server_id}", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def delete_server(server_id: str, db: Session = Depends(get_db)):
    server = db.query(Server).filter(Server.id == server_id).first()
    if not server:
        raise HTTPException(status_code=404, detail="سرور یافت نشد")
    db.delete(server)
    db.commit()
    return GenericResponse(success=True, message=f"سرور {server_id} حذف شد")


# ────────────────── Gift Codes ──────────────────

@router.post("/giftcodes", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def create_gift_code(body: CreateGiftCodeRequest, db: Session = Depends(get_db)):
    if db.query(GiftCode).filter(GiftCode.code == body.code.upper()).first():
        raise HTTPException(status_code=409, detail="این کد قبلاً وجود دارد")

    gift = GiftCode(
        code=body.code.upper(),
        bonus_gb=body.bonus_gb,
        bonus_days=body.bonus_days,
        description=body.description,
        is_active=True,
        max_uses=body.max_uses,
        used_count=0,
    )
    db.add(gift)
    db.commit()
    return GenericResponse(success=True, message=f"کد هدیه {body.code} ساخته شد")


# ────────────────── App Config ──────────────────

@router.patch("/config/{key}", dependencies=[Depends(require_admin)], response_model=GenericResponse)
def update_config(key: str, value: str, db: Session = Depends(get_db)):
    row = db.query(AppConfig).filter(AppConfig.key == key).first()
    if row:
        row.value = value
    else:
        db.add(AppConfig(key=key, value=value))
    db.commit()
    return GenericResponse(success=True, message=f"تنظیم {key} به‌روز شد")
