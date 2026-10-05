#!/usr/bin/env python3
import json
import urllib.request
import urllib.error

BASE = "http://localhost"
ADMIN_KEY = "GvYPcMmIAFTx49gHE0seVpln15KJNWbB"

def post(path, body, headers={}):
    h = {"Content-Type": "application/json"}
    h.update(headers)
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode("utf-8"),
        headers=h,
        method="POST"
    )
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())

users = [
    {
        "username": "ali",
        "password": "ali1234",
        "plan_type": "MULTI_USER",
        "total_quota_gb": 50,
        "remaining_days": 30,
        "max_devices": 2,
        "expiry_date": "1404/07/30"
    },
    {
        "username": "sara",
        "password": "sara5678",
        "plan_type": "SINGLE_USER",
        "total_quota_gb": 20,
        "remaining_days": 60,
        "max_devices": 1,
        "expiry_date": "1404/09/30"
    },
    {
        "username": "testadmin",
        "password": "Admin@9999",
        "plan_type": "MULTI_USER",
        "total_quota_gb": 100,
        "remaining_days": 365,
        "max_devices": 2,
        "expiry_date": "1405/07/30"
    },
]

print("ساخت کاربران...\n")
for u in users:
    status, body = post(
        "/api/admin/users",
        u,
        {"x-admin-key": ADMIN_KEY}
    )
    icon = "✅" if status == 200 else "❌"
    msg = body.get("message", body.get("detail", str(body)))
    print(f"{icon} {u['username']:15} | {status} | {msg}")

print("\nتست login...\n")
for u in users:
    status, body = post("/api/auth/login", {
        "username": u["username"],
        "password": u["password"]
    })
    icon = "✅" if status == 200 else "❌"
    if status == 200:
        msg = f"token={body['token'][:25]}... | remaining={body['remaining_gb']}GB"
    else:
        msg = body.get("detail", str(body))
    print(f"{icon} {u['username']:15} | {msg}")
