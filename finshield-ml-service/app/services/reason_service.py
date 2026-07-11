from __future__ import annotations

from app.schemas.prediction import TransactionFeatures, TransactionType


class FraudReasonService:
    def top_reasons(self, features: TransactionFeatures) -> list[str]:
        reasons: list[tuple[float, int, str]] = []
        amount_ratio = features.amount / features.average_customer_amount
        is_new_device = features.device_age_days is None or features.device_age_days < 1

        if amount_ratio >= 3:
            reasons.append((min(amount_ratio / 6, 1), 1, "Transaction amount is much higher than customer average"))
        if is_new_device and amount_ratio >= 2:
            reasons.append((min(amount_ratio / 5, 1), 2, "New device used for high-value transaction"))
        if features.transaction_count_5m >= 5:
            reasons.append((min(features.transaction_count_5m / 10, 1), 3, "Multiple transactions detected within short time window"))
        if features.geo_distance_km >= 500:
            reasons.append((min(features.geo_distance_km / 2_000, 1), 4, "Geo-location mismatch"))
        if features.customer_risk_score >= 60:
            reasons.append((features.customer_risk_score / 100, 5, "High-risk customer profile"))

        if features.failed_login_count >= 3:
            reasons.append((min(features.failed_login_count / 8, 1), 6, "Multiple failed logins preceded the transaction"))
        if not features.device_trusted and not is_new_device:
            reasons.append((0.75, 7, "Transaction originated from an untrusted device"))
        if features.ip_address_changed:
            reasons.append((0.65, 8, "Device IP address changed from the known profile"))
        if features.beneficiary_age_hours is None or features.beneficiary_age_hours < 24:
            reasons.append((0.8, 9, "Beneficiary is new or has limited transaction history"))
        if features.aml_risk_score >= 60:
            reasons.append((features.aml_risk_score / 100, 10, "AML indicators are elevated"))
        if features.beneficiary_risk_score >= 60:
            reasons.append((features.beneficiary_risk_score / 100, 11, "Beneficiary risk profile is elevated"))
        if features.transaction_type is TransactionType.INTERNATIONAL_TRANSFER:
            reasons.append((0.55, 12, "Transaction is an international transfer"))
        if features.total_amount_1h > 0 and features.amount / features.total_amount_1h >= 0.5:
            reasons.append((0.5, 13, "Transaction represents a large share of recent activity"))

        reasons.sort(key=lambda item: (-item[0], item[1], item[2]))
        return [reason for _, _, reason in reasons[:5]] or ["No dominant fraud indicator was identified"]
