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

export type TrustBadge = 'NOVA' | 'CONFIAVEL';

export type ReportReason = 'INFORMACAO_FALSA' | 'CONTEUDO_OFENSIVO' | 'SPAM' | 'OUTRO';

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

/** POST /api/places/batch: local que ninguém avaliou ainda vem zerado, não some. */
export interface PlaceSummary {
  placeId: string;
  averageScore: number;
  reviewCount: number;
  tagStats: Partial<Record<AccessibilityTag, TagStats>>;
}

export interface PlaceBatchRequest {
  /** No máximo 60. */
  placeIds: string[];
  /** Se vier, só voltam os locais que têm todas essas características. */
  tags?: AccessibilityTag[];
  /** Soma as tags essenciais para as necessidades do perfil. Exige login. */
  useMyNeeds?: boolean;
}

export interface Review {
  id: string;
  placeId: string;
  rating: number;
  comment: string | null;
  tags: ReviewTags;
  /** Só vem para o próprio autor: é dado sensível (LGPD). */
  reviewerNeeds?: AccessibilityNeed[] | null;
  status: ReviewStatus;
  createdAt: string;
  updatedAt: string;
  /** A API não expõe quem avaliou: diz só se a avaliação é de quem está logado. */
  mine: boolean;
  /** Nível da conta quando avaliou. Só vem para esses dois: os outros não têm selo. */
  trustBadge?: TrustBadge;
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

/** PATCH /api/users/me: campos ausentes não mudam; o backend não aceita esvaziar um campo. */
export interface UserUpdateRequest {
  name?: string;
  email?: string;
  phone?: string;
  age?: number;
  accessibilityNeeds?: AccessibilityNeed[];
  /** Troca de senha: o backend exige as duas juntas e encerra todas as sessões. */
  password?: string;
  currentPassword?: string;
}
