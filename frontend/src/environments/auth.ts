import type { AuthConfig } from '../app/core/auth/auth.types';

/** Build-time defaults — override at runtime via `public/auth-config.local.js` or sessionStorage. */
export const authBuildConfig: AuthConfig = {
  enabled: false,
  bearerToken: '',
  basicUsername: '',
  basicPassword: '',
};
