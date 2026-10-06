"""
Alpha VPN - Database Layer
SQLite با SQLAlchemy - ساده، بدون نیاز به سرور جداگانه
v2: اضافه شدن مدل‌های Announcement برای NotificationAgent
    یکپارچه‌سازی نام فیلدها با ایجنت‌ها
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
    # نام‌گذاری یکپارچه با ایجنت‌ها: add_gb / add_days
    add_gb      = Column(Float,       default=5.0)
    add_days    = Column(Integer,     default=0)
    description = Column(String(256), default="")
    expires_at  = Column(String(32),  nullable=True)
    is_active   = Column(Boolean,     default=True)
    max_uses    = Column(Integer,     default=1)       # 0 = unlimited
    used_count  = Column(Integer,     default=0)
    created_at  = Column(DateTime,    default=datetime.utcnow)


class GiftCodeRedemption(Base):
    __tablename__ = "gift_code_redemptions"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    # یکپارچه با ایجنت‌ها: gift_code (نه code)
    gift_code   = Column(String(64),  ForeignKey("gift_codes.code"), nullable=False)
    username    = Column(String(64),  ForeignKey("users.username"),  nullable=False)
    redeemed_at = Column(String(32),  default="")


class ServerConfig(Base):
    """
    کانفیگ جزئیات اتصال VPN هر سرور
    پروتکل‌های پشتیبانی‌شده: VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC
    """
    __tablename__ = "server_configs"

    id               = Column(Integer,     primary_key=True, autoincrement=True)
    server_id        = Column(String(64),  ForeignKey("servers.id"), unique=True, nullable=False, index=True)

    # ─── پروتکل اصلی ───────────────────────────────────────
    protocol         = Column(String(32),  default="vless")   # vless | vmess | trojan | ss | hysteria2 | tuic
    network          = Column(String(32),  default="tcp")      # tcp | ws | grpc | h2 | quic
    security         = Column(String(32),  default="tls")      # tls | reality | none

    # ─── TLS / Reality ─────────────────────────────────────
    sni              = Column(String(256), default="")
    alpn             = Column(String(128), default="h2,http/1.1")
    fingerprint      = Column(String(64),  default="chrome")   # برای Reality
    public_key       = Column(String(512), default="")         # Reality public key
    short_id         = Column(String(128), default="")         # Reality short id

    # ─── WebSocket / gRPC ──────────────────────────────────
    path             = Column(String(256), default="/")
    grpc_service_name= Column(String(128), default="")

    # ─── OBFS (برای Shadowsocks و غیره) ────────────────────
    obfs_type        = Column(String(64),  default="")
    obfs_password    = Column(String(256), default="")

    # ─── اطلاعات احراز هویت هر پروتکل ─────────────────────
    vmess_id         = Column(String(128), default="")         # VMess UUID
    vless_id         = Column(String(128), default="")         # VLESS UUID
    trojan_password  = Column(String(256), default="")
    ss_method        = Column(String(64),  default="aes-256-gcm")
    ss_password      = Column(String(256), default="")

    # ─── Hysteria2 / TUIC ──────────────────────────────────
    # password و auth در trojan_password / ss_password ذخیره می‌شن

    # ─── خروجی نهایی ───────────────────────────────────────
    extra_params     = Column(Text,        default="")         # پارامترهای اضافه JSON
    raw_config       = Column(Text,        default="")         # کانفیگ کامل (URI یا JSON)

    updated_at       = Column(DateTime,    default=datetime.utcnow, onupdate=datetime.utcnow)



    __tablename__ = "app_config"

    key         = Column(String(64),  primary_key=True)
    value       = Column(Text,        default="")
    updated_at  = Column(DateTime,    default=datetime.utcnow, onupdate=datetime.utcnow)


class Announcement(Base):
    """اطلاعیه‌های درون‌اپ — مدیریت‌شده توسط NotificationAgent"""
    __tablename__ = "announcements"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    title       = Column(String(128), nullable=False)
    body        = Column(Text,        nullable=False)
    type        = Column(String(16),  default="info")   # info | warning | success | error
    is_active   = Column(Boolean,     default=True)
    created_at  = Column(String(32),  default="")
    expires_at  = Column(String(32),  nullable=True)


class AdminRole(Base):
    """نقش‌های ادمین — هر نقش یه مجموعه دسترسی دارد"""
    __tablename__ = "admin_roles"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    name        = Column(String(64),  unique=True, nullable=False)   # SUPER_ADMIN, USER_MANAGER, ...
    display_name= Column(String(128), default="")
    description = Column(String(256), default="")
    is_active   = Column(Boolean,     default=True)
    created_at  = Column(DateTime,    default=datetime.utcnow)


class Permission(Base):
    """دسترسی‌های اتمیک — فرمت: resource:action"""
    __tablename__ = "permissions"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    code        = Column(String(128), unique=True, nullable=False)  # مثلاً users:write
    resource    = Column(String(64),  nullable=False)               # users, servers, giftcodes, ...
    action      = Column(String(64),  nullable=False)               # read, write, delete, manage
    description = Column(String(256), default="")


class RolePermission(Base):
    """رابطه نقش ↔ دسترسی"""
    __tablename__ = "role_permissions"

    id            = Column(Integer, primary_key=True, autoincrement=True)
    role_id       = Column(Integer, ForeignKey("admin_roles.id"), nullable=False)
    permission_id = Column(Integer, ForeignKey("permissions.id"), nullable=False)


class Admin(Base):
    """ادمین‌های سیستم"""
    __tablename__ = "admins"

    id            = Column(Integer,     primary_key=True, autoincrement=True)
    username      = Column(String(64),  unique=True, nullable=False, index=True)
    password_hash = Column(String(128), nullable=False)
    full_name     = Column(String(128), default="")
    email         = Column(String(128), default="")
    role_id       = Column(Integer,     ForeignKey("admin_roles.id"), nullable=False)
    is_active     = Column(Boolean,     default=True)
    last_login_at = Column(DateTime,    nullable=True)
    created_at    = Column(DateTime,    default=datetime.utcnow)
    created_by    = Column(Integer,     nullable=True)   # id ادمین سازنده


class AdminSession(Base):
    """session های فعال ادمین‌ها"""
    __tablename__ = "admin_sessions"

    id           = Column(Integer,     primary_key=True, autoincrement=True)
    admin_id     = Column(Integer,     ForeignKey("admins.id"), nullable=False)
    token_hash   = Column(String(256), unique=True, nullable=False, index=True)
    ip_address   = Column(String(64),  default="")
    user_agent   = Column(String(256), default="")
    created_at   = Column(DateTime,    default=datetime.utcnow)
    expires_at   = Column(DateTime,    nullable=False)
    is_revoked   = Column(Boolean,     default=False)


class AuditLog(Base):
    """لاگ کامل فعالیت‌های ادمین‌ها"""
    __tablename__ = "audit_logs"

    id          = Column(Integer,     primary_key=True, autoincrement=True)
    admin_id    = Column(Integer,     ForeignKey("admins.id"), nullable=True)
    admin_name  = Column(String(64),  default="")        # snapshot نام در زمان لاگ
    action      = Column(String(128), nullable=False)    # مثلاً user.ban
    resource    = Column(String(64),  default="")        # نوع منبع
    target_id   = Column(String(128), default="")        # id منبع تغییر یافته
    detail      = Column(Text,        default="")        # جزئیات JSON
    ip_address  = Column(String(64),  default="")
    status      = Column(String(16),  default="success") # success | failed
    created_at  = Column(DateTime,    default=datetime.utcnow)


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
        "telegram_support":      "AlphaSupport_ir",
        "telegram_bot_token":    "",
        "telegram_admin_chat_id":"",
        "channel_url":           "https://t.me/AlphaSupport_ir",
        "announcement":          "",
        "app_version":           "1.0.0",
        "latest_version":        "1.0.0",
        "force_update":          "false",
        "update_url":            "",
        "update_changelog":      "",
        "maintenance_mode":      "false",
        "maintenance_message":   "سرویس موقتاً در دسترس نیست",
        "maintenance_since":     "",
        "broadcast_enabled":     "false",
        "broadcast_message":     "",
        "broadcast_type":        "info",
        "broadcast_expires":     "",
    }
    for k, v in defaults.items():
        if not db.query(AppConfig).filter(AppConfig.key == k).first():
            db.add(AppConfig(key=k, value=v))

    # سرور نمونه
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

    # کد هدیه نمونه
    if not db.query(GiftCode).filter(GiftCode.code == "ALPHA5").first():
        db.add(GiftCode(
            code="ALPHA5",
            add_gb=5.0,
            add_days=0,
            description="کد هدیه ۵ گیگابایت",
            is_active=True,
            max_uses=0,
        ))

    db.commit()

    # ─── seed RBAC: نقش‌ها + دسترسی‌ها + SUPER_ADMIN اولیه ──────────
    # اگه هیچ نقشی نداشت، seed کن
    if not db.query(AdminRole).first():
        from app.agents.role_permission_agent import RolePermissionAgent
        RolePermissionAgent(db).execute("seed_defaults", {})

    # اگه هیچ ادمینی نداشت، یه SUPER_ADMIN پیش‌فرض بساز
    if not db.query(Admin).first():
        from app.agents.admin_auth_agent import hash_admin_password
        super_role = db.query(AdminRole).filter(AdminRole.name == "SUPER_ADMIN").first()
        if super_role:
            default_pass = os.getenv("SUPER_ADMIN_PASSWORD", "admin1234")
            admin = Admin(
                username="admin",
                password_hash=hash_admin_password(default_pass),
                full_name="Super Admin",
                email="",
                role_id=super_role.id,
                is_active=True,
            )
            db.add(admin)
            db.commit()
            import logging
            logging.getLogger("alphavpn").warning(
                f"SUPER_ADMIN پیش‌فرض ساخته شد: username=admin password={default_pass} — حتماً رمز رو عوض کن!"
            )
