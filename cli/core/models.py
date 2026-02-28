from typing import Optional, List, Dict, Any
from pydantic import BaseModel, ConfigDict


class TrackoResponse(BaseModel):
    """Base response wrapper from the Tracko backend."""
    model_config = ConfigDict(extra="allow")
    
    timestamp: str
    status: int
    message: str
    path: str
    result: Optional[Any] = None


class Account(BaseModel):
    id: int
    name: str
    userId: str
    currency: Optional[str] = None


class AccountListResponse(BaseModel):
    model_config = ConfigDict(extra="allow")
    result: List[Account]


class Category(BaseModel):
    id: int
    name: str
    userId: str
    categoryType: Optional[str | int] = None
    isRollOverEnabled: Optional[bool] = None
    parentCategoryId: Optional[int] = None


class CategoryListResponse(BaseModel):
    model_config = ConfigDict(extra="allow")
    result: List[Category]


class Transaction(BaseModel):
    id: int
    accountId: int
    categoryId: int
    amount: float
    transactionType: int
    name: str
    date: int
    comments: Optional[str] = None
    isCountable: int
    userId: str
    originalAmount: Optional[float] = None
    originalCurrency: Optional[str] = None
    exchangeRate: Optional[float] = None
