"""
Alpha VPN - Base Agent
کلاس پایه‌ای که همه ایجنت‌ها ازش ارث می‌برن
هر ایجنت یه حوزه مسئولیت مشخص داره و با DB کار می‌کنه
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, Optional
from sqlalchemy.orm import Session
import logging

logger = logging.getLogger("alphavpn.agent")


class AgentResult:
    """نتیجه استاندارد همه ایجنت‌ها"""

    def __init__(
        self,
        success: bool,
        message: str,
        data: Optional[Any] = None,
        error_code: Optional[str] = None,
    ):
        self.success = success
        self.message = message
        self.data = data
        self.error_code = error_code

    def to_dict(self) -> Dict:
        result = {"success": self.success, "message": self.message}
        if self.data is not None:
            result["data"] = self.data
        if self.error_code:
            result["error_code"] = self.error_code
        return result

    @classmethod
    def ok(cls, message: str, data: Any = None) -> "AgentResult":
        return cls(success=True, message=message, data=data)

    @classmethod
    def fail(cls, message: str, error_code: str = "AGENT_ERROR") -> "AgentResult":
        return cls(success=False, message=message, error_code=error_code)


class BaseAgent(ABC):
    """
    کلاس پایه ایجنت‌ها
    هر ایجنت باید:
      - یه name داشته باشه
      - متد execute رو پیاده‌سازی کنه
    """

    name: str = "BaseAgent"
    description: str = ""

    def __init__(self, db: Session):
        self.db = db
        self._log = logging.getLogger(f"alphavpn.agent.{self.name}")

    def log(self, msg: str, level: str = "info"):
        getattr(self._log, level)(f"[{self.name}] {msg}")

    @abstractmethod
    def execute(self, action: str, payload: Dict) -> AgentResult:
        """
        هر ایجنت باید این متد رو پیاده‌سازی کنه.
        action: عملیاتی که باید انجام بشه (مثلاً 'create', 'delete', ...)
        payload: داده‌های ورودی
        """
        ...

    def __repr__(self) -> str:
        return f"<Agent: {self.name}>"
