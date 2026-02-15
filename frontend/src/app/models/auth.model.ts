export interface User {
  id: number;
  email: string;
  fullName?: string;
  role: string;
  createdAt?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName?: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  userId: number;
  fullName?: string;
}

export interface ErrorResponse {
  message: string;
  timestamp?: string;
  status: number;
}
