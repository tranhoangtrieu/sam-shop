export interface JwtPayload {
  sub?: string;
  preferred_username?: string;
  userId?: number;
  realm_access?: { roles?: string[] };
}

export function parseJwtPayload(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

export function resolveRole(payload: JwtPayload | null): 'USER' | 'EMPLOYEE' | 'ADMIN' {
  const roles = payload?.realm_access?.roles ?? [];
  if (roles.includes('ADMIN')) return 'ADMIN';
  if (roles.includes('EMPLOYEE')) return 'EMPLOYEE';
  return 'USER';
}
