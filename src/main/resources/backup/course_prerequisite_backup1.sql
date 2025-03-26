-- MySQL dump 10.13  Distrib 8.0.33, for Win64 (x86_64)
--
-- Host: localhost    Database: enrollment_database
-- ------------------------------------------------------
-- Server version	8.0.33

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `course_prerequisite`
--

DROP TABLE IF EXISTS `course_prerequisite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `course_prerequisite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `child_id` int NOT NULL,
  `group_id` int NOT NULL,
  `is_child` bit(1) NOT NULL,
  `is_parent` bit(1) NOT NULL,
  `operator_to_next` enum('AND','OR') DEFAULT NULL,
  `parent_id` int NOT NULL,
  `percentage_value` double NOT NULL,
  `prerequisite_type` enum('AND','OR') DEFAULT NULL,
  `special` bit(1) NOT NULL,
  `special_type` enum('ADMISSION_PROGRAMME','COMPLETION_LEVEL_PERCENT') DEFAULT NULL,
  `target_level` smallint NOT NULL,
  `course_id` bigint NOT NULL,
  `prerequisite_id` bigint DEFAULT NULL,
  `programme_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK45vm6hef5h141uiu8qeod3wcg` (`course_id`),
  KEY `FK7ymu65b28wpw2opjmrx7hsbxl` (`prerequisite_id`),
  KEY `FK66m6gn3tpbonevaifsd27rtn9` (`programme_id`),
  CONSTRAINT `FK45vm6hef5h141uiu8qeod3wcg` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`),
  CONSTRAINT `FK66m6gn3tpbonevaifsd27rtn9` FOREIGN KEY (`programme_id`) REFERENCES `programme` (`id`),
  CONSTRAINT `FK7ymu65b28wpw2opjmrx7hsbxl` FOREIGN KEY (`prerequisite_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_prerequisite`
--

LOCK TABLES `course_prerequisite` WRITE;
/*!40000 ALTER TABLE `course_prerequisite` DISABLE KEYS */;
INSERT INTO `course_prerequisite` VALUES (10,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,15,14,NULL),(11,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,18,14,NULL),(12,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,19,15,NULL),(13,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,20,14,NULL),(14,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,20,17,2),(15,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,21,15,NULL),(16,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,22,15,NULL),(17,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,23,14,NULL),(18,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,23,16,1),(19,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,24,15,NULL),(20,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,24,23,NULL),(21,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,25,18,NULL),(22,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,26,18,NULL),(23,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,27,20,NULL),(24,0,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,28,21,NULL),(25,0,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,28,22,NULL),(26,0,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,28,19,NULL),(27,0,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,28,20,NULL),(28,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,29,24,NULL),(29,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,30,20,NULL),(30,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,31,25,NULL),(31,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,32,NULL,1),(32,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,32,NULL,2),(33,0,1,_binary '\0',_binary '','AND',0,1,'AND',_binary '','COMPLETION_LEVEL_PERCENT',100,33,NULL,NULL),(34,0,1,_binary '\0',_binary '','AND',0,1,'AND',_binary '','COMPLETION_LEVEL_PERCENT',200,33,NULL,NULL),(35,0,1,_binary '\0',_binary '','AND',0,1,'AND',_binary '','COMPLETION_LEVEL_PERCENT',300,33,NULL,NULL),(36,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,35,34,NULL),(37,2,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,36,34,NULL),(38,0,2,_binary '',_binary '\0',NULL,1,0,'AND',_binary '','ADMISSION_PROGRAMME',0,36,NULL,1),(39,0,2,_binary '',_binary '\0',NULL,1,0,'AND',_binary '','ADMISSION_PROGRAMME',0,36,NULL,2),(40,0,2,_binary '',_binary '\0',NULL,1,0,'AND',_binary '\0',NULL,0,36,32,NULL),(41,0,2,_binary '',_binary '\0',NULL,1,0.75,'AND',_binary '','COMPLETION_LEVEL_PERCENT',300,36,NULL,NULL),(42,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,37,34,NULL),(43,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,38,NULL,1),(44,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,38,NULL,2),(45,0,1,_binary '\0',_binary '','AND',0,0.75,'AND',_binary '','COMPLETION_LEVEL_PERCENT',300,38,NULL,NULL),(46,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,39,NULL,1),(47,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,39,NULL,2),(48,0,1,_binary '\0',_binary '','AND',0,0.75,'AND',_binary '','COMPLETION_LEVEL_PERCENT',300,39,NULL,NULL),(49,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,40,NULL,1),(50,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,40,NULL,2),(51,0,1,_binary '\0',_binary '','AND',0,0.75,'AND',_binary '','COMPLETION_LEVEL_PERCENT',300,40,NULL,NULL),(52,0,1,_binary '\0',_binary '','AND',0,1,'AND',_binary '','COMPLETION_LEVEL_PERCENT',200,10,NULL,NULL),(53,0,1,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,6,14,NULL),(54,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,7,14,NULL),(58,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,8,7,NULL),(59,0,2,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,8,24,NULL),(60,0,2,_binary '\0',_binary '','AND',0,0,'OR',_binary '\0',NULL,0,8,19,NULL),(61,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,9,7,NULL),(62,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,5,11,NULL),(63,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,5,12,NULL),(64,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '\0',NULL,0,13,12,NULL),(65,0,1,_binary '\0',_binary '','AND',0,0,'AND',_binary '','ADMISSION_PROGRAMME',0,41,NULL,3);
/*!40000 ALTER TABLE `course_prerequisite` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-26 17:58:08
