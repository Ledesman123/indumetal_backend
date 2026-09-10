-- ============================================================
-- V3__add_password_reset_codes.sql
-- Soporta el flujo de "olvide mi password":
--   1) el usuario pide un codigo (se guarda su hash + expiracion)
--   2) el usuario valida el codigo (se emite un token temporal de reset)
--   3) el usuario cambia su password usando ese token
-- ============================================================

CREATE TABLE password_reset_codes (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    codigo_hash         VARCHAR(100) NOT NULL,
    codigo_expiracion   TIMESTAMPTZ NOT NULL,
    codigo_verificado   BOOLEAN NOT NULL DEFAULT FALSE,
    token_hash          VARCHAR(100),
    token_expiracion    TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en      TIMESTAMPTZ
);

CREATE INDEX idx_password_reset_usuario ON password_reset_codes(usuario_id);

COMMENT ON TABLE password_reset_codes IS
    'Codigos de 6 digitos y tokens temporales para el flujo de recuperacion de contrasena (olvide mi password).';