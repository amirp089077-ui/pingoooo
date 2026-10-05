"""
Alpha VPN - Users Router
GET  /api/users/me
PATCH /api/users/me/usage
PATCH /api/users/me/devices
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db, User
from app.auth import get_current_user
from app.schemas import UserProfile, UpdateUsageRequest, UpdateDevicesRequest, GenericResponse

router = APIRouter(prefix="/api/users", tags=["Users"])


@router.get("/me", response_model=UserProfile)
def get_my_profile(
    username: str = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="کاربر یافت نشد")

    return UserProfile(
        username=user.username,
        plan_type=user.plan_type,
        status=user.status,
        remaining_gb=user.remaining_gb,
        used_gb=user.used_gb,
        total_quota_gb=user.total_quota_gb,
        remaining_days=user.remaining_days,
        total_days=user.total_days,
        expiry_date=user.expiry_date,
        max_devices=user.max_devices,
        active_devices=user.active_devices,
    )


@router.patch("/me/usage", response_model=GenericResponse)
def update_usage(
    body: UpdateUsageRequest,
    username: str = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="کاربر یافت نشد")

    user.used_gb = round(body.used_gb, 4)
    user.remaining_gb = round(max(0.0, body.remaining_gb), 4)

    # اگه حجم تموم شد status رو EXPIRED کن
    if user.remaining_gb <= 0:
        user.status = "EXPIRED"

    db.commit()
    return GenericResponse(success=True, message="مصرف به‌روزرسانی شد")


@router.patch("/me/devices", response_model=GenericResponse)
def update_devices(
    body: UpdateDevicesRequest,
    username: str = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="کاربر یافت نشد")

    user.active_devices = max(0, body.active_devices)
    db.commit()
    return GenericResponse(success=True, message="تعداد دستگاه‌ها به‌روزرسانی شد")
