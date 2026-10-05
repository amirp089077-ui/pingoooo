"""
Alpha VPN - Database Layer
SQLite با SQLAlchemy - ساده، بدون نیاز به سرور جداگانه
"""

import os
from datetime import datetime
from sqlalchemy import (
    create_engine, Column, String, Float, Integer,
    Boolean, DateTime, Text, ForeignKey
)
from sqlalchemy.orm import declarative_base, sessionmaker, Session
from sqlalchemy.pool import StaticPool

DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./alphavpn.db")

# برای SQLite باید check_same_thread رو غیرفعال کنیم
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}

engine = create_engine(
    DATABASE_URL,
    connect_args=connect_args,
    poolclass=StaticPool if DATABASE_URL.startswith("sqlite") else None,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


# ─────────────────────────────── Models ────────────────────────────────

class User(Base):
    __tablename__ = "users"

    username        = Column(String(64),  primary_key=True, index=True)
    password_hash   = Column(String(128), nullable=False)
    plan_type       = Column(String(32),  default="MULTI_USER")   # SINGLE_USER / MULTI_USER
    status          = Column(String(16),  default="ACTIVE")        # ACTIVE / EXPIRED / BANNED
    remaining_gb    = Column(Float,       default=10.0)
    used_gb         = Column(Float,       default=0.0)
    total_quota_gb  = Column(Float,       default=10.0)
    remaining_days  = Column(Integer,     default=30)
    total_days      = Column(Integer,     default=30)
    expiry_date     = Column(String(32),  default="")
    max_devices     = Column(Integer,     default=2)
    active_devices  = Column(Integer,     default=0)
    created_at      = Column(DateTime,    default=datetime.utcnow)
    updated_at      = Column(DateTime,    default=datetime.utcnow, onupdate=datetime.utcnow)


class Server(Base):
    __tablename__ = "servers"

    id          = Column(String(64),  primary_key=True, index=True)
    country     = Column(String(64),  nullable=False)
    city        = Column(String(64),  nullable=False)
    flag        = Column(String(8),   default="🌐")
    host        = Column(String(256), nullable=False)
    port        = Column(Integer,     default=443)
    badge       = Column(String(16),  nullable=True)
    emoji       = Column(String(16),  default="")
    is_pro      = Column(Boolean,     default=False)
    is_active   = Column(Boolean,     default=True)
    config_uri  = Column(Text,        default="")
    ping        = Column(Integer,     default=50)
    order_idx   = Column(Integer,     default=0)


class GiftCode(Base):
    __tablename__ = "gift_codes"

    code        = Column(String(64),  primary_key=True, index=True)
    bonus_gb    = Column(Float,       default=5.0)
    bonus_days  = Column(Integer,     default=0)
    description = Column(String(256), default="کد هدیه")
    is_active   = Column(Boolean,     default=True)
    max_uses    = Column(Integer,     default=1)       # -1 = unlimited
    used_count  = Column(Integer,     default=0)
    created_at  = Column(DateTime,    default=datetime.utcnow)


class GiftCodeRedemption(Base):
    __tablename__ = "gift_code_redemptions"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    code        = Column(String(64),  ForeignKey("gift_codes.code"), nullable=False)
    username    = Column(String(64),  ForeignKey("users.username"),  nullable=False)
    redeemed_at = Column(DateTime,    default=datetime.utcnow)


class AppConfig(Base):
    __tablename__ = "app_config"

    key         = Column(String(64),  primary_key=True)
    value       = Column(Text,        default="")
    updated_at  = Column(DateTime,    default=datetime.utcnow, onupdate=datetime.utcnow)


# ─────────────────────────────── Init ──────────────────────────────────

def create_tables():
    Base.metadata.create_all(bind=engine, checkfirst=True)


def get_db():
    """FastAPI dependency برای گرفتن session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def seed_default_data(db: Session):
    """داده‌های اولیه پیش‌فرض — فقط اگه خالی باشه"""

    # Config پیش‌فرض
    defaults = {
        "telegram_support": "AlphaSupport_ir",
        "channel_url":      "https://t.me/AlphaSupport_ir",
        "announcement":     "",
        "app_version":      "1.0.0",
        "force_update":     "false",
    }
    for k, v in defaults.items():
        if not db.query(AppConfig).filter(AppConfig.key == k).first():
            db.add(AppConfig(key=k, value=v))

    # یه سرور نمونه
    if not db.query(Server).first():
        db.add(Server(
            id="srv_de_1",
            country="آلمان",
            city="فرانکفورت",
            flag="🇩🇪",
            host="YOUR_SERVER_IP",
            port=443,
            badge="A",
            is_pro=False,
            is_active=True,
            config_uri="",
            ping=45,
            order_idx=1,
        ))

    # یه کد هدیه نمونه
    if not db.query(GiftCode).filter(GiftCode.code == "ALPHA5").first():
        db.add(GiftCode(
            code="ALPHA5",
            bonus_gb=5.0,
            bonus_days=0,
            description="کد هدیه ۵ گیگابایت",
            is_active=True,
            max_uses=-1,
        ))

    db.commit()
