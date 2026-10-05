"""
Alpha VPN - Auth Router
POST /api/auth/login
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db, User
from app.auth import verify_password, create_access_token
from app.schemas import LoginRequest, LoginResponse

router = APIRouter(prefix="/api/auth", tags=["Auth"])


@router.post("/login", response_model=LoginResponse)
def login(body: LoginRequest, db: Session = Depends(get_db)):
    username = body.username.strip()
    password = body.password.strip()

    # 1. یوزر وجود داره؟
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="کاربری با این نام یافت نشد"
        )

    # 2. رمز عبور درسته؟
    if not verify_password(password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="رمز عبور نادرست است"
        )

    # 3. اشتراک منقضی نشده؟
    if user.status == "EXPIRED" or user.remaining_gb <= 0:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="اشتراک این حساب منقضی شده است"
        )

    if user.status == "BANNED":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="این حساب مسدود شده است"
        )

    # 4. سقف دستگاه
    if user.active_devices >= user.max_devices:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"سقف دستگاه‌ها ({user.max_devices} دستگاه) پر است. ابتدا از دستگاه قبلی خارج شوید"
        )

    # 5. active_devices رو یه واحد اضافه کن
    user.active_devices += 1
    db.commit()
    db.refresh(user)

    # 6. JWT بساز
    token = create_access_token(username)

    return LoginResponse(
        token=token,
        username=user.username,
        plan_type=user.plan_type,
        remaining_gb=user.remaining_gb,
        used_gb=user.used_gb,
        total_quota_gb=user.total_quota_gb,
        remaining_days=user.remaining_days,
        total_days=user.total_days,
        expiry_date=user.expiry_date,
        max_devices=user.max_devices,
        active_devices=user.active_devices,
        status=user.status,
    )


@router.post("/logout")
def logout(
    username: str = Depends(__import__("app.auth", fromlist=["get_current_user"]).get_current_user),
    db: Session = Depends(get_db),
):
    user: User = db.query(User).filter(User.username == username).first()
    if user and user.active_devices > 0:
        user.active_devices -= 1
        db.commit()
    return {"success": True, "message": "خروج موفق"}
