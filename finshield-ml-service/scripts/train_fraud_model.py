from __future__ import annotations

import argparse
import json
from datetime import UTC, datetime
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import (
    average_precision_score,
    classification_report,
    roc_auc_score,
)
from sklearn.model_selection import train_test_split

FEATURE_COLUMNS = [
    "amount",
    "averageCustomerAmount",
    "transactionHour",
    "isNewDevice",
    "isNewBeneficiary",
    "failedLoginCount",
    "transactionsLast5Min",
    "amountLast1Hour",
    "customerRiskLevelEncoded",
    "geoMismatch",
]
TARGET_COLUMN = "isFraud"
MODEL_VERSION = "random-forest-synthetic-v1"


def generate_synthetic_transactions(rows: int, seed: int) -> pd.DataFrame:
    if rows < 5_000:
        raise ValueError("rows must be at least 5,000 to produce a useful stratified dataset")

    rng = np.random.default_rng(seed)

    average_amount = np.clip(rng.lognormal(mean=7.1, sigma=0.85, size=rows), 100, 250_000)
    amount_multiplier = rng.lognormal(mean=0.0, sigma=0.65, size=rows)
    amount = np.clip(average_amount * amount_multiplier, 10, 10_000_000)

    high_value_burst = rng.random(rows) < 0.018
    amount[high_value_burst] *= rng.uniform(4, 15, size=high_value_burst.sum())
    amount = np.clip(amount, 10, 10_000_000)

    hour_probabilities = np.array(
        [0.012, 0.009, 0.007, 0.006, 0.006, 0.012, 0.025, 0.045,
         0.060, 0.065, 0.065, 0.060, 0.060, 0.060, 0.060, 0.060,
         0.060, 0.060, 0.055, 0.050, 0.045, 0.040, 0.025, 0.018]
    )
    hour_probabilities /= hour_probabilities.sum()
    transaction_hour = rng.choice(np.arange(24), size=rows, p=hour_probabilities)

    is_new_device = rng.binomial(1, 0.07, rows)
    is_new_beneficiary = rng.binomial(1, 0.13, rows)
    failed_login_count = np.clip(rng.poisson(0.25, rows), 0, 12)
    login_attack = rng.random(rows) < 0.025
    failed_login_count[login_attack] += rng.integers(3, 9, login_attack.sum())
    failed_login_count = np.clip(failed_login_count, 0, 20)

    transactions_5m = np.clip(rng.poisson(1.4, rows) + 1, 1, 20)
    velocity_burst = rng.random(rows) < 0.025
    transactions_5m[velocity_burst] += rng.integers(4, 12, velocity_burst.sum())
    transactions_5m = np.clip(transactions_5m, 1, 30)

    other_hourly_activity = average_amount * rng.gamma(shape=1.4, scale=1.2, size=rows)
    amount_last_hour = np.clip(amount + other_hourly_activity, amount, 25_000_000)

    customer_risk = rng.choice([0, 1, 2, 3], size=rows, p=[0.62, 0.25, 0.10, 0.03])
    geo_mismatch_probability = 0.025 + (is_new_device * 0.12) + (customer_risk * 0.015)
    geo_mismatch = rng.binomial(1, np.clip(geo_mismatch_probability, 0, 0.6))

    amount_ratio = np.clip(amount / average_amount, 0, 25)
    hourly_ratio = np.clip(amount_last_hour / average_amount, 0, 60)
    night_transaction = ((transaction_hour <= 4) | (transaction_hour >= 23)).astype(float)

    log_odds = (
        -6.6
        + 0.25 * np.log1p(amount_ratio)
        + 0.55 * is_new_device
        + 0.40 * is_new_beneficiary
        + 0.24 * failed_login_count
        + 0.18 * np.maximum(transactions_5m - 3, 0)
        + 0.08 * np.log1p(hourly_ratio)
        + 0.52 * customer_risk
        + 1.15 * geo_mismatch
        + 0.35 * night_transaction
        + 0.80 * is_new_device * (amount_ratio >= 4)
        + 0.60 * is_new_beneficiary * (transactions_5m >= 5)
        + rng.normal(0, 0.45, rows)
    )
    fraud_probability = 1 / (1 + np.exp(-np.clip(log_odds, -20, 20)))
    is_fraud = rng.binomial(1, fraud_probability)

    return pd.DataFrame(
        {
            "amount": np.round(amount, 2),
            "averageCustomerAmount": np.round(average_amount, 2),
            "transactionHour": transaction_hour,
            "isNewDevice": is_new_device,
            "isNewBeneficiary": is_new_beneficiary,
            "failedLoginCount": failed_login_count,
            "transactionsLast5Min": transactions_5m,
            "amountLast1Hour": np.round(amount_last_hour, 2),
            "customerRiskLevelEncoded": customer_risk,
            "geoMismatch": geo_mismatch,
            TARGET_COLUMN: is_fraud,
        }
    )


def train_model(data: pd.DataFrame, seed: int) -> tuple[RandomForestClassifier, dict[str, object]]:
    features = data[FEATURE_COLUMNS]
    target = data[TARGET_COLUMN]
    x_train, x_test, y_train, y_test = train_test_split(
        features,
        target,
        test_size=0.2,
        random_state=seed,
        stratify=target,
    )

    model = RandomForestClassifier(
        n_estimators=350,
        max_depth=14,
        min_samples_leaf=5,
        max_features="sqrt",
        class_weight="balanced_subsample",
        n_jobs=-1,
        random_state=seed,
    )
    model.fit(x_train, y_train)

    probabilities = model.predict_proba(x_test)[:, 1]
    predictions = (probabilities >= 0.5).astype(int)
    report = classification_report(y_test, predictions, output_dict=True, zero_division=0)
    metrics: dict[str, object] = {
        "rocAuc": round(float(roc_auc_score(y_test, probabilities)), 6),
        "averagePrecision": round(float(average_precision_score(y_test, probabilities)), 6),
        "precision": round(float(report["1"]["precision"]), 6),
        "recall": round(float(report["1"]["recall"]), 6),
        "f1": round(float(report["1"]["f1-score"]), 6),
        "testRows": int(len(y_test)),
        "testFraudRate": round(float(y_test.mean()), 6),
    }
    return model, metrics


def save_artifacts(
    model: RandomForestClassifier,
    feature_columns: list[str],
    metrics: dict[str, object],
    output_path: Path,
    training_rows: int,
    fraud_rate: float,
) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    trained_at = datetime.now(UTC).isoformat()
    artifact = {
        "model": model,
        "version": MODEL_VERSION,
        "feature_columns": feature_columns,
        "metrics": metrics,
        "trained_at": trained_at,
        "training_rows": training_rows,
        "training_fraud_rate": fraud_rate,
    }
    joblib.dump(artifact, output_path, compress=3)

    feature_path = output_path.with_name("feature_columns.json")
    feature_path.write_text(json.dumps(feature_columns, indent=2) + "\n", encoding="utf-8")

    metadata_path = output_path.with_name("training_metadata.json")
    metadata_path.write_text(
        json.dumps(
            {
                "modelVersion": MODEL_VERSION,
                "trainedAt": trained_at,
                "trainingRows": training_rows,
                "trainingFraudRate": round(fraud_rate, 6),
                "metrics": metrics,
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train the FinShield fraud classifier")
    parser.add_argument("--rows", type=int, default=100_000, help="Synthetic training rows")
    parser.add_argument("--seed", type=int, default=42, help="Random seed")
    parser.add_argument(
        "--output",
        type=Path,
        default=Path("models/fraud_model.joblib"),
        help="Joblib artifact output path",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    data = generate_synthetic_transactions(args.rows, args.seed)
    if data[TARGET_COLUMN].nunique() != 2:
        raise RuntimeError("Synthetic dataset did not produce both fraud classes")

    model, metrics = train_model(data, args.seed)
    save_artifacts(
        model,
        FEATURE_COLUMNS,
        metrics,
        args.output,
        len(data),
        float(data[TARGET_COLUMN].mean()),
    )
    print(f"Saved model to {args.output}")
    print(f"Training fraud rate: {data[TARGET_COLUMN].mean():.4%}")
    print(json.dumps(metrics, indent=2))


if __name__ == "__main__":
    main()
