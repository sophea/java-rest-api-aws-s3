DROP DATABASE if EXISTS sample_mybatis;
CREATE DATABASE sample_mybatis;
USE sample_mybatis;

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `description` varchar(256) DEFAULT NULL,
  `type` varchar(256) DEFAULT NULL,
  `parentId` bigint(20) DEFAULT NULL,
  `logoUrl` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

 INSERT INTO category ( name, description) values('restaurant', 'restaurant Cambodia');
  INSERT INTO category ( name, description) values('movie', 'movie Cambodia');