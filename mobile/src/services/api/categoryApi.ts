import { http } from './client';

/** Mirrors the backend CategoryResponse DTO. `system` = shared default category. */
export type CategoryDto = {
  id: string;
  name: string;
  categoryType: 'EXPENSE' | 'INCOME';
  system: boolean;
  createdAt: string;
};

/** GET /categories */
export async function listCategories(): Promise<CategoryDto[]> {
  const res = await http.get<CategoryDto[]>('/categories');
  return res.data;
}
