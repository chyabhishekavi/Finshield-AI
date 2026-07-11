# Model artifacts

Mount or copy a trusted `fraud_model.joblib` artifact into this directory.

The artifact may be either:

- an estimator or pipeline exposing `predict_proba`, or
- a dictionary containing `model` and `version` keys.

Only load artifacts produced by a trusted training pipeline. Joblib files can execute code during deserialization.
