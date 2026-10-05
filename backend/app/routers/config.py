"""
Alpha VPN - App Config Router
GET /api/config  → تنظیمات اپ (اعلان، تلگرام، ...)
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db, AppConfig
from app.schemas import AppConfigResponse

router = APIRouter(prefix="/api/config", tags=["Config"])


@router.get("", response_model=AppConfigResponse)
def get_app_config(db: Session = Depends(get_db)):
    """این endpoint نیاز به token نداره — اپ قبل از login هم میتونه بخونه"""

    def val(key: str, default: str = "") -> str:
        row = db.query(AppConfig).filter(AppConfig.key == key).first()
        return row.value if row else default

    return AppConfigResponse(
        telegram_support=val("telegram_support", "AlphaSupport_ir"),
        channel_url=val("channel_url",      "https://t.me/AlphaSupport_ir"),
        announcement=val("announcement",    ""),
        app_version=val("app_version",      "1.0.0"),
        force_update=val("force_update",    "false").lower() == "true",
    )
