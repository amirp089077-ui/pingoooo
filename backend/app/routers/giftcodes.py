"""
Alpha VPN - Gift Codes Router
POST /api/giftcodes/redeem
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db, User, GiftCode, GiftCodeRedemption
from app.auth import get_current_user
from app.schemas import RedeemGiftCodeRequest, GiftCodeResponse, GenericResponse

router = APIRouter(prefix="/api/giftcodes", tags=["Gift Codes"])


@router.post("/redeem", response_model=GiftCodeResponse)
def redeem_gift_code(
    body: RedeemGiftCodeRequest,
    username: str = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    code = body.code.strip().upper()

    # 1. کد وجود داره؟
    gift: GiftCode = db.query(GiftCode).filter(GiftCode.code == code).first()
    if not gift or not gift.is_active:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="کد هدیه نامعتبر یا غیرفعال است"
        )

    # 2. سقف استفاده بررسی کن
    if gift.max_uses != -1 and gift.used_count >= gift.max_uses:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="ظرفیت این کد هدیه تکمیل شده است"
        )

    # 3. این کاربر قبلاً استفاده کرده؟
    already = (
        db.query(GiftCodeRedemption)
        .filter(
            GiftCodeRedemption.code == code,
            GiftCodeRedemption.username == username,
        )
        .first()
    )
    if already:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="این کد قبلاً توسط شما استفاده شده است"
        )

    # 4. اعمال کن
    user: User = db.query(User).filter(User.username == username).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="کاربر یافت نشد")

    user.remaining_gb   += gift.bonus_gb
    user.total_quota_gb += gift.bonus_gb
    user.remaining_days += gift.bonus_days

    # اگه قبلاً EXPIRED بود و حالا حجم گرفت، دوباره ACTIVE کن
    if user.status == "EXPIRED" and user.remaining_gb > 0:
        user.status = "ACTIVE"

    gift.used_count += 1

    db.add(GiftCodeRedemption(code=code, username=username))
    db.commit()

    return GiftCodeResponse(
        code=gift.code,
        bonus_gb=gift.bonus_gb,
        bonus_days=gift.bonus_days,
        description=gift.description,
    )
