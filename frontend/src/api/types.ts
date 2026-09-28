// Tipos espelhando os DTOs/entidades do backend (br.com.accessmap.backend).

export type AccessibilityNeed =
  | 'MOBILIDADE_REDUZIDA'
  | 'DEFICIENCIA_VISUAL'
  | 'DEFICIENCIA_AUDITIVA'
  | 'OUTROS';

export type Role = 'USER' | 'ADMIN';

export interface User {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  age: number | null;
  accessibilityNeeds: AccessibilityNeed[] | null;
  role: Role;
  createdAt: string;
  updatedAt: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  phone: string;
  password: string;
}

export interface RegisterRequest {
  phone: string;
  password: string;
  name: string;
  email?: string;
  age?: number;
  accessibilityNeeds?: AccessibilityNeed[];
}

// Formato de Page serializado como PagedModel pelo backend
export interface Paged<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
