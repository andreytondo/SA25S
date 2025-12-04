export interface RegisterRequest {
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  twoFactorRequired: boolean;
  token?: string;
  temporaryToken?: string;
  message?: string;
}

export interface TwoFaSetupResponse {
  qrCodeDataUri: string;
  otpauthUrl: string;
  secretBase32: string;
  message: string;
}

export interface OtpVerifyRequest {
  temporaryToken: string;
  otp: string;
}

export interface Disable2faRequest {
  otp: string;
}
