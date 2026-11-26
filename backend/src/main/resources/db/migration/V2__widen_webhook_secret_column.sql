-- Widen webhook_secret column to accommodate encrypted values (ENC: prefix + Base64(IV + ciphertext + GCM tag))
ALTER TABLE integration_configs ALTER COLUMN webhook_secret TYPE VARCHAR(1000);
