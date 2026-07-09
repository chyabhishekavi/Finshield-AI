from fastapi import APIRouter, Depends

from app.core.config import Settings, get_settings
from app.dependencies import get_model_service
from app.schemas.health import HealthResponse
from app.services.model_service import FraudModelService

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health(
    settings: Settings = Depends(get_settings),
    model_service: FraudModelService = Depends(get_model_service),
) -> HealthResponse:
    return HealthResponse(
        status="ok" if model_service.model_loaded else "degraded",
        service=settings.service_name,
        version=settings.service_version,
        modelLoaded=model_service.model_loaded,
        modelVersion=model_service.model_version,
    )
