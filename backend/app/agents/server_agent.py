"""
Alpha VPN - Agent #2: ServerManagementAgent
مسئولیت: مدیریت کامل سرورهای VPN
عملیات: create, update, delete, toggle_active, update_ping,
         reorder, bulk_import, get, list, list_inactive,
         update_config, get_config, list_all
"""

from typing import Dict, List

from app.agents.base import BaseAgent, AgentResult
from app.database import Server, ServerConfig


class ServerManagementAgent(BaseAgent):
    name = "ServerManagementAgent"
    description = "مدیریت سرورهای VPN — ایجاد، ویرایش، فعال/غیرفعال، ترتیب‌بندی"

    def execute(self, action: str, payload: Dict) -> AgentResult:
        self.log(f"action={action} payload_keys={list(payload.keys())}")
        dispatch = {
            "create":        self._create_server,
            "update":        self._update_server,
            "delete":        self._delete_server,
            "toggle_active": self._toggle_active,
            "update_ping":   self._update_ping,
            "reorder":       self._reorder,
            "bulk_import":   self._bulk_import,
            "get":           self._get_server,
            "list":          self._list_servers,
            "list_inactive": self._list_inactive,
            "list_all":      self._list_all,
            "update_config": self._update_config,
            "get_config":    self._get_config,
        }
        handler = dispatch.get(action)
        if not handler:
            return AgentResult.fail(f"عملیات '{action}' پشتیبانی نمی‌شود", "UNKNOWN_ACTION")
        try:
            return handler(payload)
        except Exception as exc:
            self.log(f"خطا در {action}: {exc}", "error")
            self.db.rollback()
            return AgentResult.fail(f"خطای داخلی: {exc}", "INTERNAL_ERROR")

    # ─────────────────── Handlers ───────────────────────────

    def _create_server(self, p: Dict) -> AgentResult:
        server_id = p.get("id", "").strip()
        host      = p.get("host", "").strip()
        country   = p.get("country", "").strip()
        city      = p.get("city", "").strip()

        if not all([server_id, host, country, city]):
            return AgentResult.fail("id، host، country و city الزامی هستند", "VALIDATION_ERROR")

        if self.db.query(Server).filter(Server.id == server_id).first():
            return AgentResult.fail(f"سرور '{server_id}' از قبل وجود دارد", "DUPLICATE")

        max_order = self.db.query(Server).count()
        server = Server(
            id=server_id,
            country=country,
            city=city,
            flag=p.get("flag", "🌐"),
            host=host,
            port=int(p.get("port", 443)),
            badge=p.get("badge"),
            emoji=p.get("emoji", ""),
            is_pro=bool(p.get("is_pro", False)),
            is_active=True,
            config_uri=p.get("config_uri", ""),
            ping=int(p.get("ping", 50)),
            order_idx=int(p.get("order_idx", max_order + 1)),
        )
        self.db.add(server)
        self.db.commit()
        self.log(f"سرور {server_id} ساخته شد")
        return AgentResult.ok(f"سرور '{server_id}' با موفقیت ساخته شد", self._serialize(server))

    def _update_server(self, p: Dict) -> AgentResult:
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")

        updatable = ["country", "city", "flag", "host", "port", "badge",
                     "emoji", "is_pro", "config_uri", "ping", "order_idx"]
        for field in updatable:
            if field in p and p[field] is not None:
                setattr(server, field, p[field])

        self.db.commit()
        return AgentResult.ok(f"سرور '{server.id}' به‌روز شد", self._serialize(server))

    def _delete_server(self, p: Dict) -> AgentResult:
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")
        sid = server.id
        self.db.delete(server)
        self.db.commit()
        return AgentResult.ok(f"سرور '{sid}' حذف شد")

    def _toggle_active(self, p: Dict) -> AgentResult:
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")

        if "is_active" in p:
            server.is_active = bool(p["is_active"])
        else:
            server.is_active = not server.is_active

        self.db.commit()
        state = "فعال" if server.is_active else "غیرفعال"
        return AgentResult.ok(f"سرور '{server.id}' {state} شد", {"is_active": server.is_active})

    def _update_ping(self, p: Dict) -> AgentResult:
        updates: List[Dict] = p.get("updates", [])
        if not updates:
            server = self._find(p.get("id", ""))
            if not server:
                return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")
            server.ping = int(p.get("ping", 50))
            self.db.commit()
            return AgentResult.ok(f"ping سرور '{server.id}' = {server.ping}ms")

        updated = []
        for item in updates:
            s = self._find(item.get("id", ""))
            if s:
                s.ping = int(item.get("ping", 50))
                updated.append(s.id)
        self.db.commit()
        return AgentResult.ok(f"ping {len(updated)} سرور آپدیت شد", {"updated": updated})

    def _reorder(self, p: Dict) -> AgentResult:
        order: List[str] = p.get("order", [])
        if not order:
            return AgentResult.fail("لیست ترتیب خالی است", "VALIDATION_ERROR")

        for idx, sid in enumerate(order, start=1):
            server = self._find(sid)
            if server:
                server.order_idx = idx

        self.db.commit()
        return AgentResult.ok(f"{len(order)} سرور مرتب‌سازی شد")

    def _bulk_import(self, p: Dict) -> AgentResult:
        servers_data: List[Dict] = p.get("servers", [])
        if not servers_data:
            return AgentResult.fail("لیست سرورها خالی است", "VALIDATION_ERROR")

        created, skipped = [], []
        base_order = self.db.query(Server).count()

        for idx, sd in enumerate(servers_data):
            sid = sd.get("id", "").strip()
            if not sid or self.db.query(Server).filter(Server.id == sid).first():
                skipped.append(sid or "(بدون id)")
                continue

            self.db.add(Server(
                id=sid,
                country=sd.get("country", "نامشخص"),
                city=sd.get("city", "نامشخص"),
                flag=sd.get("flag", "🌐"),
                host=sd.get("host", ""),
                port=int(sd.get("port", 443)),
                badge=sd.get("badge"),
                emoji=sd.get("emoji", ""),
                is_pro=bool(sd.get("is_pro", False)),
                is_active=True,
                config_uri=sd.get("config_uri", ""),
                ping=int(sd.get("ping", 50)),
                order_idx=base_order + idx + 1,
            ))
            created.append(sid)

        self.db.commit()
        return AgentResult.ok(
            f"{len(created)} سرور درج شد، {len(skipped)} رد شد",
            {"created": created, "skipped": skipped}
        )

    def _get_server(self, p: Dict) -> AgentResult:
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")
        return AgentResult.ok("اطلاعات سرور", self._serialize(server))

    def _list_servers(self, p: Dict) -> AgentResult:
        servers = (
            self.db.query(Server)
            .filter(Server.is_active == True)
            .order_by(Server.order_idx.asc())
            .all()
        )
        return AgentResult.ok(f"{len(servers)} سرور فعال", [self._serialize(s) for s in servers])

    def _list_inactive(self, p: Dict) -> AgentResult:
        servers = (
            self.db.query(Server)
            .filter(Server.is_active == False)
            .order_by(Server.order_idx.asc())
            .all()
        )
        return AgentResult.ok(f"{len(servers)} سرور غیرفعال", [self._serialize(s) for s in servers])

    def _list_all(self, p: Dict) -> AgentResult:
        """همه سرورها — فعال و غیرفعال"""
        servers = self.db.query(Server).order_by(Server.order_idx.asc()).all()
        return AgentResult.ok(f"{len(servers)} سرور", [self._serialize(s) for s in servers])

    def _update_config(self, p: Dict) -> AgentResult:
        """
        آپدیت کانفیگ اتصال VPN یه سرور.
        می‌تونه config_uri مستقیم ست بشه یا از ServerConfig جزئیات بگیره.
        """
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")

        # آپدیت config_uri سریع
        if "config_uri" in p:
            server.config_uri = p["config_uri"].strip()
            self.db.commit()

        # آپدیت جزئیات کانفیگ در جدول ServerConfig
        cfg_data = p.get("config", {})
        if cfg_data:
            cfg = self.db.query(ServerConfig).filter(ServerConfig.server_id == server.id).first()
            if not cfg:
                cfg = ServerConfig(server_id=server.id)
                self.db.add(cfg)

            updatable_cfg = [
                "protocol", "network", "security", "sni",
                "alpn", "fingerprint", "public_key",
                "short_id", "path", "grpc_service_name",
                "obfs_type", "obfs_password",
                "vmess_id", "vless_id", "trojan_password",
                "ss_method", "ss_password",
                "extra_params", "raw_config",
            ]
            for field in updatable_cfg:
                if field in cfg_data and cfg_data[field] is not None:
                    setattr(cfg, field, cfg_data[field])

            self.db.commit()

        return AgentResult.ok(
            f"کانفیگ سرور '{server.id}' آپدیت شد",
            self._serialize_full(server)
        )

    def _get_config(self, p: Dict) -> AgentResult:
        """خواندن کانفیگ کامل یه سرور"""
        server = self._find(p.get("id", ""))
        if not server:
            return AgentResult.fail("سرور یافت نشد", "NOT_FOUND")
        return AgentResult.ok("کانفیگ سرور", self._serialize_full(server))

    # ─────────────────── Helpers ────────────────────────────

    def _find(self, server_id: str):
        return self.db.query(Server).filter(Server.id == server_id.strip()).first()

    def _serialize(self, s: Server) -> Dict:
        return {
            "id":         s.id,
            "country":    s.country,
            "city":       s.city,
            "flag":       s.flag,
            "host":       s.host,
            "port":       s.port,
            "badge":      s.badge,
            "emoji":      s.emoji,
            "is_pro":     s.is_pro,
            "is_active":  s.is_active,
            "config_uri": s.config_uri,
            "ping":       s.ping,
            "order_idx":  s.order_idx,
        }

    def _serialize_full(self, s: Server) -> Dict:
        """سریالایز کامل با کانفیگ جزئیات"""
        data = self._serialize(s)
        cfg  = self.db.query(ServerConfig).filter(ServerConfig.server_id == s.id).first()
        data["config"] = self._serialize_cfg(cfg) if cfg else {}
        return data

    def _serialize_cfg(self, cfg: "ServerConfig") -> Dict:
        return {
            "protocol":         cfg.protocol,
            "network":          cfg.network,
            "security":         cfg.security,
            "sni":              cfg.sni,
            "alpn":             cfg.alpn,
            "fingerprint":      cfg.fingerprint,
            "public_key":       cfg.public_key,
            "short_id":         cfg.short_id,
            "path":             cfg.path,
            "grpc_service_name":cfg.grpc_service_name,
            "obfs_type":        cfg.obfs_type,
            "obfs_password":    cfg.obfs_password,
            "vmess_id":         cfg.vmess_id,
            "vless_id":         cfg.vless_id,
            "trojan_password":  cfg.trojan_password,
            "ss_method":        cfg.ss_method,
            "ss_password":      cfg.ss_password,
            "extra_params":     cfg.extra_params,
            "raw_config":       cfg.raw_config,
        }
