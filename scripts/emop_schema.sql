-- MySQL dump 10.13  Distrib 5.1.66, for redhat-linux-gnu (x86_64)
--
-- Host: localhost    Database: emop
-- ------------------------------------------------------
-- Server version	5.1.66

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `eebo_word_freq`
--

DROP DATABASE IF EXISTS emop;
CREATE DATABASE emop CHARACTER SET=utf8;
use emop;

DROP TABLE IF EXISTS `eebo_word_freq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `eebo_word_freq` (
  `word` varchar(50) COLLATE utf8_bin NOT NULL,
  `frequency` bigint(20) NOT NULL,
  PRIMARY KEY (`word`),
  KEY `freq` (`frequency`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `pages`
--

DROP TABLE IF EXISTS `pages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pages` (
  `pg_page_id` int(11) NOT NULL,
  `pg_ref_number` int(11) DEFAULT NULL,
  `pg_ground_truth_file` varchar(200) DEFAULT NULL,
  `pg_work_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`pg_page_id`),
  KEY `pg_work_id` (`pg_work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `scratch`
--

DROP TABLE IF EXISTS `scratch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `scratch` (
  `scr_key` varchar(100) DEFAULT NULL,
  `scr_value` varchar(500) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `table_keys`
--

DROP TABLE IF EXISTS `table_keys`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `table_keys` (
  `tk_table` varchar(50) DEFAULT NULL,
  `tk_key` int(11) DEFAULT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `works`
--

DROP TABLE IF EXISTS `works`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `works` (
  `wks_work_id` int(11) NOT NULL,
  `wks_tcp_number` varchar(45) DEFAULT NULL,
  `wks_estc_number` varchar(45) DEFAULT NULL,
  `wks_tcp_bibno` int(11) DEFAULT NULL,
  `wks_marc_record` varchar(45) DEFAULT NULL,
  `wks_eebo_citation_id` int(11) DEFAULT NULL,
  `wks_eebo_directory` varchar(100) DEFAULT NULL,
  `wks_ecco_number` varchar(45) DEFAULT NULL,
  `wks_book_id` int(11) DEFAULT NULL,
  `wks_author` varchar(200) DEFAULT NULL,
  `wks_publisher` varchar(500) DEFAULT NULL,
  `wks_word_count` int(11) DEFAULT NULL,
  `wks_title` text,
  `wks_eebo_image_id` varchar(45) DEFAULT NULL,
  `wks_eebo_url` varchar(200) DEFAULT NULL,
  `wks_eebo_date` varchar(45) DEFAULT NULL,
  `wks_ecco_uncorrected_gale_ocr_path` varchar(200) DEFAULT NULL,
  `wks_ecco_corrected_xml_path` varchar(200) DEFAULT NULL,
  `wks_ecco_corrected_text_path` varchar(200) DEFAULT NULL,
  `wks_ecco_directory` varchar(200) DEFAULT NULL,
  `wks_ecco_gale_ocr_xml_path` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`wks_work_id`),
  UNIQUE KEY `wks_work_id_UNIQUE` (`wks_work_id`),
  KEY `wks_ecco_number` (`wks_ecco_number`),
  KEY `wks_book_id` (`wks_book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `works_backup`
--

DROP TABLE IF EXISTS `works_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `works_backup` (
  `wks_work_id` int(11) NOT NULL,
  `wks_tcp_number` int(11) DEFAULT NULL,
  `wks_estc_number` int(11) DEFAULT NULL,
  `wks_tcp_bibno` int(11) DEFAULT NULL,
  `wks_marc_record` varchar(45) DEFAULT NULL,
  `wks_eebo_citation_id` int(11) DEFAULT NULL,
  `wks_eebo_directory` varchar(100) DEFAULT NULL,
  `wks_ecco_number` int(11) DEFAULT NULL,
  `wks_book_id` int(11) DEFAULT NULL,
  `wks_author` varchar(200) DEFAULT NULL,
  `wks_publisher` varchar(400) DEFAULT NULL,
  `wks_word_count` int(11) DEFAULT NULL,
  `wks_title` text,
  `wks_eebo_image_id` varchar(45) DEFAULT NULL,
  `wks_eebo_url` varchar(200) DEFAULT NULL,
  `wks_eebo_date` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`wks_work_id`),
  UNIQUE KEY `wks_work_id_UNIQUE` (`wks_work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-05-03 10:08:30
