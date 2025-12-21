-- phpMyAdmin SQL Dump
-- version 5.2.2deb1+deb13u1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Erstellungszeit: 21. Dez 2025 um 22:28
-- Server-Version: 11.8.3-MariaDB-0+deb13u1 from Debian
-- PHP-Version: 8.4.11

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Datenbank: wow2dc
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle dc_acc2wow_char
--

CREATE TABLE dc_acc2wow_char (
  guild_id bigint(20) UNSIGNED NOT NULL,
  wow_char_id bigint(20) UNSIGNED NOT NULL,
  wow_char_server varchar(64) NOT NULL,
  wow_char_name varchar(64) NOT NULL,
  wow_char_rank tinyint(4) NOT NULL,
  dc_id bigint(20) UNSIGNED DEFAULT NULL,
  dc_member_name varchar(64) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle dc_online_users
--

CREATE TABLE dc_online_users (
  guild_id bigint(20) UNSIGNED NOT NULL,
  member_id bigint(20) UNSIGNED NOT NULL,
  last_online date NOT NULL,
  member_name varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle dc_settings
--

CREATE TABLE dc_settings (
  guild_id bigint(20) UNSIGNED NOT NULL,
  channel_id bigint(20) UNSIGNED NOT NULL,
  wow_guild_id bigint(20) NOT NULL,
  wow_region varchar(4) NOT NULL,
  wow_realm_slug varchar(64) NOT NULL,
  wow_name_slug varchar(64) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle wow_rank2dc_role
--

CREATE TABLE wow_rank2dc_role (
  guild_id bigint(20) UNSIGNED NOT NULL,
  wow_rank tinyint(3) UNSIGNED NOT NULL,
  role_id bigint(20) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

--
-- Indizes der exportierten Tabellen
--

--
-- Indizes für die Tabelle dc_acc2wow_char
--
ALTER TABLE dc_acc2wow_char
  ADD PRIMARY KEY (guild_id,wow_char_id),
  ADD KEY idx_dc_acc2wow_char_dc_id (dc_id) USING BTREE,
  ADD KEY idx_dc_acc2wow_char_wow_char_name (wow_char_name) USING BTREE;

--
-- Indizes für die Tabelle dc_online_users
--
ALTER TABLE dc_online_users
  ADD PRIMARY KEY (guild_id,member_id);

--
-- Indizes für die Tabelle dc_settings
--
ALTER TABLE dc_settings
  ADD PRIMARY KEY (guild_id);

--
-- Indizes für die Tabelle wow_rank2dc_role
--
ALTER TABLE wow_rank2dc_role
  ADD PRIMARY KEY (guild_id,wow_rank,role_id),
  ADD KEY idx_dc_role2wow_rank_guild_id (guild_id);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
