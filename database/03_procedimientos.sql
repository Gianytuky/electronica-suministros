USE electronica_suministros;

DELIMITER @@

-- ---------------------------------------------------------------------
-- 1) SP_Generar_Codigo
--    Autogenera códigos correlativos únicos. El UPDATE bloquea la fila del
--    correlativo hasta que termine la transacción, por lo que dos usuarios
--    que registren al mismo tiempo nunca reciben el mismo número.
--      'OT'          -> OT-1042
--      'SERVICIO'    -> ST014
--      'COMPROBANTE' -> B001-00000033
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_Generar_Codigo @@
CREATE PROCEDURE SP_Generar_Codigo(IN p_tipo VARCHAR(15), OUT p_codigo VARCHAR(20))
BEGIN
    DECLARE v_prefijo   VARCHAR(10);
    DECLARE v_separador VARCHAR(2);
    DECLARE v_longitud  INT;
    DECLARE v_numero    INT;

    SET v_prefijo = NULL;

    UPDATE correlativo
       SET ultimo_numero = ultimo_numero + 1
     WHERE tipo = p_tipo;

    SELECT prefijo, separador, longitud, ultimo_numero
      INTO v_prefijo, v_separador, v_longitud, v_numero
      FROM correlativo
     WHERE tipo = p_tipo;

    IF v_prefijo IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Tipo de correlativo no configurado';
    END IF;

    SET p_codigo = CONCAT(v_prefijo, v_separador, LPAD(v_numero, v_longitud, '0'));
END @@

-- ---------------------------------------------------------------------
-- 2) SP_Obtener_Codigo
--    Envoltura de SP_Generar_Codigo que devuelve el código como una tabla de
--    una fila y una columna. Spring Boot la invoca con CALL (igual que el
--    ejemplo de spmes visto en clase) para obtener el código de la OT
--    justo antes de guardarla.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_Obtener_Codigo @@
CREATE PROCEDURE SP_Obtener_Codigo(IN p_tipo VARCHAR(15))
BEGIN
    DECLARE v_codigo VARCHAR(20);
    CALL SP_Generar_Codigo(p_tipo, v_codigo);
    SELECT v_codigo AS codigo;
END @@

-- ---------------------------------------------------------------------
-- 3) SP_Registrar_Servicio
--    Igual que "spadicion" de clase: el código del servicio (ST014) no lo
--    escribe el usuario, lo genera la base de datos justo antes del INSERT.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_Registrar_Servicio @@
CREATE PROCEDURE SP_Registrar_Servicio(
    IN p_id_categoria INT,
    IN p_formato      VARCHAR(50),
    IN p_tipo_cobro   VARCHAR(30),
    IN p_precio       DECIMAL(10,2),
    IN p_imagen       VARCHAR(80))
BEGIN
    DECLARE v_codigo VARCHAR(20);

    CALL SP_Generar_Codigo('SERVICIO', v_codigo);

    INSERT INTO servicio_tarifario (id_categoria, codigo, formato, tipo_cobro, precio_unitario, imagen, estado)
    VALUES (p_id_categoria, v_codigo, p_formato, p_tipo_cobro, p_precio, p_imagen, 'ACTIVO');
END @@

-- ---------------------------------------------------------------------
-- 4) SP_Reporte_Ventas
--    Resume en la base de datos los ingresos LIQUIDADOS (boletas emitidas) de
--    un año: devuelve solo los consolidados por mes (no todas las filas).
--    Alimenta el gráfico de barras / líneas del Dashboard (ReporteVentasDTO).
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_Reporte_Ventas @@
CREATE PROCEDURE SP_Reporte_Ventas(IN p_anio INT)
BEGIN
    SELECT MONTH(c.fecha_pago)  AS mes,
           COUNT(*)             AS cantidad_ordenes,
           SUM(c.monto_final)   AS total_ingresos
      FROM comprobante_pago c
     WHERE YEAR(c.fecha_pago) = p_anio
     GROUP BY MONTH(c.fecha_pago)
     ORDER BY MONTH(c.fecha_pago);
END @@

-- ---------------------------------------------------------------------
-- 5) SP_Liquidar_Orden
--    Operación transaccional del cierre del servicio: la OT debe estar "Por Notificar";
--    el procedimiento genera el número de boleta (SP_Generar_Codigo), registra el
--    comprobante de pago con el monto final recalculado por el laboratorio y cambia el
--    estado de la OT a "Finalizado". Si algo falla, MySQL deshace todo.
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS SP_Liquidar_Orden @@
CREATE PROCEDURE SP_Liquidar_Orden(IN p_id_ot INT)
BEGIN
    DECLARE v_estado VARCHAR(30);
    DECLARE v_monto  DECIMAL(10,2);
    DECLARE v_numero VARCHAR(20);

    SET v_estado = NULL;

    SELECT estado, monto_estimado INTO v_estado, v_monto
      FROM orden_trabajo
     WHERE id_ot = p_id_ot
       FOR UPDATE;

    IF v_estado IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La orden de trabajo no existe';
    END IF;

    IF v_estado <> 'Por Notificar' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede cobrar una orden en estado Por Notificar';
    END IF;

    CALL SP_Generar_Codigo('COMPROBANTE', v_numero);

    INSERT INTO comprobante_pago (id_ot, tipo_comprobante, numero_comprobante, monto_final, fecha_pago)
    VALUES (p_id_ot, 'BOLETA', v_numero, v_monto, NOW());

    UPDATE orden_trabajo
       SET estado = 'Finalizado'
     WHERE id_ot = p_id_ot;
END @@

DELIMITER ;
