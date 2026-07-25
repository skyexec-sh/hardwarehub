export interface Category {
  id: number;
  name: string;
  description?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Brand {
  id: number;
  name: string;
  description?: string | null;
  logoUrl?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Product {
  id: number;
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  brandId?: number | null;
  brandName?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  unit: string;
  costPrice: number;
  sellingPrice: number;
  currentStock: number;
  minimumStock: number;
  maximumStock?: number | null;
  imageUrl?: string | null;
  active: boolean;
  lowStock: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryPayload {
  name: string;
  description?: string | null;
  active?: boolean;
}

export interface BrandPayload {
  name: string;
  description?: string | null;
  logoUrl?: string | null;
  active?: boolean;
}

export interface ProductPayload {
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  brandId?: number | null;
  categoryId?: number | null;
  unit: string;
  costPrice: number;
  sellingPrice: number;
  currentStock: number;
  minimumStock: number;
  maximumStock?: number | null;
  imageUrl?: string | null;
  active?: boolean;
}
