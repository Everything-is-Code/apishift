export interface AuthConfig {
  enabled: boolean;
  bearerToken: string;
  basicUsername: string;
  basicPassword: string;
}

declare global {
  interface Window {
    __APISHIFT_AUTH__?: Partial<AuthConfig>;
  }
}

export {};
