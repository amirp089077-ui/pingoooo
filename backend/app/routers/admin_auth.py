"""
Alpha VPN - Admin Auth Router
لاگین، refresh، logout، تغییر رمز ادمین‌ها
بدون نیاز به X-Admin-Key — از JWT استفاده می‌کنه
"""

from fastapi import APIRouter, Depends, Request
from sqlalchemy.orm import Session
from pydantic import BaseModel, Field
from typing import Optional

from app.database import get_db
from app.agents import AdminAuthAgent, RolePermissionAgent
from app.schemas import GenericResponse

router = APIRouter(prefix="/api/admin/auth", tags=["Admin Auth"])


# ─── Request schemas ────────────────────────────────────────────────────

class AdminLoginRequest(BaseModel):
    username: str = Field(..., min_length=1)
    password: str = Field(..., min_length=1)


class AdminRefreshRequest(BaseModel):
    token: str


class AdminChangePasswordRequest(BaseModel):
    old_password: str = Field(..., min_length=1)
    new_password: str = Field(..., min_length=6)


def _resp(result) -> dict:
    if not result.success:
        from fastapi import HTTPException
        raise HTTPException(status_code=400, detail=result.message)
    return {"success": True, "message": result.message, "data": result.data}


def _get_token(request: Request) -> str:
    header = request.headers.get("Authorization", "")
    return header.removeprefix("Bearer ").strip() if header.startswith("Bearer ") else ""


# ─── Endpoints ──────────────────────────────────────────────────────────

@router.post("/login")
def admin_login(body: AdminLoginRequest, request: Request, db: Session = Depends(get_db)):
    """لاگین ادمین — برمی‌گردونه JWT + نقش + دسترسی‌ها"""
    result = AdminAuthAgent(db).execute("login", {
        "username":   body.username,
        "password":   body.password,
        "ip_address": request.client.host if request.client else "",
        "user_agent": request.headers.get("User-Agent", ""),
    })
    return _resp(result)


@router.post("/refresh")
def admin_refresh(body: AdminRefreshRequest, request: Request, db: Session = Depends(get_db)):
    """تجدید توکن"""
    result = AdminAuthAgent(db).execute("refresh", {
        "token":      body.token,
        "ip_address": request.client.host if request.client else "",
    })
    return _resp(result)


@router.post("/logout")
def admin_logout(request: Request, db: Session = Depends(get_db)):
    """خروج — باطل کردن توکن جاری"""
    result = AdminAuthAgent(db).execute("logout", {
        "token":      _get_token(request),
        "ip_address": request.client.host if request.client else "",
    })
    return _resp(result)


@router.post("/logout-all")
def admin_logout_all(request: Request, db: Session = Depends(get_db)):
    """خروج از همه دستگاه‌ها"""
    token = _get_token(request)
    from app.agents import AccessControlAgent
    auth = AccessControlAgent(db).execute("get_admin_from_token", {"token": token})
    if not auth.success:
        from fastapi import HTTPException
        raise HTTPException(status_code=401, detail=auth.message)
    result = AdminAuthAgent(db).execute("logout_all", {"admin_id": auth.data["admin_id"]})
    return _resp(result)


@router.post("/change-password")
def admin_change_password(
    body: AdminChangePasswordRequest,
    request: Request,
    db: Session = Depends(get_db),
):
    """تغییر رمز عبور"""
    token = _get_token(request)
    from app.agents import AccessControlAgent
    auth = AccessControlAgent(db).execute("get_admin_from_token", {"token": token})
    if not auth.success:
        from fastapi import HTTPException
        raise HTTPException(status_code=401, detail=auth.message)
    result = AdminAuthAgent(db).execute("change_password", {
        "admin_id":     auth.data["admin_id"],
        "old_password": body.old_password,
        "new_password": body.new_password,
        "ip_address":   request.client.host if request.client else "",
    })
    return _resp(result)


@router.get("/me")
def admin_me(request: Request, db: Session = Depends(get_db)):
    """پروفایل ادمین لاگین‌شده"""
    token = _get_token(request)
    from app.agents import AccessControlAgent, AdminUserAgent
    auth = AccessControlAgent(db).execute("get_admin_from_token", {"token": token})
    if not auth.success:
        from fastapi import HTTPException
        raise HTTPException(status_code=401, detail=auth.message)
    result = AdminUserAgent(db).execute("get", {"admin_id": auth.data["admin_id"]})
    return _resp(result)


@router.post("/seed-rbac")
def seed_rbac(request: Request, db: Session = Depends(get_db)):
    """
    seed کردن دسترسی‌ها و نقش‌های پیش‌فرض.
    فقط یه‌بار اجرا کن — بعد از اجرا، این endpoint رو غیرفعال کن.
    """
    result = RolePermissionAgent(db).execute("seed_defaults", {})
    return _resp(result)
