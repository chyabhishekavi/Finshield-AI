from __future__ import annotations

import logging
from pathlib import Path
from typing import Any

import joblib
import pandas as pd

from app.schemas.prediction import FraudPredictionResponse, RiskBand, TransactionFeatures
from app.services.reason_service import FraudReasonService

logger = logging.getLogger(__name__)


class ModelPredictionError(RuntimeError):
    """Raised when a loaded model cannot produce a valid fraud probability."""


class FraudModelService:
    def __init__(self, model_path: Path) -> None:
        self._model_path = model_path
        self._model: Any | None = None
        self._feature_columns: list[str] | None = None
        self._model_version = "heuristic-fallback-v1"
        self._reasons = FraudReasonService()
        self._load_model()

    @property
    def model_loaded(self) -> bool:
        return self._model is not None

    @property
    def model_version(self) -> str:
        return self._model_version

    def predict(self, features: TransactionFeatures) -> FraudPredictionResponse:
        try:
            heuristic_probability = self._fallback_probability(features)
            probability = (
                max(self._predict_with_model(features), heuristic_probability)
                if self._model is not None
                else heuristic_probability
            )
        except (KeyError, TypeError, ValueError, IndexError) as exception:
            logger.exception("Fraud model prediction failed for model %s", self._model_version)
            raise ModelPredictionError("Fraud model could not produce a prediction") from exception
        probability = round(max(0.0, min(1.0, probability)), 6)
        return FraudPredictionResponse(
            fraudProbability=probability,
            riskBand=self._risk_band(probability),
            topReasons=self._reasons.top_reasons(features),
        )

    def _load_model(self) -> None:
        if not self._model_path.is_file():
            logger.warning("Fraud model artifact not found; using explicit heuristic fallback")
            return
        artifact = joblib.load(self._model_path)
        if isinstance(artifact, dict):
            self._model = artifact.get("model")
            self._model_version = str(artifact.get("version", self._model_path.stem))
            columns = artifact.get("feature_columns")
            if columns is not None:
                if not isinstance(columns, (list, tuple)) or not all(
                    isinstance(column, str) and column for column in columns
                ):
                    raise ValueError("feature_columns must be a non-empty string list")
                self._feature_columns = list(columns)
        else:
            self._model = artifact
            self._model_version = self._model_path.stem
        if self._model is None or not hasattr(self._model, "predict_proba"):
            raise ValueError("Model artifact must provide predict_proba")
        logger.info("Loaded fraud model version %s", self._model_version)

    def _predict_with_model(self, features: TransactionFeatures) -> float:
        frame = self._feature_frame(features)
        columns = self._feature_columns
        if columns is None and hasattr(self._model, "feature_names_in_"):
            columns = list(self._model.feature_names_in_)
        if columns is not None:
            frame = frame[columns]
        probabilities = self._model.predict_proba(frame)
        classes = list(getattr(self._model, "classes_", [0, 1]))
        if 1 not in classes:
            raise ValueError("Model classes must include fraud class 1")
        fraud_class_index = classes.index(1)
        return float(probabilities[0][fraud_class_index])

    def _fallback_probability(self, features: TransactionFeatures) -> float:
        signals = [
            min(features.transaction_count_5m / 10, 1.0),
            min((features.amount / features.average_customer_amount) / 5, 1.0),
            min((features.amount / max(features.total_amount_1h, features.amount)) / 0.5, 1.0),
            0.0 if features.device_trusted else 1.0,
            1.0 if features.ip_address_changed else 0.0,
            1.0 if features.beneficiary_age_hours is None else max(0.0, 1 - features.beneficiary_age_hours / 168),
            features.beneficiary_risk_score / 100,
            min(features.geo_distance_km / 2_000, 1.0),
            features.customer_risk_score / 100,
            features.aml_risk_score / 100,
            min(features.failed_login_count / 8, 1.0),
        ]
        weights = [0.12, 0.12, 0.06, 0.15, 0.07, 0.10, 0.08, 0.08, 0.08, 0.07, 0.07]
        return sum(signal * weight for signal, weight in zip(signals, weights, strict=True))

    def _feature_frame(self, features: TransactionFeatures) -> pd.DataFrame:
        customer_risk_encoded = min(int(features.customer_risk_score // 25), 3)
        return pd.DataFrame(
            [
                {
                    "amount": features.amount,
                    "averageCustomerAmount": features.average_customer_amount,
                    "transactionHour": features.hour_of_day,
                    "isNewDevice": int(features.device_age_days is None or features.device_age_days < 1),
                    "isNewBeneficiary": int(
                        features.beneficiary_age_hours is None or features.beneficiary_age_hours < 24
                    ),
                    "failedLoginCount": features.failed_login_count,
                    "transactionsLast5Min": features.transaction_count_5m,
                    "amountLast1Hour": features.total_amount_1h,
                    "customerRiskLevelEncoded": customer_risk_encoded,
                    "geoMismatch": int(features.geo_distance_km >= 500),
                }
            ]
        )

    def _risk_band(self, probability: float) -> RiskBand:
        score = probability * 100
        if score <= 30:
            return RiskBand.LOW
        if score <= 60:
            return RiskBand.MEDIUM
        if score <= 80:
            return RiskBand.HIGH
        return RiskBand.CRITICAL
