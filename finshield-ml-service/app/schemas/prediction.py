from __future__ import annotations

from enum import StrEnum

from typing import Any

from pydantic import AliasChoices, BaseModel, ConfigDict, Field, field_validator, model_validator


class RiskBand(StrEnum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class TransactionType(StrEnum):
    ACCOUNT_TRANSFER = "ACCOUNT_TRANSFER"
    CARD_PURCHASE = "CARD_PURCHASE"
    CASH_DEPOSIT = "CASH_DEPOSIT"
    CASH_WITHDRAWAL = "CASH_WITHDRAWAL"
    BILL_PAYMENT = "BILL_PAYMENT"
    INTERNATIONAL_TRANSFER = "INTERNATIONAL_TRANSFER"
    REFUND = "REFUND"


class TransactionChannel(StrEnum):
    MOBILE_BANKING = "MOBILE_BANKING"
    INTERNET_BANKING = "INTERNET_BANKING"
    ATM = "ATM"
    BRANCH = "BRANCH"
    POS = "POS"
    API = "API"


class TransactionFeatures(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    transaction_reference: str = Field(default="ADHOC-PREDICTION", alias="transactionReference", min_length=6, max_length=64)
    amount: float = Field(gt=0, le=1_000_000_000)
    average_customer_amount: float = Field(alias="averageCustomerAmount", gt=0, le=1_000_000_000)
    currency: str = Field(default="INR", min_length=3, max_length=3)
    transaction_type: TransactionType = Field(default=TransactionType.ACCOUNT_TRANSFER, alias="transactionType")
    channel: TransactionChannel = TransactionChannel.MOBILE_BANKING
    hour_of_day: int = Field(validation_alias=AliasChoices("hourOfDay", "transactionHour"), ge=0, le=23)
    account_age_days: int = Field(default=365, alias="accountAgeDays", ge=0)
    transaction_count_5m: int = Field(validation_alias=AliasChoices("transactionCount5m", "transactionsLast5Min"), ge=0)
    total_amount_1h: float = Field(validation_alias=AliasChoices("totalAmount1h", "amountLast1Hour"), ge=0)
    failed_login_count: int = Field(alias="failedLoginCount", ge=0, le=100)
    beneficiary_age_hours: float | None = Field(default=None, alias="beneficiaryAgeHours", ge=0)
    beneficiary_risk_score: float = Field(alias="beneficiaryRiskScore", ge=0, le=100)
    device_trusted: bool = Field(alias="deviceTrusted")
    device_age_days: int | None = Field(default=None, alias="deviceAgeDays", ge=0)
    ip_address_changed: bool = Field(alias="ipAddressChanged")
    geo_distance_km: float = Field(alias="geoDistanceKm", ge=0, le=20_000)
    customer_risk_score: float = Field(alias="customerRiskScore", ge=0, le=100)
    aml_risk_score: float = Field(alias="amlRiskScore", ge=0, le=100)

    @model_validator(mode="before")
    @classmethod
    def expand_compact_request(cls, value: Any) -> Any:
        if not isinstance(value, dict):
            return value
        data = dict(value)
        is_new_device = data.pop("isNewDevice", None)
        is_new_beneficiary = data.pop("isNewBeneficiary", None)
        customer_risk = data.pop("customerRiskLevelEncoded", None)
        geo_mismatch = data.pop("geoMismatch", None)
        if is_new_device is not None:
            data.setdefault("deviceTrusted", not bool(is_new_device))
            data.setdefault("deviceAgeDays", 0 if is_new_device else 365)
            data.setdefault("ipAddressChanged", bool(is_new_device))
        if is_new_beneficiary is not None:
            data.setdefault("beneficiaryAgeHours", 0 if is_new_beneficiary else 720)
            data.setdefault("beneficiaryRiskScore", 70 if is_new_beneficiary else 10)
        if customer_risk is not None:
            data.setdefault("customerRiskScore", min(float(customer_risk) * 35, 100))
        if geo_mismatch is not None:
            data.setdefault("geoDistanceKm", 800 if geo_mismatch else 0)
        data.setdefault("failedLoginCount", 0)
        data.setdefault("beneficiaryRiskScore", 0)
        data.setdefault("deviceTrusted", True)
        data.setdefault("ipAddressChanged", False)
        data.setdefault("geoDistanceKm", 0)
        data.setdefault("customerRiskScore", 0)
        data.setdefault("amlRiskScore", 0)
        return data

    @field_validator("currency")
    @classmethod
    def normalize_currency(cls, value: str) -> str:
        if not value.isalpha():
            raise ValueError("currency must be a three-letter ISO code")
        return value.upper()


class FraudPredictionResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    fraud_probability: float = Field(alias="fraudProbability", ge=0, le=1)
    risk_band: RiskBand = Field(alias="riskBand")
    top_reasons: list[str] = Field(alias="topReasons", max_length=5)
