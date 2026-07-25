export interface PriceLevel {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  sortOrder: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LevelPrice {
  priceLevelId: number;
  priceLevelCode: string;
  priceLevelName: string;
  unitPrice: number;
}

export interface LevelPricePayload {
  priceLevelId: number;
  unitPrice: number;
}

export interface ProductPriceHistoryItem {
  id: number;
  productId: number;
  priceType: 'COST' | 'LEVEL';
  priceLevelId?: number | null;
  priceLevelCode?: string | null;
  priceLevelName?: string | null;
  oldPrice?: number | null;
  newPrice: number;
  reason?: string | null;
  changedBy?: string | null;
  changedAt: string;
}

export interface ResolvedPrice {
  productId: number;
  priceLevelId: number;
  priceLevelCode: string;
  priceLevelName: string;
  unitPrice: number;
  fromLevelTable: boolean;
}
