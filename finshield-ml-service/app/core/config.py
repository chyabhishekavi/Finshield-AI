from __future__ import annotations

import os
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path


@dataclass(frozen=True, slots=True)
class Settings:
    service_name: str
    service_version: str
    model_path: Path


@lru_cache
def get_settings() -> Settings:
    return Settings(
        service_name=os.getenv("SERVICE_NAME", "finshield-ml-service"),
        service_version=os.getenv("SERVICE_VERSION", "0.1.0"),
        model_path=Path(os.getenv("MODEL_PATH", "models/fraud_model.joblib")),
    )
