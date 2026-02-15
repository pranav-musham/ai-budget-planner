export interface Category {
  id?: number;
  name: string;
  icon?: string;
  color?: string;
  displayOrder?: number;
  type: 'PREDEFINED' | 'CUSTOM';
}
