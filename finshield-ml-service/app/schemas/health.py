from pydantic import BaseModel, ConfigDict, Field


class HealthResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: str
    service: str
    version: str
    model_loaded: bool = Field(alias="modelLoaded")
    model_version: str = Field(alias="modelVersion")
