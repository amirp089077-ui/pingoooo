"""
Alpha VPN - Admin RBAC Router
مدیریت ادمین‌ها، نقش‌ها، session ها، لاگ فعالیت‌ها
همه endpoint ها با Bearer JWT + بررسی permission محافظت می‌شن
"""

from typing import Optional
from fastapi import APIRouter, Depends, Request, HTTPException
from sqlalchemy.orm import Session
from pydantic import BaseModel, Field

from app.database import get_db
from app.agents import (
    AdminUserAgent, RolePermissionAgent,
    AuditLogAgent, SessionAgent, AccessControlAgent,
)
from app.schemas import GenericResponse

router = APIRouter(prefix="/api/admin", tags=["Admin RBAC"])


# ─── helpers ────────────────────────────────────────────────────────────

def _resp(result) -> dict:
    if not result.success:
        code = result.error_code or ""
        http = 404 if code == "NOT_FOUND" else 409 if code == "DUPLICATE" else 400
        raise HTTPException(status_code=http, detail=result.message)
    return {"success": True, "message": result.message, "data": result.data}


def _require(permission: str):
    """Dependency — بررسی توکن + دسترسی"""
    def dep(request: Request, db: Session = Depends(get_db)):
        header = request.headers.get("Authorization", "")
        if not header.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="توکن ارسال نشده")
        token  = header.removeprefix("Bearer ").strip()
        result = AccessControlAgent(db).execute("check", {
            "token":      token,
            "permission": permission,
        })
        if not result.success:
            code = result.error_code or ""
            http = 401 if code in ("INVALID_TOKEN", "TOKEN_EXPIRED", "MISSING_TOKEN", "SESSION_REVOKED") else 403
            raise HTTPException(status_code=http, detail=result.message)
        request.state.admin = result.data
        return result.data
    return dep


def _get_ip(request: Request) -> str:
    return request.client.host if request.client else ""


# ════════════════════════════════════════════════════════════════════════
#  مدیریت ادمین‌ها  /api/admin/admins
# ════════════════════════════════════════════════════════════════════════

class CreateAdminRequest(BaseModel):
    username:  str   = Field(..., min_length=1, max_length=64)
    password:  str   = Field(..., min_length=6, max_length=128)
    full_name: str   = ""
    email:     str   = ""
    role:      str   = "SUPPORT"


class UpdateAdminRequest(BaseModel):
    full_name: Optional[str] = None
    email:     Optional[str] = None
    role:      Optional[str] = None


@router.get("/admins", dependencies=[Depends(_require("admins:read"))])
def list_admins(
    role: Optional[str] = None,
    active_only: bool = False,
    request: Request = None,
    db: Session = Depends(get_db),
):
    result = AdminUserAgent(db).execute("list", {"role": role, "active_only": active_only})
    return _resp(result)


@router.get("/admins/{admin_id}", dependencies=[Depends(_require("admins:read"))])
def get_admin(admin_id: int, db: Session = Depends(get_db)):
    result = AdminUserAgent(db).execute("get", {"admin_id": admin_id})
    return _resp(result)


@router.post("/admins")
def create_admin(
    body: CreateAdminRequest,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    result = AdminUserAgent(db).execute("create", {
        **body.model_dump(),
        "created_by":   caller["admin_id"],
        "creator_name": caller["username"],
        "ip_address":   _get_ip(request),
    })
    return _resp(result)


@router.patch("/admins/{admin_id}")
def update_admin(
    admin_id: int,
    body: UpdateAdminRequest,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    payload = body.model_dump(exclude_none=True)
    payload["admin_id"]   = admin_id
    payload["ip_address"] = _get_ip(request)
    result = AdminUserAgent(db).execute("update", payload)
    AuditLogAgent(db).execute("log", {
        "admin_id":   caller["admin_id"],
        "admin_name": caller["username"],
        "action":     "admin.update_admin",
        "resource":   "admins",
        "target_id":  str(admin_id),
        "detail":     str(payload),
        "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.delete("/admins/{admin_id}")
def delete_admin(
    admin_id: int,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:delete")),
):
    result = AdminUserAgent(db).execute("delete", {
        "admin_id":    admin_id,
        "deleted_by":  caller["admin_id"],
        "deleter_name":caller["username"],
        "ip_address":  _get_ip(request),
    })
    return _resp(result)


@router.post("/admins/{admin_id}/activate")
def activate_admin(
    admin_id: int,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    result = AdminUserAgent(db).execute("activate", {"admin_id": admin_id})
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "admin.activate", "resource": "admins",
        "target_id": str(admin_id), "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.post("/admins/{admin_id}/deactivate")
def deactivate_admin(
    admin_id: int,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    result = AdminUserAgent(db).execute("deactivate", {"admin_id": admin_id})
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "admin.deactivate", "resource": "admins",
        "target_id": str(admin_id), "ip_address": _get_ip(request),
    })
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  مدیریت نقش‌ها  /api/admin/roles
# ════════════════════════════════════════════════════════════════════════

class CreateRoleRequest(BaseModel):
    name:         str
    display_name: str = ""
    description:  str = ""
    permissions:  list = []


class UpdateRoleRequest(BaseModel):
    display_name: Optional[str] = None
    description:  Optional[str] = None
    is_active:    Optional[bool] = None


class AssignPermissionRequest(BaseModel):
    permissions: list[str]


@router.get("/roles", dependencies=[Depends(_require("admins:manage_roles"))])
def list_roles(db: Session = Depends(get_db)):
    result = RolePermissionAgent(db).execute("list_roles", {})
    return _resp(result)


@router.get("/roles/{role_name}", dependencies=[Depends(_require("admins:manage_roles"))])
def get_role(role_name: str, db: Session = Depends(get_db)):
    result = RolePermissionAgent(db).execute("get_role", {"name": role_name})
    return _resp(result)


@router.post("/roles")
def create_role(
    body: CreateRoleRequest,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    result = RolePermissionAgent(db).execute("create_role", body.model_dump())
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "role.create", "resource": "roles",
        "target_id": body.name, "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.patch("/roles/{role_name}")
def update_role(
    role_name: str,
    body: UpdateRoleRequest,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    payload = body.model_dump(exclude_none=True)
    payload["name"] = role_name
    result = RolePermissionAgent(db).execute("update_role", payload)
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "role.update", "resource": "roles",
        "target_id": role_name, "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.delete("/roles/{role_name}")
def delete_role(
    role_name: str,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    result = RolePermissionAgent(db).execute("delete_role", {"name": role_name})
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "role.delete", "resource": "roles",
        "target_id": role_name, "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.post("/roles/{role_name}/permissions")
def assign_permission_to_role(
    role_name: str,
    body: AssignPermissionRequest,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    result = RolePermissionAgent(db).execute("assign_permission", {
        "role_name":   role_name,
        "permissions": body.permissions,
    })
    AuditLogAgent(db).execute("log", {
        "admin_id": caller["admin_id"], "admin_name": caller["username"],
        "action": "role.assign_permissions", "resource": "roles",
        "target_id": role_name, "detail": str(body.permissions),
        "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.delete("/roles/{role_name}/permissions/{perm_code}")
def revoke_permission_from_role(
    role_name: str,
    perm_code: str,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    result = RolePermissionAgent(db).execute("revoke_permission", {
        "role_name":  role_name,
        "permission": perm_code,
    })
    return _resp(result)


@router.get("/permissions", dependencies=[Depends(_require("admins:manage_roles"))])
def list_permissions(resource: Optional[str] = None, db: Session = Depends(get_db)):
    result = RolePermissionAgent(db).execute("list_permissions", {"resource": resource})
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  مدیریت Session ها  /api/admin/sessions
# ════════════════════════════════════════════════════════════════════════

@router.get("/sessions", dependencies=[Depends(_require("admins:read"))])
def list_sessions(
    admin_id: Optional[int] = None,
    active_only: bool = True,
    db: Session = Depends(get_db),
):
    action = "list_active" if active_only else "list_all"
    result = SessionAgent(db).execute(action, {"admin_id": admin_id})
    return _resp(result)


@router.get("/sessions/stats", dependencies=[Depends(_require("admins:read"))])
def session_stats(db: Session = Depends(get_db)):
    result = SessionAgent(db).execute("stats", {})
    return _resp(result)


@router.delete("/sessions/{session_id}")
def revoke_session(
    session_id: int,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    result = SessionAgent(db).execute("revoke", {
        "session_id": session_id,
        "revoked_by": caller["admin_id"],
        "admin_name": caller["username"],
        "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.delete("/sessions/admin/{target_admin_id}")
def revoke_all_sessions(
    target_admin_id: int,
    request: Request,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:write")),
):
    result = SessionAgent(db).execute("revoke_all", {
        "admin_id":   target_admin_id,
        "revoked_by": caller["admin_id"],
        "admin_name": caller["username"],
        "ip_address": _get_ip(request),
    })
    return _resp(result)


@router.post("/sessions/cleanup", dependencies=[Depends(_require("admins:manage_roles"))])
def cleanup_sessions(days_old: int = 7, db: Session = Depends(get_db)):
    result = SessionAgent(db).execute("cleanup", {"days_old": days_old})
    return _resp(result)


# ════════════════════════════════════════════════════════════════════════
#  لاگ فعالیت‌ها  /api/admin/audit
# ════════════════════════════════════════════════════════════════════════

@router.get("/audit", dependencies=[Depends(_require("audit:read"))])
def list_audit_logs(
    admin_id:  Optional[int] = None,
    resource:  Optional[str] = None,
    status:    Optional[str] = None,
    days:      Optional[int] = None,
    page:      int = 1,
    per_page:  int = 50,
    db: Session = Depends(get_db),
):
    result = AuditLogAgent(db).execute("list", {
        "admin_id": admin_id, "resource": resource,
        "status": status, "days": days,
        "page": page, "per_page": per_page,
    })
    return _resp(result)


@router.get("/audit/stats", dependencies=[Depends(_require("audit:read"))])
def audit_stats(days: int = 30, db: Session = Depends(get_db)):
    result = AuditLogAgent(db).execute("stats", {"days": days})
    return _resp(result)


@router.get("/audit/search", dependencies=[Depends(_require("audit:read"))])
def search_audit_logs(keyword: str, per_page: int = 50, db: Session = Depends(get_db)):
    result = AuditLogAgent(db).execute("search", {"keyword": keyword, "per_page": per_page})
    return _resp(result)


@router.get("/audit/{log_id}", dependencies=[Depends(_require("audit:read"))])
def get_audit_log(log_id: int, db: Session = Depends(get_db)):
    result = AuditLogAgent(db).execute("get", {"log_id": log_id})
    return _resp(result)


@router.post("/audit/purge")
def purge_audit_logs(
    older_than_days: int = 90,
    request: Request = None,
    db: Session = Depends(get_db),
    caller=Depends(_require("admins:manage_roles")),
):
    result = AuditLogAgent(db).execute("purge", {
        "older_than_days": older_than_days,
        "admin_id":        caller["admin_id"],
        "admin_name":      caller["username"],
        "ip_address":      _get_ip(request),
    })
    return _resp(result)
