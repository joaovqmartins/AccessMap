import type { AccessibilityNeed, AccessibilityTag, TagAssessment } from '../api/types';

export const NEED_LABELS: Record<AccessibilityNeed, string> = {
  MOBILIDADE_REDUZIDA: 'Mobilidade reduzida',
  DEFICIENCIA_VISUAL: 'Deficiência visual',
  DEFICIENCIA_AUDITIVA: 'Deficiência auditiva',
  OUTROS: 'Outras necessidades',
};

export const TAG_LABELS: Record<AccessibilityTag, string> = {
  RAMPAS_E_ENTRADAS: 'Rampas e entradas',
  ELEVADORES: 'Elevadores',
  BANHEIROS_ADAPTADOS: 'Banheiros adaptados',
  VAGAS_ESTACIONAMENTO: 'Vagas de estacionamento',
  SINALIZACAO: 'Sinalização',
  ESPACO_CIRCULACAO: 'Espaço de circulação',
  ATENDIMENTO: 'Atendimento',
  OUTROS: 'Outros',
};

export const ASSESSMENT_LABELS: Record<TagAssessment, string> = {
  ADEQUADO: 'Adequado',
  INADEQUADO: 'Inadequado',
  INEXISTENTE: 'Inexistente',
};

// Ordem de exibição (mesma dos enums do backend)
export const ALL_TAGS = Object.keys(TAG_LABELS) as AccessibilityTag[];
export const ALL_NEEDS = Object.keys(NEED_LABELS) as AccessibilityNeed[];
export const ALL_ASSESSMENTS = Object.keys(ASSESSMENT_LABELS) as TagAssessment[];
