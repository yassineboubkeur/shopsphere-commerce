export interface ProductCategory {
  id: number;
  name: string;
  description: string;
  imageUrl: string | null;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stockQuantity: number;
  imageUrl: string | null;
  active: boolean;
  category: ProductCategory | null;
}

export interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  price: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  userName: string;
  status: string;
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
  shippingName?: string | null;
  shippingAddress?: string | null;
  shippingCity?: string | null;
  shippingZip?: string | null;
  shippingPhone?: string | null;
}

export interface PaymentRequest {
  orderId: number;
  userId: number;
  amount: number;
  paymentMethod: string;
  cardNumber?: string;
  cardExpiryMonth?: string;
  cardExpiryYear?: string;
  cardCvv?: string;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  role: string;
}

export interface PageResult<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  page: number;
  size: number;
}

export interface SpringPage<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  empty: boolean;
}

export interface AuthResponse {
  token: string;
  type: string;
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: string;
}

export interface Notification {
  id: number;
  userId: number;
  type: string;
  subject: string;
  message: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}