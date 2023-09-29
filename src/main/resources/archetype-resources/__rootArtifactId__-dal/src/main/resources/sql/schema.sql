CREATE DATABASE IF NOT EXISTS `accounts` CHARACTER SET utf8mb4;


/******************************************/
/*   DatabaseName = accounts   */
/*   TableName = user   */
/******************************************/
CREATE TABLE IF NOT EXISTS `accounts`.`user` (
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `user_id` varchar(32) NOT NULL COMMENT 'immutable user identifier',
  `username` varchar(128) NOT NULL COMMENT 'mutable user identifier',
  `password` varchar(128) NOT NULL,
  `nickname` varchar(128) DEFAULT NULL,
  `given_name` varchar(128) DEFAULT NULL,
  `middle_name` varchar(128) DEFAULT NULL,
  `family_name` varchar(128) DEFAULT NULL,
  `preferred_username` varchar(128) DEFAULT NULL,
  `profile` varchar(256) DEFAULT NULL,
  `picture` varchar(256) DEFAULT NULL,
  `website` varchar(256) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `email_verified` TINYINT NOT NULL DEFAULT 0,
  `gender` varchar(32) DEFAULT NULL,
  `birthdate` datetime DEFAULT NULL,
  `zoneinfo` varchar(32) DEFAULT NULL,
  `locale` varchar(32) DEFAULT NULL,
  `phone_number` varchar(32) DEFAULT NULL,
  `phone_number_verified` TINYINT NOT NULL DEFAULT 0,
  `address` json DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `platform` varchar(128) NOT NULL DEFAULT 'system',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `locked` tinyint NOT NULL DEFAULT 0,
  `expires_at` datetime DEFAULT NULL,
  `password_expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_user_pass` (`username`,`password`),
  KEY `idx_user_platform` (`username`,`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='user table'
;

/******************************************/
/*   DatabaseName = accounts   */
/*   TableName = authority   */
/******************************************/
CREATE TABLE IF NOT EXISTS `accounts`.`authority` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `user_id` varchar(32) NOT NULL,
  `scope` text NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_scope` (`user_id`,`scope`(128)),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='authority table'
;

/******************************************/
/*   DatabaseName = accounts   */
/*   TableName = binding   */
/******************************************/
CREATE TABLE IF NOT EXISTS `accounts`.`binding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `user_id` varchar(32) NOT NULL,
  `platform` varchar(128) NOT NULL,
  `sub` varchar(256) NOT NULL,
  `iss` varchar(256) DEFAULT NULL,
  `aud` text DEFAULT NULL,
  `refresh_token` text DEFAULT NULL,
  `issued_at` datetime DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='oauth2 binding table'
;

/******************************************/
/*   DatabaseName = accounts   */
/*   TableName = api_token   */
/******************************************/
CREATE TABLE IF NOT EXISTS `accounts`.`api_token` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  `user_id` varchar(32) NOT NULL,
  `token` text NOT NULL,
  `type` varchar(16) NOT NULL DEFAULT 'access',
  `policy` text DEFAULT NULL,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token` (`token`(64)),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='api_token table'
;