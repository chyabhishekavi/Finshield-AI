from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["service"] == "finshield-ml-service"


def high_risk_request() -> dict[str, object]:
    return {
        "transactionReference": "TXN-100001",
        "amount": 8500,
        "averageCustomerAmount": 1400,
        "currency": "INR",
        "transactionType": "ACCOUNT_TRANSFER",
        "channel": "MOBILE_BANKING",
        "hourOfDay": 2,
        "accountAgeDays": 700,
        "transactionCount5m": 6,
        "totalAmount1h": 12000,
        "failedLoginCount": 4,
        "beneficiaryAgeHours": 2,
        "beneficiaryRiskScore": 65,
        "deviceTrusted": False,
        "deviceAgeDays": 0,
        "ipAddressChanged": True,
        "geoDistanceKm": 800,
        "customerRiskScore": 80,
        "amlRiskScore": 30,
    }


def test_fraud_prediction_contract() -> None:
    response = client.post("/predict/fraud", json=high_risk_request())
    assert response.status_code == 200
    body = response.json()
    assert list(body) == ["fraudProbability", "riskBand", "topReasons"]
    assert 0 <= body["fraudProbability"] <= 1
    assert body["riskBand"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
    assert 1 <= len(body["topReasons"]) <= 5


def test_prediction_response_is_stable() -> None:
    first = client.post("/predict/fraud", json=high_risk_request())
    second = client.post("/predict/fraud", json=high_risk_request())
    assert first.status_code == 200
    assert first.json() == second.json()
    assert "Transaction amount is much higher than customer average" in first.json()["topReasons"]
