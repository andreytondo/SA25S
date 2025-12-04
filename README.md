# SA25S – Login + 2FA (Senha + TOTP)

Stack: **Quarkus (Java 17)** no backend + **Angular** no frontend. Inclui fluxo completo de registro, login, habilitação TOTP com QR Code, verificação de OTP, bloqueio/limitação de tentativas e JWT de curta duração.

## Como rodar

### Backend (Quarkus)
1. `cd backend`
2. Defina chaves (dev):
   ```bash
   export JWT_SECRET="change-me-please"
   export MASTER_KEY="$(openssl rand -base64 32)"   # AES-256 para criptografar o segredo TOTP
   ```
3. `./mvnw quarkus:dev` (ou `mvn quarkus:dev`)
   - Banco H2 em memória, schema auto `update`.
   - Portas padrão: `8080`.

### Frontend (Angular)
1. `cd frontend`
2. `npm install`
3. `npm start` (abre em `http://localhost:4200`)
4. API base configurada em `src/environments/environment.ts`.

## Endpoints principais
- `POST /auth/register` – `{ "email": "", "password": "" }`
- `POST /auth/login` – `{ "email": "", "password": "" }`
  - Se 2FA desativado → `{ token }`
  - Se 2FA ativo → `{ temporaryToken, twoFactorRequired: true }`
- `POST /auth/2fa/setup` – Requer `Authorization: Bearer <accessToken>`; retorna `{ qrCodeDataUri, secretBase32, otpauthUrl }`
- `POST /auth/2fa/verify` – `{ temporaryToken, otp }` → retorna `{ token }`
- `POST /auth/2fa/disable` – Header `Authorization` + body `{ otp }`

## Fluxo resumido
1. Registro (email/senha) → senha guardada com **bcrypt**.
2. Login:
   - Sem 2FA → JWT de acesso curto (15 min).
   - Com 2FA → JWT temporário (5 min, single-use via nonce salvo no usuário).
3. Usuário digita OTP (`/auth/2fa/verify`):
   - Validação RFC-6238 via `otp-java`, janela ±1 passo.
   - Rate limiting simples (5 tentativas/5min) + bloqueio temporário após 5 falhas.
   - Emite JWT final.
4. Configurar 2FA (`/auth/2fa/setup`):
   - Gera segredo Base32, encripta com **AES/GCM** usando `MASTER_KEY`.
   - Cria QR Code (ZXing) e `otpauth://`.
5. Desativar 2FA exige OTP válido.

## Estrutura do backend
- `domain/User` – email, passwordHash, flags de 2FA, secret encriptado, controle de falhas/bloqueio.
- `service/*`
  - `PasswordService` (bcrypt)
  - `EncryptionService` (AES/GCM com master key; planeje KMS/Vault em prod)
  - `TotpService` (geração/validação RFC-6238)
  - `QrCodeService` (data URI PNG)
  - `JwtService` (JWT HS256 curto + token temporário)
  - `RateLimiterService` (in-memory)
  - `AuthService` (orquestra fluxo, logs de segurança via `Logger`)
- `api/AuthResource` – endpoints REST.
- Config: `backend/src/main/resources/application.yml`.

## Estrutura do frontend (Angular)
- Páginas: `login`, `register`, `2fa/setup`, `2fa/verify`, `account` (disable), `dashboard`.
- Guards: `AuthGuard` (rotas protegidas), `TempTokenGuard` (rota de OTP).
- `AuthService` mantém tokens (access/temporary), injeta Bearer via interceptor.
- UI escura com instruções de 2FA e QR Code.

## Exemplos de chamadas HTTP
```bash
# Registrar
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"MyStrongPass1!"}'

# Login (2FA ativo → retorna temporaryToken)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"MyStrongPass1!"}'

# Setup 2FA (envie Authorization com token de acesso)
curl -X POST http://localhost:8080/auth/2fa/setup \
  -H "Authorization: Bearer <accessToken>"

# Verificar OTP (trocar pelo código do app)
curl -X POST http://localhost:8080/auth/2fa/verify \
  -H "Content-Type: application/json" \
  -d '{"temporaryToken":"<temp>","otp":"123456"}'
```

## Notas de segurança
- Use **MASTER_KEY** forte (Base64 32 bytes) e mantenha fora do código.
- Planeje mover chaves para **AWS KMS** / **HashiCorp Vault** em produção.
- JWTs são HS256 curtos; preferir rotação e cookies HttpOnly/Secure se desejar.
- Rate limiting é in-memory; para produção use Redis ou serviço dedicado.
- Habilite HTTPS (terminação no gateway ou Quarkus TLS) para proteger tokens.

## Próximos passos sugeridos
- Adicionar refresh token/cookie HttpOnly + Secure.
- Persistir falhas/bloqueios em store compartilhado (Redis) para clusters.
- E-mails de alerta em enable/disable 2FA (obrigatório em produção).
- Integração de HOTP opcional reutilizando `TotpService` como base.
