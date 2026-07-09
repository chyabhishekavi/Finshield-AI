from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from collections.abc import AsyncIterator

from fastapi import FastAPI

from app.api.routes.health import router as health_router
from app.api.routes.prediction import router as prediction_router
from app.core.config import get_settings
from app.dependencies import get_model_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    get_model_service()
    yield


settings = get_settings()
app = FastAPI(
    title="FinShield ML Fraud Service",
    version=settings.service_version,
    description="Fraud probability inference for FinShield transaction risk scoring.",
    lifespan=lifespan,
)
app.include_router(health_router)
app.include_router(prediction_router)
