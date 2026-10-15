
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

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `projeto` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `projeto`;
DROP TABLE IF EXISTS `cliente`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cliente` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cpf` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `nome` varchar(255) NOT NULL,
  `telefone` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `cliente` WRITE;
/*!40000 ALTER TABLE `cliente` DISABLE KEYS */;
INSERT INTO `cliente` VALUES (1,'123.456.789-01','maria.fernandes@email.com','Maria Fernandes da Silva','(18) 99999-1111'),(2,'234.567.890-12','pedro.lima@email.com','Pedro Henrique Lima','(18) 98888-2222'),(3,'345.678.901-23','fernanda.costa@email.com','Fernanda Costa','(18) 97777-3333'),(4,'456.789.012-34','rafael.mendes@email.com','Rafael Mendes','(18) 96666-4444'),(5,'567.890.123-45','juliana.rocha@email.com','Juliana Rocha','(18) 95555-5555');
/*!40000 ALTER TABLE `cliente` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `funcionario`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `funcionario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cargo` varchar(255) NOT NULL,
  `cpf` varchar(255) NOT NULL,
  `nome` varchar(100) NOT NULL,
  `salario` double DEFAULT NULL,
  `senha` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `funcionario` WRITE;
/*!40000 ALTER TABLE `funcionario` DISABLE KEYS */;
INSERT INTO `funcionario` VALUES (1,'Operador de Caixa','111.111.111-11','Luiz Almeida',2200,'1234'),(2,'Gerente','222.222.222-22','Maria Silva',4500,'1234'),(3,'Estoquista','333.333.333-33','Joao Souza',1900,'1234'),(4,'Repositor','444.444.444-44','Ana Oliveira',1800,'1234'),(5,'Supervisor','555.555.555-55','Carlos Pereira',3200,'1234');
/*!40000 ALTER TABLE `funcionario` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `item_venda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `item_venda` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `quantidade` int DEFAULT NULL,
  `subtotal` double DEFAULT NULL,
  `produto_id` bigint DEFAULT NULL,
  `venda_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK7wkinkvno0wlhv821hhu34y04` (`produto_id`),
  KEY `FKkiky88fkai72328rhw3r3yebx` (`venda_id`),
  CONSTRAINT `FK7wkinkvno0wlhv821hhu34y04` FOREIGN KEY (`produto_id`) REFERENCES `produto` (`id`),
  CONSTRAINT `FKkiky88fkai72328rhw3r3yebx` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `item_venda` WRITE;
/*!40000 ALTER TABLE `item_venda` DISABLE KEYS */;
/*!40000 ALTER TABLE `item_venda` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `produto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `produto` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `descricao` varchar(255) NOT NULL,
  `estoque` int DEFAULT NULL,
  `nome` varchar(100) NOT NULL,
  `preco` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `produto` WRITE;
/*!40000 ALTER TABLE `produto` DISABLE KEYS */;
INSERT INTO `produto` VALUES (1,'Salgado assado, unidade 90g',42,'Coxinha de Frango',7.5),(2,'Dose única, copo 50ml',100,'Café Expresso',4),(3,'Cola, 350ml',87,'Refrigerante Lata',6),(4,'Pacote com 4 unidades',30,'Pão de Queijo',5.5),(5,'Sabor morango, 60ml',15,'Sorvete Picolé',8),(6,'Ao leite, 90g',58,'Chocolate Barra',6.5),(7,'Assada, unidade 100g',21,'Esfirra de Carne',8.5),(8,'Copo 200ml, adoçado',60,'Cappuccino',6),(9,'Garrafa 500ml',64,'Água com Gás',4.5);
/*!40000 ALTER TABLE `produto` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `venda`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venda` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data_venda` date NOT NULL,
  `valor_total` double DEFAULT NULL,
  `cliente_id` bigint NOT NULL,
  `funcionario_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK50murhuotq9h2dnxej317jjiy` (`cliente_id`),
  KEY `FKon6h4s8x1kdjwkcipq65i5my7` (`funcionario_id`),
  CONSTRAINT `FK50murhuotq9h2dnxej317jjiy` FOREIGN KEY (`cliente_id`) REFERENCES `cliente` (`id`),
  CONSTRAINT `FKon6h4s8x1kdjwkcipq65i5my7` FOREIGN KEY (`funcionario_id`) REFERENCES `funcionario` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `venda` WRITE;
/*!40000 ALTER TABLE `venda` DISABLE KEYS */;
/*!40000 ALTER TABLE `venda` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `venda_produto`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `venda_produto` (
  `venda_id` bigint NOT NULL,
  `produto_id` bigint NOT NULL,
  KEY `FK4go8k4gnl96q60tpreyxc1d88` (`produto_id`),
  KEY `FKonh0ak31073oc57xyeu2etdx4` (`venda_id`),
  CONSTRAINT `FK4go8k4gnl96q60tpreyxc1d88` FOREIGN KEY (`produto_id`) REFERENCES `produto` (`id`),
  CONSTRAINT `FKonh0ak31073oc57xyeu2etdx4` FOREIGN KEY (`venda_id`) REFERENCES `venda` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `venda_produto` WRITE;
/*!40000 ALTER TABLE `venda_produto` DISABLE KEYS */;
/*!40000 ALTER TABLE `venda_produto` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

