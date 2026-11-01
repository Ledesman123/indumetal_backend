-- Perfil de usuario: telefono y foto (imagen comprimida en base64 desde el frontend).
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS telefono VARCHAR(20);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS foto_url TEXT;

-- RF-19: la imagen del material se guarda como data URL (base64), que no cabe en VARCHAR(500).
ALTER TABLE materiales ALTER COLUMN imagen_url TYPE TEXT;
