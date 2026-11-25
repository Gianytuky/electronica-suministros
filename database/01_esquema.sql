CREATE DATABASE IF NOT EXISTS electronica_suministros
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE electronica_suministros;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS comprobante_pago;
DROP TABLE IF EXISTS captura_tecnica;
DROP TABLE IF EXISTS mantenimiento_cinta;
DROP TABLE IF EXISTS detalle_orden;
DROP TABLE IF EXISTS orden_trabajo;
DROP TABLE IF EXISTS servicio_tarifario;
DROP TABLE IF EXISTS categoria_material;
DROP TABLE IF EXISTS mensaje_contacto;
DROP TABLE IF EXISTS correlativo;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS rol;
SET FOREIGN_KEY_CHECKS = 1;

-- MÓDULO 1: SEGURIDAD Y ACCESOS

CREATE TABLE rol (
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol  VARCHAR(30)  NOT NULL UNIQUE,   -- ADMIN | EJECUTIVO | TECNICO
    descripcion VARCHAR(120)
) ENGINE = InnoDB;

CREATE TABLE usuario (
    id_usuario     INT AUTO_INCREMENT PRIMARY KEY,
    id_rol         INT          NOT NULL,
    nombres        VARCHAR(100) NOT NULL,
    correo         VARCHAR(100) NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,       -- hash BCrypt ({bcrypt}$2a$10$...); antes SHA-256 en hexadecimal
    estado         VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',   -- ACTIVO | INACTIVO
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Avance final: recuperación de contraseña con token UUID temporal (de un solo uso).
    -- Quedan en NULL mientras no haya una recuperación en curso.
    reset_token        VARCHAR(36) NULL,        -- UUID, p. ej. 3f2b8c1e-9d4a-4b7e-a1c2-5e6f7a8b9c0d
    reset_token_expira DATETIME    NULL,        -- fecha y hora límite para usar el token
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol),
    CONSTRAINT uq_usuario_reset_token UNIQUE (reset_token)
) ENGINE = InnoDB;

CREATE TABLE cliente (
    id_cliente     INT AUTO_INCREMENT PRIMARY KEY,
    dni_cliente    VARCHAR(15)  NOT NULL UNIQUE,   -- DNI (8 dígitos) o RUC (11 dígitos)
    nombres        VARCHAR(100) NOT NULL,
    telefono       VARCHAR(20),
    correo         VARCHAR(100),
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;

-- MÓDULO 2: CATÁLOGO Y TARIFAS

CREATE TABLE categoria_material (
    id_categoria     INT AUTO_INCREMENT PRIMARY KEY,
    nombre_categoria VARCHAR(50)  NOT NULL UNIQUE,    -- Video | Audio | Imagen
    descripcion      VARCHAR(200),
    imagen           VARCHAR(80)  NOT NULL DEFAULT 'banner.jpg',
    estado           VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO'
) ENGINE = InnoDB;

CREATE TABLE servicio_tarifario (
    id_servicio     INT AUTO_INCREMENT PRIMARY KEY,
    id_categoria    INT           NOT NULL,
    codigo          VARCHAR(10)   NOT NULL UNIQUE,    -- ST001, ST002... (lo genera SP_Generar_Codigo)
    formato         VARCHAR(50)   NOT NULL,           -- VHS, Betamax, LongPlay (LP)...
    tipo_cobro      VARCHAR(30)   NOT NULL,           -- 'Por Hora o Fracción' | 'Por Unidad'
    precio_unitario DECIMAL(10,2) NOT NULL,
    imagen          VARCHAR(80)   NOT NULL DEFAULT 'banner.jpg',
    estado          VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT fk_servicio_categoria FOREIGN KEY (id_categoria) REFERENCES categoria_material (id_categoria),
    CONSTRAINT uq_servicio_formato UNIQUE (formato),
    CONSTRAINT ck_servicio_precio CHECK (precio_unitario > 0)
) ENGINE = InnoDB;

-- Contadores de códigos (los usa el procedimiento SP_Generar_Codigo)
CREATE TABLE correlativo (
    tipo          VARCHAR(15) PRIMARY KEY,    -- OT | SERVICIO | COMPROBANTE
    prefijo       VARCHAR(10) NOT NULL,       -- OT, ST, B001
    separador     VARCHAR(2)  NOT NULL DEFAULT '-',
    longitud      INT         NOT NULL,       -- dígitos del número
    ultimo_numero INT         NOT NULL        -- último número entregado
) ENGINE = InnoDB;

-- MÓDULO 3: OPERACIÓN Y ÓRDENES DE TRABAJO

-- Eje del sistema: vincula al cliente, al ejecutivo de mostrador y (por sus detalles) al técnico.
-- Estados: 'En Recepción' -> 'Por Notificar' -> 'Finalizado'   (o 'Cancelado')
CREATE TABLE orden_trabajo (
    id_ot           INT AUTO_INCREMENT PRIMARY KEY,
    codigo_ot       VARCHAR(20)   NOT NULL UNIQUE,          -- OT-1001
    id_cliente      INT           NOT NULL,
    id_ejecutivo    INT           NOT NULL,                 -- usuario que registró la OT
    fecha_recepcion DATETIME      NOT NULL,
    estado          VARCHAR(30)   NOT NULL DEFAULT 'En Recepción',
    monto_estimado  DECIMAL(10,2) NOT NULL DEFAULT 0,       -- estimado al recibir; se recalcula con la captura técnica
    CONSTRAINT fk_ot_cliente   FOREIGN KEY (id_cliente)   REFERENCES cliente (id_cliente),
    CONSTRAINT fk_ot_ejecutivo FOREIGN KEY (id_ejecutivo) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_ot_estado CHECK (estado IN ('En Recepción', 'Por Notificar', 'Finalizado', 'Cancelado'))
) ENGINE = InnoDB;

-- Cada línea de la OT: qué formato se digitaliza y cuántas unidades / horas estimadas.
CREATE TABLE detalle_orden (
    id_detalle         INT AUTO_INCREMENT PRIMARY KEY,
    id_ot              INT           NOT NULL,
    id_servicio        INT           NOT NULL,
    cantidad_materiales INT          NOT NULL,              -- unidades (audio/imagen) u horas estimadas (video)
    precio_unitario    DECIMAL(10,2) NOT NULL,              -- tarifa vigente al recibir la OT
    subtotal           DECIMAL(10,2) NOT NULL,              -- incluye la limpieza si aplica
    CONSTRAINT fk_detalle_ot       FOREIGN KEY (id_ot)       REFERENCES orden_trabajo (id_ot) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_servicio FOREIGN KEY (id_servicio) REFERENCES servicio_tarifario (id_servicio),
    CONSTRAINT ck_detalle_cantidad CHECK (cantidad_materiales > 0)
) ENGINE = InnoDB;

-- Limpieza / eliminación de hongos de una cinta (1 a 1 con el detalle; solo existe si se solicita).
CREATE TABLE mantenimiento_cinta (
    id_mantenimiento INT AUTO_INCREMENT PRIMARY KEY,
    id_detalle       INT           NOT NULL UNIQUE,
    aplica_limpieza  TINYINT(1)    NOT NULL DEFAULT 1,
    costo_adicional  DECIMAL(10,2) NOT NULL DEFAULT 15.00,
    CONSTRAINT fk_mant_detalle FOREIGN KEY (id_detalle) REFERENCES detalle_orden (id_detalle) ON DELETE CASCADE
) ENGINE = InnoDB;

-- Medición real hecha por el técnico en el laboratorio (1 a 1 con el detalle).
CREATE TABLE captura_tecnica (
    id_captura           INT AUTO_INCREMENT PRIMARY KEY,
    id_detalle           INT           NOT NULL UNIQUE,
    id_tecnico           INT           NOT NULL,
    duracion_horas_reales DECIMAL(5,2) NOT NULL,
    peso_gb              DECIMAL(6,2)  NOT NULL,
    medio_entrega        VARCHAR(50)   NOT NULL,            -- USB, disco duro o enlace en la nube
    fecha_captura        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_captura_detalle FOREIGN KEY (id_detalle) REFERENCES detalle_orden (id_detalle) ON DELETE CASCADE,
    CONSTRAINT fk_captura_tecnico FOREIGN KEY (id_tecnico) REFERENCES usuario (id_usuario)
) ENGINE = InnoDB;

-- Boleta de la liquidación (1 a 1 con la OT). La inserta SP_Liquidar_Orden.
CREATE TABLE comprobante_pago (
    id_comprobante     INT AUTO_INCREMENT PRIMARY KEY,
    id_ot              INT           NOT NULL UNIQUE,
    tipo_comprobante   VARCHAR(30)   NOT NULL DEFAULT 'BOLETA',
    numero_comprobante VARCHAR(20)   NOT NULL UNIQUE,       -- B001-00000001
    monto_final        DECIMAL(10,2) NOT NULL,
    fecha_pago         DATETIME      NOT NULL,
    CONSTRAINT fk_comprobante_ot FOREIGN KEY (id_ot) REFERENCES orden_trabajo (id_ot)
) ENGINE = InnoDB;

CREATE TABLE mensaje_contacto (
    id_mensaje  INT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    correo      VARCHAR(100) NOT NULL,
    telefono    VARCHAR(20),
    mensaje     TEXT         NOT NULL,
    fecha_envio DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(10)  NOT NULL DEFAULT 'NUEVO'       -- NUEVO | LEIDO
) ENGINE = InnoDB;

-- Índices para los filtros y las listas en cascada
CREATE INDEX ix_ot_cliente ON orden_trabajo (id_cliente);
CREATE INDEX ix_ot_estado_fecha ON orden_trabajo (estado, fecha_recepcion);
CREATE INDEX ix_detalle_ot ON detalle_orden (id_ot);
CREATE INDEX ix_servicio_categoria ON servicio_tarifario (id_categoria);
CREATE INDEX ix_comprobante_fecha ON comprobante_pago (fecha_pago);
