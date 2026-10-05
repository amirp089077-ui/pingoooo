"""
Alpha VPN Backend — FastAPI
"""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.database import create_tables, SessionLocal, seed_default_data
from app.routers import auth, users, servers, config, giftcodes, admin


# ─────────────────── Startup ────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    # جداول بساز و داده اولیه بریز
    create_tables()
    db = SessionLocal()
    try:
        seed_default_data(db)
    finally:
        db.close()
    yield


# ─────────────────── App ────────────────────────

app = FastAPI(
    title="Alpha VPN API",
    version="1.0.0",
    description="Backend سرور شخصی Alpha VPN",
    docs_url="/docs",       # Swagger UI
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS — فقط برای تست. در production محدود کن
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─────────────────── Routers ────────────────────

app.include_router(auth.router)
app.include_router(users.router)
app.include_router(servers.router)
app.include_router(config.router)
app.include_router(giftcodes.router)
app.include_router(admin.router)


# ─────────────────── Health ─────────────────────

@app.get("/", tags=["Health"])
def root():
    return {"status": "ok", "service": "Alpha VPN API", "version": "1.0.0"}


@app.get("/health", tags=["Health"])
def health():
    return {"status": "healthy"}
