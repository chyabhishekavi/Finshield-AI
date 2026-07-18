# FinShield ML Service

FastAPI inference service for transaction fraud probability scoring.

## Local run

```bash
python -m venv .venv
.venv/Scripts/activate
pip install -r requirements-dev.txt
uvicorn app.main:app --reload
```

- Health: http://localhost:8000/health
- OpenAPI: http://localhost:8000/docs
- Prediction: `POST http://localhost:8000/predict/fraud`

Set `MODEL_PATH` to a trusted joblib model artifact. When no artifact exists, the service reports a degraded health state and uses an explicitly identified heuristic fallback.

## Train the synthetic baseline model

```bash
python -m scripts.train_fraud_model --rows 100000 --output models/fraud_model.joblib
```

The command writes the joblib model, `feature_columns.json`, and `training_metadata.json`.
