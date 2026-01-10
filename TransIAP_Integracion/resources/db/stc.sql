-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- Servidor: localhost
-- Tiempo de generación: 08-11-2023 a las 14:52:02
-- Versión del servidor: 10.4.21-MariaDB
-- Versión de PHP: 8.0.13

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `stc`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `compañia_naviera`
--

CREATE TABLE `compañia_naviera` (
  `id` varchar(64) NOT NULL,
  `nombre` varchar(1024) NOT NULL,
  `codigo_pais` varchar(2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Volcado de datos para la tabla `compañia_naviera`
--

INSERT INTO `compañia_naviera` (`id`, `nombre`, `codigo_pais`) VALUES
('PackTrans', 'PackTrans', 'CN'),
('SeaTrans', 'SeaTrans', 'IE'),
('WorldPort', 'WorldPort', 'ES');

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `contenedor`
--

CREATE TABLE `contenedor` (
  `idc` varchar(64) NOT NULL,
  `tipo` varchar(16) NOT NULL,
  `peso` int(11) NOT NULL,
  `dimension` varchar(32) NOT NULL,
  `precinto` int(11) NOT NULL,
  `descarga` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `direccion`
--

CREATE TABLE `direccion` (
  `id` varchar(64) NOT NULL,
  `direccion` varchar(256) NOT NULL,
  `cp` int(11) NOT NULL,
  `ciudad` varchar(64) NOT NULL,
  `codigo_pais` varchar(2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `locallizacion_GPS`
--

CREATE TABLE `locallizacion_GPS` (
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `traslado` varchar(64) NOT NULL,
  `latitud` double NOT NULL,
  `longitud` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `solicitud_descarga`
--

CREATE TABLE `solicitud_descarga` (
  `id` int(11) NOT NULL,
  `fecha` varchar(64) NOT NULL,
  `naviera` varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `solicitud_transporte`
--

CREATE TABLE `solicitud_transporte` (
  `id` int(11) NOT NULL,
  `timestamp` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `aprobada` tinyint(1) NOT NULL DEFAULT 0,
  `terminada` tinyint(1) NOT NULL DEFAULT 0,
  `traslado` varchar(64) NOT NULL,
  `vehiculo` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `transportista`
--

CREATE TABLE `transportista` (
  `id` varchar(64) NOT NULL,
  `nombre` varchar(128) NOT NULL,
  `licencia` varchar(64) NOT NULL,
  `externo` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `traslado`
--

CREATE TABLE `traslado` (
  `idt` varchar(64) NOT NULL,
  `fechaEntrega` varchar(64) NOT NULL,
  `estado` varchar(32) NOT NULL,
  `destino` varchar(64) NOT NULL,
  `contenedor` varchar(64) NOT NULL,
  `ultima_ubicacion` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `vehiculo`
--

CREATE TABLE `vehiculo` (
  `matricula` varchar(32) NOT NULL,
  `PMA` varchar(64) NOT NULL,
  `ETN` varchar(16) NOT NULL,
  `transportista` varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `compañia_naviera`
--
ALTER TABLE `compañia_naviera`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `contenedor`
--
ALTER TABLE `contenedor`
  ADD PRIMARY KEY (`idc`),
  ADD KEY `FK_Desarga_INDEX` (`descarga`);

--
-- Indices de la tabla `direccion`
--
ALTER TABLE `direccion`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `locallizacion_GPS`
--
ALTER TABLE `locallizacion_GPS`
  ADD PRIMARY KEY (`timestamp`,`traslado`),
  ADD KEY `traslado` (`traslado`);

--
-- Indices de la tabla `solicitud_descarga`
--
ALTER TABLE `solicitud_descarga`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_Naviera_INDEX` (`naviera`);

--
-- Indices de la tabla `solicitud_transporte`
--
ALTER TABLE `solicitud_transporte`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_Solicitud-Traslado_INDEX` (`traslado`),
  ADD KEY `FK_Vehiculo_INDEX` (`vehiculo`);

--
-- Indices de la tabla `transportista`
--
ALTER TABLE `transportista`
  ADD PRIMARY KEY (`id`);

--
-- Indices de la tabla `traslado`
--
ALTER TABLE `traslado`
  ADD PRIMARY KEY (`idt`),
  ADD KEY `FK_destino_INDEX` (`destino`),
  ADD KEY `FK_Contenedor_INDEX` (`contenedor`),
  ADD KEY `FK_LocalizacionGPS_INDEX` (`ultima_ubicacion`);

--
-- Indices de la tabla `vehiculo`
--
ALTER TABLE `vehiculo`
  ADD PRIMARY KEY (`matricula`),
  ADD KEY `FK_Transportista_INDEX` (`transportista`);

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `contenedor`
--
ALTER TABLE `contenedor`
  ADD CONSTRAINT `contenedor_ibfk_1` FOREIGN KEY (`descarga`) REFERENCES `solicitud_descarga` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `locallizacion_GPS`
--
ALTER TABLE `locallizacion_GPS`
  ADD CONSTRAINT `locallizacion_gps_ibfk_1` FOREIGN KEY (`traslado`) REFERENCES `traslado` (`idt`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `solicitud_descarga`
--
ALTER TABLE `solicitud_descarga`
  ADD CONSTRAINT `solicitud_descarga_ibfk_1` FOREIGN KEY (`naviera`) REFERENCES `compañia_naviera` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `solicitud_transporte`
--
ALTER TABLE `solicitud_transporte`
  ADD CONSTRAINT `solicitud_transporte_ibfk_1` FOREIGN KEY (`traslado`) REFERENCES `traslado` (`idt`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `solicitud_transporte_ibfk_2` FOREIGN KEY (`vehiculo`) REFERENCES `vehiculo` (`matricula`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Filtros para la tabla `traslado`
--
ALTER TABLE `traslado`
  ADD CONSTRAINT `traslado_ibfk_1` FOREIGN KEY (`destino`) REFERENCES `direccion` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `traslado_ibfk_2` FOREIGN KEY (`contenedor`) REFERENCES `contenedor` (`idc`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `traslado_ibfk_3` FOREIGN KEY (`ultima_ubicacion`) REFERENCES `locallizacion_GPS` (`timestamp`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Filtros para la tabla `vehiculo`
--
ALTER TABLE `vehiculo`
  ADD CONSTRAINT `vehiculo_ibfk_1` FOREIGN KEY (`transportista`) REFERENCES `transportista` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
