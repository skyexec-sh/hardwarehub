export type InventoryTransactionType = 'STOCK_IN' | 'STOCK_OUT' | 'ADJUSTMENT';

export interface InventoryTransaction {
  id: number;
  productId: number;
  productSku: string;
  productName: string;
  unit: string;
  transactionType: InventoryTransactionType;
  quantity: number;
  quantityBefore: number;
  quantityAfter: number;
  unitCost?: number | null;
  referenceNo?: string | null;
  notes?: string | null;
  createdAt: string;
  createdBy?: string | null;
}

export interface InventoryTransactionPayload {
  productId: number;
  transactionType: InventoryTransactionType;
  quantity: number;
  unitCost?: number | null;
  referenceNo?: string | null;
  notes?: string | null;
}

export interface InventoryBatchLinePayload {
  productId: number;
  quantity: number;
  unitCost?: number | null;
}

export interface InventoryBatchPayload {
  transactionType: InventoryTransactionType;
  referenceNo?: string | null;
  notes?: string | null;
  lines: InventoryBatchLinePayload[];
}

export interface InventoryMovementLine {
  productId: number;
  sku: string;
  name: string;
  unit: string;
  stock: number;
  quantity: number;
  unitCost: number | null;
}

export interface LowStockProduct {
  productId: number;
  sku: string;
  name: string;
  unit: string;
  currentStock: number;
  minimumStock: number;
  maximumStock?: number | null;
  deficit: number;
}

export interface InventorySummary {
  lowStockCount: number;
  outOfStockCount: number;
  transactionCount: number;
}
