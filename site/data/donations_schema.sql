-- Tabela de registro de doacoes voluntarias via Pix (Mercado Pago)
CREATE TABLE IF NOT EXISTS `donations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `login` VARCHAR(45) NOT NULL,
    `external_reference` CHAR(64) NOT NULL UNIQUE,
    `payment_id` VARCHAR(64) NULL UNIQUE,
    `amount` DECIMAL(10,2) NOT NULL,
    `currency` CHAR(3) NOT NULL DEFAULT 'BRL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
    `pix_qr_code` TEXT NULL,
    `pix_qr_code_base64` MEDIUMTEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `approved_at` DATETIME NULL,
    INDEX `idx_donations_login` (`login`),
    INDEX `idx_donations_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
