from functools import lru_cache

from app.core.config import get_settings
from app.services.model_service import FraudModelService


@lru_cache
def get_model_service() -> FraudModelService:
    return FraudModelService(get_settings().model_path)
