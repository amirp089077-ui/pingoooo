"""
Alpha VPN - Servers Router
GET /api/servers  → لیست سرورها (نیاز به token)
"""

from typing import List
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.database import get_db, Server
from app.auth import get_current_user
from app.schemas import ServersResponse, ServerItem

router = APIRouter(prefix="/api/servers", tags=["Servers"])


@router.get("", response_model=ServersResponse)
def get_servers(
    _: str = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    servers = (
        db.query(Server)
        .filter(Server.is_active == True)
        .order_by(Server.order_idx.asc())
        .all()
    )

    items: List[ServerItem] = [
        ServerItem(
            id=s.id,
            country=s.country,
            city=s.city,
            flag=s.flag,
            host=s.host,
            port=s.port,
            badge=s.badge,
            emoji=s.emoji or "",
            is_pro=s.is_pro,
            config_uri=s.config_uri or "",
            ping=s.ping,
        )
        for s in servers
    ]

    return ServersResponse(servers=items)
