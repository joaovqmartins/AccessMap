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

export type AccessibilityTag =
  | 'RAMPAS_E_ENTRADAS'
  | 'ELEVADORES'
  | 'BANHEIROS_ADAPTADOS'
  | 'VAGAS_ESTACIONAMENTO'
  | 'SINALIZACAO'
  | 'ESPACO_CIRCULACAO'
  | 'ATENDIMENTO'
  | 'OUTROS';

export type TagAssessment = 'ADEQUADO' | 'INADEQUADO' | 'INEXISTENTE';

export type ReviewStatus = 'PUBLICADA' | 'OCULTA' | 'REMOVIDA';

export interface TagStats {
  adequadoCount: number;
  inadequadoCount: number;
  inexistenteCount: number;
}

export interface Place {
  id: string;
  placeId: string;
  averageScore: number;
  reviewCount: number;
  tagStats: Partial<Record<AccessibilityTag, TagStats>>;
  createdAt: string;
  updatedAt: string;
}

export interface Review {
  id: string;
  userId: string;
  placeId: string;
  rating: number;
  comment: string | null;
  tags: ReviewTags;
  reviewerNeeds: AccessibilityNeed[] | null;
  status: ReviewStatus;
  createdAt: string;
  updatedAt: string;
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

export type ReviewTags = Partial<Record<AccessibilityTag, TagAssessment>>;

export interface ReviewRequest {
  placeId: string;
  rating: number;
  comment?: string;
  tags: ReviewTags;
}

/** PATCH: campos ausentes não mudam; `tags`, se enviado, substitui o mapa inteiro. */
export type ReviewUpdateRequest = Partial<Pick<ReviewRequest, 'rating' | 'comment' | 'tags'>>;
