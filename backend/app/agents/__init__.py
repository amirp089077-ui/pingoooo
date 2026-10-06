"""
Alpha VPN - Agents Package
ایجنت‌های اصلی + ایجنت‌های RBAC ادمین
"""

# ─── ایجنت‌های اصلی (مدیریت محتوا) ───────────────────────────────────
from app.agents.user_agent         import UserManagementAgent
from app.agents.server_agent       import ServerManagementAgent
from app.agents.giftcode_agent     import GiftCodeAgent
from app.agents.analytics_agent    import AnalyticsAgent
from app.agents.notification_agent import NotificationAgent

# ─── ایجنت‌های RBAC ادمین ─────────────────────────────────────────────
from app.agents.admin_auth_agent      import AdminAuthAgent
from app.agents.role_permission_agent import RolePermissionAgent
from app.agents.admin_user_agent      import AdminUserAgent
from app.agents.audit_log_agent       import AuditLogAgent
from app.agents.access_control_agent  import AccessControlAgent, require_permission
from app.agents.session_agent         import SessionAgent

__all__ = [
    # اصلی
    "UserManagementAgent",
    "ServerManagementAgent",
    "GiftCodeAgent",
    "AnalyticsAgent",
    "NotificationAgent",
    # RBAC
    "AdminAuthAgent",
    "RolePermissionAgent",
    "AdminUserAgent",
    "AuditLogAgent",
    "AccessControlAgent",
    "SessionAgent",
    # dependency
    "require_permission",
]
