from fastapi import APIRouter, Depends, HTTPException, status

from app.dependencies import get_model_service
from app.schemas.prediction import FraudPredictionResponse, TransactionFeatures
from app.services.model_service import FraudModelService, ModelPredictionError

router = APIRouter(prefix="/predict", tags=["prediction"])


@router.post("/fraud", response_model=FraudPredictionResponse)
def predict_fraud(
    request: TransactionFeatures,
    model_service: FraudModelService = Depends(get_model_service),
) -> FraudPredictionResponse:
    try:
        return model_service.predict(request)
    except ModelPredictionError as exception:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail={
                "code": "MODEL_PREDICTION_UNAVAILABLE",
                "message": "Fraud prediction is temporarily unavailable",
            },
        ) from exception
