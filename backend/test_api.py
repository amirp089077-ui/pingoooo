#!/usr/bin/env python3
"""تست کامل API - اجرا روی سرور"""

import urllib.request
import urllib.error
import json
import sys

BASE = "http://localhost:8000"
ADMIN_KEY = "GvYPcMmIAFTx49gHE0seVpln15KJNWbB"

def req(method, path, body=None, headers=None):
    url = BASE + path
    h = {"Content-Type": "application/json"}
    if headers:
        h.update(headers)
    data = json.dumps(body).encode() if body else None
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"raw": raw.decode(errors="replace")}

ok = True

# 1. Health check
s, b = req("GET", "/")
assert s == 200 and b["status"] == "ok", f"FAIL health: {b}"
print(f"✅  GET /               → {b['status']}")

# 2. Config
s, b = req("GET", "/api/config")
assert s == 200, f"FAIL config: {b}"
print(f"✅  GET /api/config     → announcement='{b['announcement']}'")

# 3. Create user (admin)
s, b = req("POST", "/api/admin/users",
    body={"username":"testuser","password":"test1234",
          "plan_type":"MULTI_USER","total_quota_gb":50,
          "remaining_days":30,"max_devices":2},
    headers={"x-admin-key": ADMIN_KEY})
assert s == 200, f"FAIL create_user: {b}"
print(f"✅  POST /api/admin/users → {b['message']}")

# 4. Login با رمز اشتباه
s, b = req("POST", "/api/auth/login",
    body={"username":"testuser","password":"WRONG"})
assert s == 401, f"FAIL wrong_pass: {b}"
print(f"✅  POST /api/auth/login (wrong pw) → 401 ✓")

# 5. Login صحیح
s, b = req("POST", "/api/auth/login",
    body={"username":"testuser","password":"test1234"})
assert s == 200, f"FAIL login: {b}"
token = b["token"]
print(f"✅  POST /api/auth/login → token={token[:20]}...")

# 6. دریافت پروفایل
s, b = req("GET", "/api/users/me",
    headers={"Authorization": f"Bearer {token}"})
assert s == 200 and b["username"] == "testuser", f"FAIL profile: {b}"
print(f"✅  GET /api/users/me   → remaining_gb={b['remaining_gb']}")

# 7. دریافت سرورها
s, b = req("GET", "/api/servers",
    headers={"Authorization": f"Bearer {token}"})
assert s == 200, f"FAIL servers: {b}"
print(f"✅  GET /api/servers    → {len(b['servers'])} سرور")

# 8. آپدیت مصرف
s, b = req("PATCH", "/api/users/me/usage",
    body={"used_gb": 1.5, "remaining_gb": 48.5},
    headers={"Authorization": f"Bearer {token}"})
assert s == 200, f"FAIL usage: {b}"
print(f"✅  PATCH /api/users/me/usage → {b['message']}")

# 9. Gift code
s, b = req("POST", "/api/giftcodes/redeem",
    body={"code": "ALPHA5"},
    headers={"Authorization": f"Bearer {token}"})
assert s == 200, f"FAIL giftcode: {b}"
print(f"✅  POST /api/giftcodes/redeem → +{b['bonus_gb']}GB")

# 10. Logout
s, b = req("POST", "/api/auth/logout",
    headers={"Authorization": f"Bearer {token}"})
assert s == 200, f"FAIL logout: {b}"
print(f"✅  POST /api/auth/logout → {b['message']}")

# 11. پاک کردن یوزر تست
s, b = req("DELETE", "/api/admin/users/testuser",
    headers={"x-admin-key": ADMIN_KEY})
assert s == 200, f"FAIL delete_user: {b}"
print(f"✅  DELETE /api/admin/users/testuser → {b['message']}")

print("\n🎉  همه تست‌ها پاس شدن — API آماده‌ست!")
