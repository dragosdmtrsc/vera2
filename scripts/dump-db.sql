-- MySQL dump 10.14  Distrib 5.5.54-MariaDB, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: localhost
-- ------------------------------------------------------
-- Server version	5.5.54-MariaDB-1ubuntu0.14.04.1

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
-- Current Database: `nova`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `nova` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `nova`;

--
-- Table structure for table `agent_builds`
--

DROP TABLE IF EXISTS `agent_builds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agent_builds` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hypervisor` varchar(255) DEFAULT NULL,
  `os` varchar(255) DEFAULT NULL,
  `architecture` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `md5hash` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_agent_builds0hypervisor0os0architecture0deleted` (`hypervisor`,`os`,`architecture`,`deleted`),
  KEY `agent_builds_hypervisor_os_arch_idx` (`hypervisor`,`os`,`architecture`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agent_builds`
--

LOCK TABLES `agent_builds` WRITE;
/*!40000 ALTER TABLE `agent_builds` DISABLE KEYS */;
/*!40000 ALTER TABLE `agent_builds` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aggregate_hosts`
--

DROP TABLE IF EXISTS `aggregate_hosts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `aggregate_hosts` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) DEFAULT NULL,
  `aggregate_id` int(11) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_aggregate_hosts0host0aggregate_id0deleted` (`host`,`aggregate_id`,`deleted`),
  KEY `aggregate_id` (`aggregate_id`),
  CONSTRAINT `aggregate_hosts_ibfk_1` FOREIGN KEY (`aggregate_id`) REFERENCES `aggregates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aggregate_hosts`
--

LOCK TABLES `aggregate_hosts` WRITE;
/*!40000 ALTER TABLE `aggregate_hosts` DISABLE KEYS */;
/*!40000 ALTER TABLE `aggregate_hosts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aggregate_metadata`
--

DROP TABLE IF EXISTS `aggregate_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `aggregate_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `aggregate_id` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_aggregate_metadata0aggregate_id0key0deleted` (`aggregate_id`,`key`,`deleted`),
  KEY `aggregate_metadata_key_idx` (`key`),
  CONSTRAINT `aggregate_metadata_ibfk_1` FOREIGN KEY (`aggregate_id`) REFERENCES `aggregates` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aggregate_metadata`
--

LOCK TABLES `aggregate_metadata` WRITE;
/*!40000 ALTER TABLE `aggregate_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `aggregate_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `aggregates`
--

DROP TABLE IF EXISTS `aggregates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `aggregates` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `aggregate_uuid_idx` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `aggregates`
--

LOCK TABLES `aggregates` WRITE;
/*!40000 ALTER TABLE `aggregates` DISABLE KEYS */;
/*!40000 ALTER TABLE `aggregates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `allocations`
--

DROP TABLE IF EXISTS `allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `allocations` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_provider_id` int(11) NOT NULL,
  `consumer_id` varchar(36) NOT NULL,
  `resource_class_id` int(11) NOT NULL,
  `used` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `allocations_consumer_id_idx` (`consumer_id`),
  KEY `allocations_resource_class_id_idx` (`resource_class_id`),
  KEY `allocations_resource_provider_class_used_idx` (`resource_provider_id`,`resource_class_id`,`used`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `allocations`
--

LOCK TABLES `allocations` WRITE;
/*!40000 ALTER TABLE `allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `block_device_mapping`
--

DROP TABLE IF EXISTS `block_device_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `block_device_mapping` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) DEFAULT NULL,
  `delete_on_termination` tinyint(1) DEFAULT NULL,
  `snapshot_id` varchar(36) DEFAULT NULL,
  `volume_id` varchar(36) DEFAULT NULL,
  `volume_size` int(11) DEFAULT NULL,
  `no_device` tinyint(1) DEFAULT NULL,
  `connection_info` mediumtext,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `source_type` varchar(255) DEFAULT NULL,
  `destination_type` varchar(255) DEFAULT NULL,
  `guest_format` varchar(255) DEFAULT NULL,
  `device_type` varchar(255) DEFAULT NULL,
  `disk_bus` varchar(255) DEFAULT NULL,
  `boot_index` int(11) DEFAULT NULL,
  `image_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `snapshot_id` (`snapshot_id`),
  KEY `volume_id` (`volume_id`),
  KEY `block_device_mapping_instance_uuid_idx` (`instance_uuid`),
  KEY `block_device_mapping_instance_uuid_device_name_idx` (`instance_uuid`,`device_name`),
  KEY `block_device_mapping_instance_uuid_volume_id_idx` (`instance_uuid`,`volume_id`),
  CONSTRAINT `block_device_mapping_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `block_device_mapping`
--

LOCK TABLES `block_device_mapping` WRITE;
/*!40000 ALTER TABLE `block_device_mapping` DISABLE KEYS */;
INSERT INTO `block_device_mapping` VALUES ('2017-03-07 14:47:46','2017-03-07 14:47:47',NULL,1,'/dev/vda',1,NULL,NULL,NULL,0,NULL,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59',0,'image','local',NULL,'disk',NULL,0,'0c4eb282-7baf-4ac1-aea0-c4205a3abe22');
/*!40000 ALTER TABLE `block_device_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bw_usage_cache`
--

DROP TABLE IF EXISTS `bw_usage_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bw_usage_cache` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `start_period` datetime NOT NULL,
  `last_refreshed` datetime DEFAULT NULL,
  `bw_in` bigint(20) DEFAULT NULL,
  `bw_out` bigint(20) DEFAULT NULL,
  `mac` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `last_ctr_in` bigint(20) DEFAULT NULL,
  `last_ctr_out` bigint(20) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `bw_usage_cache_uuid_start_period_idx` (`uuid`,`start_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bw_usage_cache`
--

LOCK TABLES `bw_usage_cache` WRITE;
/*!40000 ALTER TABLE `bw_usage_cache` DISABLE KEYS */;
/*!40000 ALTER TABLE `bw_usage_cache` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cells`
--

DROP TABLE IF EXISTS `cells`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cells` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `api_url` varchar(255) DEFAULT NULL,
  `weight_offset` float DEFAULT NULL,
  `weight_scale` float DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `is_parent` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `transport_url` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_cells0name0deleted` (`name`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cells`
--

LOCK TABLES `cells` WRITE;
/*!40000 ALTER TABLE `cells` DISABLE KEYS */;
/*!40000 ALTER TABLE `cells` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `certificates`
--

DROP TABLE IF EXISTS `certificates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `certificates` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `certificates_project_id_deleted_idx` (`project_id`,`deleted`),
  KEY `certificates_user_id_deleted_idx` (`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `certificates`
--

LOCK TABLES `certificates` WRITE;
/*!40000 ALTER TABLE `certificates` DISABLE KEYS */;
/*!40000 ALTER TABLE `certificates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `compute_nodes`
--

DROP TABLE IF EXISTS `compute_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `compute_nodes` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `service_id` int(11) DEFAULT NULL,
  `vcpus` int(11) NOT NULL,
  `memory_mb` int(11) NOT NULL,
  `local_gb` int(11) NOT NULL,
  `vcpus_used` int(11) NOT NULL,
  `memory_mb_used` int(11) NOT NULL,
  `local_gb_used` int(11) NOT NULL,
  `hypervisor_type` mediumtext NOT NULL,
  `hypervisor_version` int(11) NOT NULL,
  `cpu_info` mediumtext NOT NULL,
  `disk_available_least` int(11) DEFAULT NULL,
  `free_ram_mb` int(11) DEFAULT NULL,
  `free_disk_gb` int(11) DEFAULT NULL,
  `current_workload` int(11) DEFAULT NULL,
  `running_vms` int(11) DEFAULT NULL,
  `hypervisor_hostname` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `host_ip` varchar(39) DEFAULT NULL,
  `supported_instances` text,
  `pci_stats` text,
  `metrics` text,
  `extra_resources` text,
  `stats` text,
  `numa_topology` text,
  `host` varchar(255) DEFAULT NULL,
  `ram_allocation_ratio` float DEFAULT NULL,
  `cpu_allocation_ratio` float DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `disk_allocation_ratio` float DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_compute_nodes0host0hypervisor_hostname0deleted` (`host`,`hypervisor_hostname`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `compute_nodes`
--

LOCK TABLES `compute_nodes` WRITE;
/*!40000 ALTER TABLE `compute_nodes` DISABLE KEYS */;
INSERT INTO `compute_nodes` VALUES ('2017-03-01 13:19:46','2017-04-02 08:14:31',NULL,1,NULL,1,2001,39,1,1024,1,'QEMU',2005000,'{\"vendor\": \"Intel\", \"model\": \"Westmere\", \"arch\": \"x86_64\", \"features\": [\"pge\", \"avx\", \"clflush\", \"sep\", \"syscall\", \"vme\", \"tsc\", \"xsave\", \"cmov\", \"fpu\", \"pat\", \"monitor\", \"lm\", \"msr\", \"nx\", \"fxsr\", \"sse4.1\", \"pae\", \"sse4.2\", \"pclmuldq\", \"mmx\", \"osxsave\", \"cx8\", \"mce\", \"de\", \"rdtscp\", \"mca\", \"pse\", \"pni\", \"abm\", \"popcnt\", \"apic\", \"sse\", \"invtsc\", \"lahf_lm\", \"aes\", \"sse2\", \"ssse3\", \"cx16\", \"pse36\", \"mtrr\", \"movbe\", \"rdrand\"], \"topology\": {\"cores\": 1, \"cells\": 1, \"threads\": 1, \"sockets\": 1}}',34,977,38,0,1,'compute1',0,'172.16.0.31','[[\"alpha\", \"qemu\", \"hvm\"], [\"armv7l\", \"qemu\", \"hvm\"], [\"aarch64\", \"qemu\", \"hvm\"], [\"cris\", \"qemu\", \"hvm\"], [\"i686\", \"qemu\", \"hvm\"], [\"lm32\", \"qemu\", \"hvm\"], [\"m68k\", \"qemu\", \"hvm\"], [\"microblaze\", \"qemu\", \"hvm\"], [\"microblazeel\", \"qemu\", \"hvm\"], [\"mips\", \"qemu\", \"hvm\"], [\"mipsel\", \"qemu\", \"hvm\"], [\"mips64\", \"qemu\", \"hvm\"], [\"mips64el\", \"qemu\", \"hvm\"], [\"openrisc\", \"qemu\", \"hvm\"], [\"ppc\", \"qemu\", \"hvm\"], [\"ppc64\", \"qemu\", \"hvm\"], [\"ppc64le\", \"qemu\", \"hvm\"], [\"ppcemb\", \"qemu\", \"hvm\"], [\"sh4\", \"qemu\", \"hvm\"], [\"sh4eb\", \"qemu\", \"hvm\"], [\"sparc\", \"qemu\", \"hvm\"], [\"sparc64\", \"qemu\", \"hvm\"], [\"unicore32\", \"qemu\", \"hvm\"], [\"x86_64\", \"qemu\", \"hvm\"], [\"xtensa\", \"qemu\", \"hvm\"], [\"xtensaeb\", \"qemu\", \"hvm\"]]','{\"nova_object.version\": \"1.1\", \"nova_object.changes\": [\"objects\"], \"nova_object.name\": \"PciDevicePoolList\", \"nova_object.data\": {\"objects\": []}, \"nova_object.namespace\": \"nova\"}','[]',NULL,'{\"num_task_None\": \"1\", \"io_workload\": \"0\", \"num_instances\": \"1\", \"num_proj_68bed08176254214900d5d6112eb3284\": \"1\", \"num_os_type_None\": \"1\", \"num_vm_active\": \"1\"}','{\"nova_object.version\": \"1.2\", \"nova_object.changes\": [\"cells\"], \"nova_object.name\": \"NUMATopology\", \"nova_object.data\": {\"cells\": [{\"nova_object.version\": \"1.2\", \"nova_object.changes\": [\"cpu_usage\", \"memory_usage\", \"cpuset\", \"pinned_cpus\", \"siblings\", \"memory\", \"mempages\", \"id\"], \"nova_object.name\": \"NUMACell\", \"nova_object.data\": {\"cpu_usage\": 0, \"memory_usage\": 0, \"cpuset\": [0], \"pinned_cpus\": [], \"siblings\": [], \"memory\": 2001, \"mempages\": [{\"nova_object.version\": \"1.0\", \"nova_object.changes\": [\"total\", \"size_kb\", \"used\"], \"nova_object.name\": \"NUMAPagesTopology\", \"nova_object.data\": {\"total\": 512491, \"used\": 0, \"size_kb\": 4}, \"nova_object.namespace\": \"nova\"}, {\"nova_object.version\": \"1.0\", \"nova_object.changes\": [\"total\", \"size_kb\", \"used\"], \"nova_object.name\": \"NUMAPagesTopology\", \"nova_object.data\": {\"total\": 0, \"used\": 0, \"size_kb\": 2048}, \"nova_object.namespace\": \"nova\"}], \"id\": 0}, \"nova_object.namespace\": \"nova\"}]}, \"nova_object.namespace\": \"nova\"}','compute1',0,0,'d6af491c-a374-4400-8d42-1f11284bd89a',0);
/*!40000 ALTER TABLE `compute_nodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `console_pools`
--

DROP TABLE IF EXISTS `console_pools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `console_pools` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `console_type` varchar(255) DEFAULT NULL,
  `public_hostname` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `compute_host` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_console_pools0host0console_type0compute_host0deleted` (`host`,`console_type`,`compute_host`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `console_pools`
--

LOCK TABLES `console_pools` WRITE;
/*!40000 ALTER TABLE `console_pools` DISABLE KEYS */;
/*!40000 ALTER TABLE `console_pools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `consoles`
--

DROP TABLE IF EXISTS `consoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `consoles` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `pool_id` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `pool_id` (`pool_id`),
  KEY `consoles_instance_uuid_idx` (`instance_uuid`),
  CONSTRAINT `consoles_ibfk_1` FOREIGN KEY (`pool_id`) REFERENCES `console_pools` (`id`),
  CONSTRAINT `consoles_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consoles`
--

LOCK TABLES `consoles` WRITE;
/*!40000 ALTER TABLE `consoles` DISABLE KEYS */;
/*!40000 ALTER TABLE `consoles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dns_domains`
--

DROP TABLE IF EXISTS `dns_domains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dns_domains` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT NULL,
  `domain` varchar(255) NOT NULL,
  `scope` varchar(255) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`domain`),
  KEY `dns_domains_domain_deleted_idx` (`domain`,`deleted`),
  KEY `dns_domains_project_id_idx` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dns_domains`
--

LOCK TABLES `dns_domains` WRITE;
/*!40000 ALTER TABLE `dns_domains` DISABLE KEYS */;
/*!40000 ALTER TABLE `dns_domains` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fixed_ips`
--

DROP TABLE IF EXISTS `fixed_ips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fixed_ips` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `network_id` int(11) DEFAULT NULL,
  `allocated` tinyint(1) DEFAULT NULL,
  `leased` tinyint(1) DEFAULT NULL,
  `reserved` tinyint(1) DEFAULT NULL,
  `virtual_interface_id` int(11) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_fixed_ips0address0deleted` (`address`,`deleted`),
  KEY `network_id` (`network_id`),
  KEY `fixed_ips_virtual_interface_id_fkey` (`virtual_interface_id`),
  KEY `address` (`address`),
  KEY `fixed_ips_instance_uuid_fkey` (`instance_uuid`),
  KEY `fixed_ips_host_idx` (`host`),
  KEY `fixed_ips_network_id_host_deleted_idx` (`network_id`,`host`,`deleted`),
  KEY `fixed_ips_address_reserved_network_id_deleted_idx` (`address`,`reserved`,`network_id`,`deleted`),
  KEY `fixed_ips_deleted_allocated_idx` (`address`,`deleted`,`allocated`),
  KEY `fixed_ips_deleted_allocated_updated_at_idx` (`deleted`,`allocated`,`updated_at`),
  CONSTRAINT `fixed_ips_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fixed_ips`
--

LOCK TABLES `fixed_ips` WRITE;
/*!40000 ALTER TABLE `fixed_ips` DISABLE KEYS */;
/*!40000 ALTER TABLE `fixed_ips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `floating_ips`
--

DROP TABLE IF EXISTS `floating_ips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `floating_ips` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `fixed_ip_id` int(11) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `auto_assigned` tinyint(1) DEFAULT NULL,
  `pool` varchar(255) DEFAULT NULL,
  `interface` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_floating_ips0address0deleted` (`address`,`deleted`),
  KEY `fixed_ip_id` (`fixed_ip_id`),
  KEY `floating_ips_host_idx` (`host`),
  KEY `floating_ips_project_id_idx` (`project_id`),
  KEY `floating_ips_pool_deleted_fixed_ip_id_project_id_idx` (`pool`,`deleted`,`fixed_ip_id`,`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `floating_ips`
--

LOCK TABLES `floating_ips` WRITE;
/*!40000 ALTER TABLE `floating_ips` DISABLE KEYS */;
/*!40000 ALTER TABLE `floating_ips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_actions`
--

DROP TABLE IF EXISTS `instance_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_actions` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `action` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `request_id` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `instance_uuid_idx` (`instance_uuid`),
  KEY `request_id_idx` (`request_id`),
  CONSTRAINT `fk_instance_actions_instance_uuid` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_actions`
--

LOCK TABLES `instance_actions` WRITE;
/*!40000 ALTER TABLE `instance_actions` DISABLE KEYS */;
INSERT INTO `instance_actions` VALUES ('2017-03-07 14:47:46',NULL,NULL,1,'create','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-61264dfa-828b-4b5c-a1f2-edbe1c700f88','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','2017-03-07 14:47:44',NULL,NULL,0),('2017-03-07 21:33:06',NULL,NULL,2,'stop','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-95d683bf-152a-49f8-9be7-eadd0e71320b',NULL,NULL,'2017-03-07 21:33:06',NULL,NULL,0),('2017-03-08 14:13:15',NULL,NULL,3,'start','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-8c2f48c1-8402-4103-9d7d-3c3be435a309','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','2017-03-08 14:13:15',NULL,NULL,0),('2017-03-08 16:30:09',NULL,NULL,4,'stop','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-33f874cd-e5a8-41b7-a319-ba810dbc9261',NULL,NULL,'2017-03-08 16:30:09',NULL,NULL,0),('2017-03-08 16:32:46',NULL,NULL,5,'start','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-6d8d378e-9dcf-4091-aa17-6f6c67600200','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','2017-03-08 16:32:45',NULL,NULL,0),('2017-03-09 14:53:31',NULL,NULL,6,'stop','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-dafa74ea-fd48-4199-a12c-c71189e616b6',NULL,NULL,'2017-03-09 14:53:31',NULL,NULL,0),('2017-04-02 08:00:40',NULL,NULL,7,'start','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','req-ad360e41-2f0d-468e-9b47-e3b352184c23','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','2017-04-02 08:00:40',NULL,NULL,0);
/*!40000 ALTER TABLE `instance_actions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_actions_events`
--

DROP TABLE IF EXISTS `instance_actions_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_actions_events` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `event` varchar(255) DEFAULT NULL,
  `action_id` int(11) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `traceback` text,
  `deleted` int(11) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `details` text,
  PRIMARY KEY (`id`),
  KEY `action_id` (`action_id`),
  CONSTRAINT `instance_actions_events_ibfk_1` FOREIGN KEY (`action_id`) REFERENCES `instance_actions` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_actions_events`
--

LOCK TABLES `instance_actions_events` WRITE;
/*!40000 ALTER TABLE `instance_actions_events` DISABLE KEYS */;
INSERT INTO `instance_actions_events` VALUES ('2017-03-07 14:47:47','2017-03-07 14:47:59',NULL,1,'compute__do_build_and_run_instance',1,'2017-03-07 14:47:47','2017-03-07 14:47:59','Success',NULL,0,NULL,NULL),('2017-03-07 21:33:06','2017-03-07 21:33:07',NULL,2,'compute_stop_instance',2,'2017-03-07 21:33:06','2017-03-07 21:33:07','Success',NULL,0,NULL,NULL),('2017-03-08 14:13:16','2017-03-08 14:13:39',NULL,3,'compute_start_instance',3,'2017-03-08 14:13:16','2017-03-08 14:13:39','Success',NULL,0,NULL,NULL),('2017-03-08 16:30:09','2017-03-08 16:30:09',NULL,4,'compute_stop_instance',4,'2017-03-08 16:30:09','2017-03-08 16:30:09','Success',NULL,0,NULL,NULL),('2017-03-08 16:32:46','2017-03-08 16:32:51',NULL,5,'compute_start_instance',5,'2017-03-08 16:32:46','2017-03-08 16:32:51','Success',NULL,0,NULL,NULL),('2017-03-09 14:53:31','2017-03-09 14:53:32',NULL,6,'compute_stop_instance',6,'2017-03-09 14:53:31','2017-03-09 14:53:32','Success',NULL,0,NULL,NULL),('2017-04-02 08:00:40','2017-04-02 08:00:46',NULL,7,'compute_start_instance',7,'2017-04-02 08:00:40','2017-04-02 08:00:45','Success',NULL,0,NULL,NULL);
/*!40000 ALTER TABLE `instance_actions_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_extra`
--

DROP TABLE IF EXISTS `instance_extra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_extra` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) NOT NULL,
  `numa_topology` text,
  `pci_requests` text,
  `flavor` text,
  `vcpu_model` text,
  `migration_context` text,
  PRIMARY KEY (`id`),
  KEY `instance_extra_idx` (`instance_uuid`),
  CONSTRAINT `instance_extra_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_extra`
--

LOCK TABLES `instance_extra` WRITE;
/*!40000 ALTER TABLE `instance_extra` DISABLE KEYS */;
INSERT INTO `instance_extra` VALUES ('2017-03-07 14:47:46','2017-04-02 08:00:45',NULL,0,1,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59',NULL,'[]','{\"new\": null, \"old\": null, \"cur\": {\"nova_object.version\": \"1.1\", \"nova_object.name\": \"Flavor\", \"nova_object.data\": {\"disabled\": false, \"root_gb\": 1, \"name\": \"m1.tiny\", \"flavorid\": \"1\", \"deleted\": false, \"created_at\": null, \"ephemeral_gb\": 0, \"updated_at\": null, \"memory_mb\": 512, \"vcpus\": 1, \"extra_specs\": {}, \"swap\": 0, \"rxtx_factor\": 1.0, \"is_public\": true, \"deleted_at\": null, \"vcpu_weight\": 0, \"id\": 2}, \"nova_object.namespace\": \"nova\"}}','{\"nova_object.version\": \"1.0\", \"nova_object.changes\": [\"vendor\", \"features\", \"model\", \"topology\", \"arch\", \"match\", \"mode\"], \"nova_object.name\": \"VirtCPUModel\", \"nova_object.data\": {\"vendor\": null, \"features\": [], \"mode\": \"host-model\", \"model\": null, \"arch\": null, \"match\": \"exact\", \"topology\": {\"nova_object.version\": \"1.0\", \"nova_object.changes\": [\"cores\", \"threads\", \"sockets\"], \"nova_object.name\": \"VirtCPUTopology\", \"nova_object.data\": {\"cores\": 1, \"threads\": 1, \"sockets\": 1}, \"nova_object.namespace\": \"nova\"}}, \"nova_object.namespace\": \"nova\"}',NULL);
/*!40000 ALTER TABLE `instance_extra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_faults`
--

DROP TABLE IF EXISTS `instance_faults`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_faults` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `code` int(11) NOT NULL,
  `message` varchar(255) DEFAULT NULL,
  `details` mediumtext,
  `host` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `instance_faults_host_idx` (`host`),
  KEY `instance_faults_instance_uuid_deleted_created_at_idx` (`instance_uuid`,`deleted`,`created_at`),
  CONSTRAINT `fk_instance_faults_instance_uuid` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_faults`
--

LOCK TABLES `instance_faults` WRITE;
/*!40000 ALTER TABLE `instance_faults` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_faults` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_group_member`
--

DROP TABLE IF EXISTS `instance_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_group_member` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_id` varchar(255) DEFAULT NULL,
  `group_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `instance_group_member_instance_idx` (`instance_id`),
  CONSTRAINT `instance_group_member_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `instance_groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_group_member`
--

LOCK TABLES `instance_group_member` WRITE;
/*!40000 ALTER TABLE `instance_group_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_group_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_group_policy`
--

DROP TABLE IF EXISTS `instance_group_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_group_policy` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `policy` varchar(255) DEFAULT NULL,
  `group_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  KEY `instance_group_policy_policy_idx` (`policy`),
  CONSTRAINT `instance_group_policy_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `instance_groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_group_policy`
--

LOCK TABLES `instance_group_policy` WRITE;
/*!40000 ALTER TABLE `instance_group_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_group_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_groups`
--

DROP TABLE IF EXISTS `instance_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_groups` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_instance_groups0uuid0deleted` (`uuid`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_groups`
--

LOCK TABLES `instance_groups` WRITE;
/*!40000 ALTER TABLE `instance_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_id_mappings`
--

DROP TABLE IF EXISTS `instance_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_instance_id_mappings_uuid` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_id_mappings`
--

LOCK TABLES `instance_id_mappings` WRITE;
/*!40000 ALTER TABLE `instance_id_mappings` DISABLE KEYS */;
INSERT INTO `instance_id_mappings` VALUES ('2017-03-07 14:47:46',NULL,NULL,1,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59',0);
/*!40000 ALTER TABLE `instance_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_info_caches`
--

DROP TABLE IF EXISTS `instance_info_caches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_info_caches` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `network_info` mediumtext,
  `instance_uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_instance_info_caches0instance_uuid` (`instance_uuid`),
  CONSTRAINT `instance_info_caches_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_info_caches`
--

LOCK TABLES `instance_info_caches` WRITE;
/*!40000 ALTER TABLE `instance_info_caches` DISABLE KEYS */;
INSERT INTO `instance_info_caches` VALUES ('2017-03-07 14:47:46','2017-03-08 15:48:28',NULL,1,'[{\"profile\": {}, \"ovs_interfaceid\": \"737565c0-2c6d-4bab-9985-8f3a1c069881\", \"preserve_on_delete\": false, \"network\": {\"bridge\": \"br-int\", \"subnets\": [{\"ips\": [{\"meta\": {}, \"version\": 4, \"type\": \"fixed\", \"floating_ips\": [{\"meta\": {}, \"version\": 4, \"type\": \"floating\", \"address\": \"203.0.113.103\"}], \"address\": \"192.168.13.3\"}], \"version\": 4, \"meta\": {\"dhcp_server\": \"192.168.13.2\"}, \"dns\": [{\"meta\": {}, \"version\": 4, \"type\": \"dns\", \"address\": \"8.8.8.8\"}], \"routes\": [], \"cidr\": \"192.168.13.0/24\", \"gateway\": {\"meta\": {}, \"version\": 4, \"type\": \"gateway\", \"address\": \"192.168.13.1\"}}], \"meta\": {\"injected\": false, \"tenant_id\": \"68bed08176254214900d5d6112eb3284\", \"mtu\": 1450}, \"id\": \"a19a8724-c0e4-4944-a636-f4a166537786\", \"label\": \"user1net\"}, \"devname\": \"tap737565c0-2c\", \"vnic_type\": \"normal\", \"qbh_params\": null, \"meta\": {}, \"details\": {\"port_filter\": true, \"ovs_hybrid_plug\": false}, \"address\": \"fa:16:3e:42:5d:ed\", \"active\": true, \"type\": \"ovs\", \"id\": \"737565c0-2c6d-4bab-9985-8f3a1c069881\", \"qbg_params\": null}]','7b7d261d-2eb6-4dc2-8353-958e4f10ea59',0);
/*!40000 ALTER TABLE `instance_info_caches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_metadata`
--

DROP TABLE IF EXISTS `instance_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `instance_metadata_instance_uuid_idx` (`instance_uuid`),
  CONSTRAINT `instance_metadata_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_metadata`
--

LOCK TABLES `instance_metadata` WRITE;
/*!40000 ALTER TABLE `instance_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_system_metadata`
--

DROP TABLE IF EXISTS `instance_system_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_system_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `instance_uuid` (`instance_uuid`),
  CONSTRAINT `instance_system_metadata_ibfk_1` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_system_metadata`
--

LOCK TABLES `instance_system_metadata` WRITE;
/*!40000 ALTER TABLE `instance_system_metadata` DISABLE KEYS */;
INSERT INTO `instance_system_metadata` VALUES ('2017-03-07 14:47:46',NULL,NULL,1,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59','image_disk_format','qcow2',0),('2017-03-07 14:47:46',NULL,NULL,2,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59','image_min_ram','0',0),('2017-03-07 14:47:46',NULL,NULL,3,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59','image_min_disk','1',0),('2017-03-07 14:47:46',NULL,NULL,4,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59','image_base_image_ref','0c4eb282-7baf-4ac1-aea0-c4205a3abe22',0),('2017-03-07 14:47:46',NULL,NULL,5,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59','image_container_format','bare',0);
/*!40000 ALTER TABLE `instance_system_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_type_extra_specs`
--

DROP TABLE IF EXISTS `instance_type_extra_specs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_type_extra_specs` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_type_id` int(11) NOT NULL,
  `key` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `value` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_instance_type_extra_specs0instance_type_id0key0deleted` (`instance_type_id`,`key`,`deleted`),
  KEY `instance_type_extra_specs_instance_type_id_key_idx` (`instance_type_id`,`key`),
  CONSTRAINT `instance_type_extra_specs_ibfk_1` FOREIGN KEY (`instance_type_id`) REFERENCES `instance_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_type_extra_specs`
--

LOCK TABLES `instance_type_extra_specs` WRITE;
/*!40000 ALTER TABLE `instance_type_extra_specs` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_type_extra_specs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_type_projects`
--

DROP TABLE IF EXISTS `instance_type_projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_type_projects` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_type_id` int(11) NOT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_instance_type_projects0instance_type_id0project_id0deleted` (`instance_type_id`,`project_id`,`deleted`),
  KEY `instance_type_id` (`instance_type_id`),
  CONSTRAINT `instance_type_projects_ibfk_1` FOREIGN KEY (`instance_type_id`) REFERENCES `instance_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_type_projects`
--

LOCK TABLES `instance_type_projects` WRITE;
/*!40000 ALTER TABLE `instance_type_projects` DISABLE KEYS */;
/*!40000 ALTER TABLE `instance_type_projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instance_types`
--

DROP TABLE IF EXISTS `instance_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instance_types` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `memory_mb` int(11) NOT NULL,
  `vcpus` int(11) NOT NULL,
  `swap` int(11) NOT NULL,
  `vcpu_weight` int(11) DEFAULT NULL,
  `flavorid` varchar(255) DEFAULT NULL,
  `rxtx_factor` float DEFAULT NULL,
  `root_gb` int(11) DEFAULT NULL,
  `ephemeral_gb` int(11) DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `is_public` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_instance_types0name0deleted` (`name`,`deleted`),
  UNIQUE KEY `uniq_instance_types0flavorid0deleted` (`flavorid`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instance_types`
--

LOCK TABLES `instance_types` WRITE;
/*!40000 ALTER TABLE `instance_types` DISABLE KEYS */;
INSERT INTO `instance_types` VALUES (NULL,NULL,NULL,'m1.medium',1,4096,2,0,NULL,'3',1,40,0,0,1,0),(NULL,NULL,NULL,'m1.tiny',2,512,1,0,NULL,'1',1,1,0,0,1,0),(NULL,NULL,NULL,'m1.large',3,8192,4,0,NULL,'4',1,80,0,0,1,0),(NULL,NULL,NULL,'m1.xlarge',4,16384,8,0,NULL,'5',1,160,0,0,1,0),(NULL,NULL,NULL,'m1.small',5,2048,1,0,NULL,'2',1,20,0,0,1,0),('2017-03-05 22:59:48',NULL,NULL,'m1.nano',6,64,1,0,NULL,'0',1,1,0,0,1,0);
/*!40000 ALTER TABLE `instance_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `instances`
--

DROP TABLE IF EXISTS `instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `instances` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `internal_id` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `image_ref` varchar(255) DEFAULT NULL,
  `kernel_id` varchar(255) DEFAULT NULL,
  `ramdisk_id` varchar(255) DEFAULT NULL,
  `launch_index` int(11) DEFAULT NULL,
  `key_name` varchar(255) DEFAULT NULL,
  `key_data` mediumtext,
  `power_state` int(11) DEFAULT NULL,
  `vm_state` varchar(255) DEFAULT NULL,
  `memory_mb` int(11) DEFAULT NULL,
  `vcpus` int(11) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `user_data` mediumtext,
  `reservation_id` varchar(255) DEFAULT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `launched_at` datetime DEFAULT NULL,
  `terminated_at` datetime DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `display_description` varchar(255) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  `locked` tinyint(1) DEFAULT NULL,
  `os_type` varchar(255) DEFAULT NULL,
  `launched_on` mediumtext,
  `instance_type_id` int(11) DEFAULT NULL,
  `vm_mode` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `architecture` varchar(255) DEFAULT NULL,
  `root_device_name` varchar(255) DEFAULT NULL,
  `access_ip_v4` varchar(39) DEFAULT NULL,
  `access_ip_v6` varchar(39) DEFAULT NULL,
  `config_drive` varchar(255) DEFAULT NULL,
  `task_state` varchar(255) DEFAULT NULL,
  `default_ephemeral_device` varchar(255) DEFAULT NULL,
  `default_swap_device` varchar(255) DEFAULT NULL,
  `progress` int(11) DEFAULT NULL,
  `auto_disk_config` tinyint(1) DEFAULT NULL,
  `shutdown_terminate` tinyint(1) DEFAULT NULL,
  `disable_terminate` tinyint(1) DEFAULT NULL,
  `root_gb` int(11) DEFAULT NULL,
  `ephemeral_gb` int(11) DEFAULT NULL,
  `cell_name` varchar(255) DEFAULT NULL,
  `node` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `locked_by` enum('owner','admin') DEFAULT NULL,
  `cleaned` int(11) DEFAULT NULL,
  `ephemeral_key_uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uuid` (`uuid`),
  UNIQUE KEY `uniq_instances0uuid` (`uuid`),
  KEY `instances_reservation_id_idx` (`reservation_id`),
  KEY `instances_terminated_at_launched_at_idx` (`terminated_at`,`launched_at`),
  KEY `instances_task_state_updated_at_idx` (`task_state`,`updated_at`),
  KEY `instances_uuid_deleted_idx` (`uuid`,`deleted`),
  KEY `instances_host_node_deleted_idx` (`host`,`node`,`deleted`),
  KEY `instances_host_deleted_cleaned_idx` (`host`,`deleted`,`cleaned`),
  KEY `instances_project_id_deleted_idx` (`project_id`,`deleted`),
  KEY `instances_deleted_created_at_idx` (`deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `instances`
--

LOCK TABLES `instances` WRITE;
/*!40000 ALTER TABLE `instances` DISABLE KEYS */;
INSERT INTO `instances` VALUES ('2017-03-07 14:47:46','2017-04-02 08:00:45',NULL,1,NULL,'ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','0c4eb282-7baf-4ac1-aea0-c4205a3abe22','','',0,'mykey','ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC3L+BbPXh0q+X+8MOAs9OghxsVecx0X+pZWQ10ioveTOzP1iXEwGpjTFO2HSej2FwG+aXTqxv1lW340c90ObUQZmON4Dub1WVQUPkjFRrl+gsAYAl3MBU44BujFIIO8wl+OwOXWbFL0u1N2AmOhlQp1Pcd5UkMXLgUXUOp8JKVH9G+ZlpUEjNPcK8lzRs67OKV1PxQxRff4oAh7o638gznhWGqZoMeki3RbNpFTpv6clRdTC2HSNRGCPMm1a+rXaZPvJGcLS+h+FOBmG06MQOk3b4ztinIHdJxWZ4ZWgKVBTkcVpbDMFzUQouKqJqZ+4Z3EV7yNl/c1EVhyinv/sO1 root@controller\n',1,'active',512,1,'selfservice-instance','compute1',NULL,'r-oxmdqix6',NULL,'2017-03-07 14:47:59',NULL,'selfservice-instance','selfservice-instance',NULL,0,NULL,'compute1',2,NULL,'7b7d261d-2eb6-4dc2-8353-958e4f10ea59',NULL,'/dev/vda',NULL,NULL,'',NULL,NULL,NULL,0,0,0,0,1,0,NULL,'compute1',0,NULL,0,NULL);
/*!40000 ALTER TABLE `instances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventories`
--

DROP TABLE IF EXISTS `inventories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `inventories` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `resource_provider_id` int(11) NOT NULL,
  `resource_class_id` int(11) NOT NULL,
  `total` int(11) NOT NULL,
  `reserved` int(11) NOT NULL,
  `min_unit` int(11) NOT NULL,
  `max_unit` int(11) NOT NULL,
  `step_size` int(11) NOT NULL,
  `allocation_ratio` float NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_inventories0resource_provider_resource_class` (`resource_provider_id`,`resource_class_id`),
  KEY `inventories_resource_provider_id_idx` (`resource_provider_id`),
  KEY `inventories_resource_class_id_idx` (`resource_class_id`),
  KEY `inventories_resource_provider_resource_class_idx` (`resource_provider_id`,`resource_class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventories`
--

LOCK TABLES `inventories` WRITE;
/*!40000 ALTER TABLE `inventories` DISABLE KEYS */;
/*!40000 ALTER TABLE `inventories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `key_pairs`
--

DROP TABLE IF EXISTS `key_pairs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `key_pairs` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `fingerprint` varchar(255) DEFAULT NULL,
  `public_key` mediumtext,
  `deleted` int(11) DEFAULT NULL,
  `type` enum('ssh','x509') NOT NULL DEFAULT 'ssh',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_key_pairs0user_id0name0deleted` (`user_id`,`name`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `key_pairs`
--

LOCK TABLES `key_pairs` WRITE;
/*!40000 ALTER TABLE `key_pairs` DISABLE KEYS */;
INSERT INTO `key_pairs` VALUES ('2017-03-05 23:00:23',NULL,NULL,1,'mykey','ac1e3b460bb14b059efb42332387b3b1','9f:a2:e8:06:f7:d6:6b:aa:03:b1:8f:ef:f0:a7:ec:a6','ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC3L+BbPXh0q+X+8MOAs9OghxsVecx0X+pZWQ10ioveTOzP1iXEwGpjTFO2HSej2FwG+aXTqxv1lW340c90ObUQZmON4Dub1WVQUPkjFRrl+gsAYAl3MBU44BujFIIO8wl+OwOXWbFL0u1N2AmOhlQp1Pcd5UkMXLgUXUOp8JKVH9G+ZlpUEjNPcK8lzRs67OKV1PxQxRff4oAh7o638gznhWGqZoMeki3RbNpFTpv6clRdTC2HSNRGCPMm1a+rXaZPvJGcLS+h+FOBmG06MQOk3b4ztinIHdJxWZ4ZWgKVBTkcVpbDMFzUQouKqJqZ+4Z3EV7yNl/c1EVhyinv/sO1 root@controller\n',0,'ssh'),('2017-03-05 23:00:58',NULL,NULL,2,'mykey','2676658a28e74ec3bd99c45854019610','dd:87:76:35:0b:11:74:2c:d7:f7:25:85:04:3a:47:ea','ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCxptFzJKCXMXvKQ8bHKYUPviD/nsdEwzY5C8FPGG8brg3VjRAV7BLdLuse9d0Sc6tSpl6tninid5JUQvSKHlDhELvdIvZ0iunZd9tm0hRZNU7BG78whfR5EKTvQlAgEfR+ObMQ/wD6ALDK4yv/PfYQMLiagpJSD/Dv7yXy7cdZs5nNoT4jhecXw4S7v53HQLZCRDxnZVkUjHoDazwEM8nBrGjKeFULZnJQ+zVI4FG8nBv5xmH+d3PaM9X02ZI84s6jkMnuf3HXvTn2qPY0NfrTIc9tHgHAVA8Wtf3AJLmhk9PWAf8Z3b6EkeeSPUIHoipT3CbJ8YQV7aSae/I0yeeP root@controller\n',0,'ssh');
/*!40000 ALTER TABLE `key_pairs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `migrate_version`
--

DROP TABLE IF EXISTS `migrate_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `migrate_version` (
  `repository_id` varchar(250) NOT NULL,
  `repository_path` text,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`repository_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `migrate_version`
--

LOCK TABLES `migrate_version` WRITE;
/*!40000 ALTER TABLE `migrate_version` DISABLE KEYS */;
INSERT INTO `migrate_version` VALUES ('nova','/usr/lib/python2.7/dist-packages/nova/db/sqlalchemy/migrate_repo',319);
/*!40000 ALTER TABLE `migrate_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `migrations`
--

DROP TABLE IF EXISTS `migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `migrations` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source_compute` varchar(255) DEFAULT NULL,
  `dest_compute` varchar(255) DEFAULT NULL,
  `dest_host` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `old_instance_type_id` int(11) DEFAULT NULL,
  `new_instance_type_id` int(11) DEFAULT NULL,
  `source_node` varchar(255) DEFAULT NULL,
  `dest_node` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `migration_type` enum('migration','resize','live-migration','evacuation') DEFAULT NULL,
  `hidden` tinyint(1) DEFAULT NULL,
  `memory_total` bigint(20) DEFAULT NULL,
  `memory_processed` bigint(20) DEFAULT NULL,
  `memory_remaining` bigint(20) DEFAULT NULL,
  `disk_total` bigint(20) DEFAULT NULL,
  `disk_processed` bigint(20) DEFAULT NULL,
  `disk_remaining` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `migrations_by_host_nodes_and_status_idx` (`deleted`,`source_compute`(100),`dest_compute`(100),`source_node`(100),`dest_node`(100),`status`),
  KEY `migrations_instance_uuid_and_status_idx` (`deleted`,`instance_uuid`,`status`),
  KEY `fk_migrations_instance_uuid` (`instance_uuid`),
  CONSTRAINT `fk_migrations_instance_uuid` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `migrations`
--

LOCK TABLES `migrations` WRITE;
/*!40000 ALTER TABLE `migrations` DISABLE KEYS */;
/*!40000 ALTER TABLE `migrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networks`
--

DROP TABLE IF EXISTS `networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networks` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `injected` tinyint(1) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `netmask` varchar(39) DEFAULT NULL,
  `bridge` varchar(255) DEFAULT NULL,
  `gateway` varchar(39) DEFAULT NULL,
  `broadcast` varchar(39) DEFAULT NULL,
  `dns1` varchar(39) DEFAULT NULL,
  `vlan` int(11) DEFAULT NULL,
  `vpn_public_address` varchar(39) DEFAULT NULL,
  `vpn_public_port` int(11) DEFAULT NULL,
  `vpn_private_address` varchar(39) DEFAULT NULL,
  `dhcp_start` varchar(39) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `cidr_v6` varchar(43) DEFAULT NULL,
  `gateway_v6` varchar(39) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `netmask_v6` varchar(39) DEFAULT NULL,
  `bridge_interface` varchar(255) DEFAULT NULL,
  `multi_host` tinyint(1) DEFAULT NULL,
  `dns2` varchar(39) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `rxtx_base` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `mtu` int(11) DEFAULT NULL,
  `dhcp_server` varchar(39) DEFAULT NULL,
  `enable_dhcp` tinyint(1) DEFAULT NULL,
  `share_address` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_networks0vlan0deleted` (`vlan`,`deleted`),
  KEY `networks_host_idx` (`host`),
  KEY `networks_cidr_v6_idx` (`cidr_v6`),
  KEY `networks_bridge_deleted_idx` (`bridge`,`deleted`),
  KEY `networks_project_id_deleted_idx` (`project_id`,`deleted`),
  KEY `networks_uuid_project_id_deleted_idx` (`uuid`,`project_id`,`deleted`),
  KEY `networks_vlan_deleted_idx` (`vlan`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networks`
--

LOCK TABLES `networks` WRITE;
/*!40000 ALTER TABLE `networks` DISABLE KEYS */;
/*!40000 ALTER TABLE `networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pci_devices`
--

DROP TABLE IF EXISTS `pci_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pci_devices` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `compute_node_id` int(11) NOT NULL,
  `address` varchar(12) NOT NULL,
  `product_id` varchar(4) NOT NULL,
  `vendor_id` varchar(4) NOT NULL,
  `dev_type` varchar(8) NOT NULL,
  `dev_id` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL,
  `status` varchar(36) NOT NULL,
  `extra_info` text,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `request_id` varchar(36) DEFAULT NULL,
  `numa_node` int(11) DEFAULT NULL,
  `parent_addr` varchar(12) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_pci_devices0compute_node_id0address0deleted` (`compute_node_id`,`address`,`deleted`),
  KEY `ix_pci_devices_compute_node_id_deleted` (`compute_node_id`,`deleted`),
  KEY `ix_pci_devices_instance_uuid_deleted` (`instance_uuid`,`deleted`),
  KEY `ix_pci_devices_compute_node_id_parent_addr_deleted` (`compute_node_id`,`parent_addr`,`deleted`),
  CONSTRAINT `pci_devices_compute_node_id_fkey` FOREIGN KEY (`compute_node_id`) REFERENCES `compute_nodes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pci_devices`
--

LOCK TABLES `pci_devices` WRITE;
/*!40000 ALTER TABLE `pci_devices` DISABLE KEYS */;
/*!40000 ALTER TABLE `pci_devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_user_quotas`
--

DROP TABLE IF EXISTS `project_user_quotas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_user_quotas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) NOT NULL,
  `project_id` varchar(255) NOT NULL,
  `resource` varchar(255) NOT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_project_user_quotas0user_id0project_id0resource0deleted` (`user_id`,`project_id`,`resource`,`deleted`),
  KEY `project_user_quotas_project_id_deleted_idx` (`project_id`,`deleted`),
  KEY `project_user_quotas_user_id_deleted_idx` (`user_id`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_user_quotas`
--

LOCK TABLES `project_user_quotas` WRITE;
/*!40000 ALTER TABLE `project_user_quotas` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_user_quotas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `provider_fw_rules`
--

DROP TABLE IF EXISTS `provider_fw_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `provider_fw_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protocol` varchar(5) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `provider_fw_rules`
--

LOCK TABLES `provider_fw_rules` WRITE;
/*!40000 ALTER TABLE `provider_fw_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `provider_fw_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quota_classes`
--

DROP TABLE IF EXISTS `quota_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quota_classes` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_quota_classes_class_name` (`class_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quota_classes`
--

LOCK TABLES `quota_classes` WRITE;
/*!40000 ALTER TABLE `quota_classes` DISABLE KEYS */;
/*!40000 ALTER TABLE `quota_classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quota_usages`
--

DROP TABLE IF EXISTS `quota_usages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quota_usages` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) NOT NULL,
  `in_use` int(11) NOT NULL,
  `reserved` int(11) NOT NULL,
  `until_refresh` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_quota_usages_project_id` (`project_id`),
  KEY `ix_quota_usages_user_id_deleted` (`user_id`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quota_usages`
--

LOCK TABLES `quota_usages` WRITE;
/*!40000 ALTER TABLE `quota_usages` DISABLE KEYS */;
INSERT INTO `quota_usages` VALUES ('2017-03-07 14:47:46','2017-03-07 14:47:46',NULL,1,'68bed08176254214900d5d6112eb3284','instances',1,0,NULL,0,'ac1e3b460bb14b059efb42332387b3b1'),('2017-03-07 14:47:46','2017-03-07 14:47:46',NULL,2,'68bed08176254214900d5d6112eb3284','ram',512,0,NULL,0,'ac1e3b460bb14b059efb42332387b3b1'),('2017-03-07 14:47:46','2017-03-07 14:47:46',NULL,3,'68bed08176254214900d5d6112eb3284','cores',1,0,NULL,0,'ac1e3b460bb14b059efb42332387b3b1'),('2017-03-07 14:47:46','2017-03-07 14:47:46',NULL,4,'68bed08176254214900d5d6112eb3284','security_groups',1,0,0,0,'ac1e3b460bb14b059efb42332387b3b1');
/*!40000 ALTER TABLE `quota_usages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotas`
--

DROP TABLE IF EXISTS `quotas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quotas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) NOT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_quotas0project_id0resource0deleted` (`project_id`,`resource`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotas`
--

LOCK TABLES `quotas` WRITE;
/*!40000 ALTER TABLE `quotas` DISABLE KEYS */;
/*!40000 ALTER TABLE `quotas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reservations` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `usage_id` int(11) NOT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `delta` int(11) NOT NULL,
  `expire` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `usage_id` (`usage_id`),
  KEY `ix_reservations_project_id` (`project_id`),
  KEY `ix_reservations_user_id_deleted` (`user_id`,`deleted`),
  KEY `reservations_uuid_idx` (`uuid`),
  KEY `reservations_deleted_expire_idx` (`deleted`,`expire`),
  CONSTRAINT `reservations_ibfk_1` FOREIGN KEY (`usage_id`) REFERENCES `quota_usages` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
INSERT INTO `reservations` VALUES ('2017-03-07 14:47:46',NULL,'2017-03-07 14:47:46',1,'de61d677-83a9-41bc-9d16-5bf3653fe06b',1,'68bed08176254214900d5d6112eb3284','instances',1,'2017-03-08 14:47:46',1,'ac1e3b460bb14b059efb42332387b3b1'),('2017-03-07 14:47:46',NULL,'2017-03-07 14:47:46',2,'07add777-a8d2-4d70-8307-507d6dd090e6',2,'68bed08176254214900d5d6112eb3284','ram',512,'2017-03-08 14:47:46',2,'ac1e3b460bb14b059efb42332387b3b1'),('2017-03-07 14:47:46',NULL,'2017-03-07 14:47:46',3,'e800d322-44bf-46ff-bc5f-be96fbe0e783',3,'68bed08176254214900d5d6112eb3284','cores',1,'2017-03-08 14:47:46',3,'ac1e3b460bb14b059efb42332387b3b1');
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_provider_aggregates`
--

DROP TABLE IF EXISTS `resource_provider_aggregates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_provider_aggregates` (
  `resource_provider_id` int(11) NOT NULL AUTO_INCREMENT,
  `aggregate_id` int(11) NOT NULL,
  PRIMARY KEY (`resource_provider_id`,`aggregate_id`),
  KEY `resource_provider_aggregates_aggregate_id_idx` (`aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_provider_aggregates`
--

LOCK TABLES `resource_provider_aggregates` WRITE;
/*!40000 ALTER TABLE `resource_provider_aggregates` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_provider_aggregates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resource_providers`
--

DROP TABLE IF EXISTS `resource_providers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resource_providers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `name` varchar(200) CHARACTER SET utf8 DEFAULT NULL,
  `generation` int(11) DEFAULT NULL,
  `can_host` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_resource_providers0uuid` (`uuid`),
  UNIQUE KEY `uniq_resource_providers0name` (`name`),
  KEY `resource_providers_uuid_idx` (`uuid`),
  KEY `resource_providers_name_idx` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resource_providers`
--

LOCK TABLES `resource_providers` WRITE;
/*!40000 ALTER TABLE `resource_providers` DISABLE KEYS */;
/*!40000 ALTER TABLE `resource_providers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `s3_images`
--

DROP TABLE IF EXISTS `s3_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `s3_images` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `s3_images`
--

LOCK TABLES `s3_images` WRITE;
/*!40000 ALTER TABLE `s3_images` DISABLE KEYS */;
INSERT INTO `s3_images` VALUES ('2017-03-07 14:47:46',NULL,NULL,1,'0c4eb282-7baf-4ac1-aea0-c4205a3abe22',0);
/*!40000 ALTER TABLE `s3_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_group_default_rules`
--

DROP TABLE IF EXISTS `security_group_default_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_group_default_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protocol` varchar(5) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_group_default_rules`
--

LOCK TABLES `security_group_default_rules` WRITE;
/*!40000 ALTER TABLE `security_group_default_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `security_group_default_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_group_instance_association`
--

DROP TABLE IF EXISTS `security_group_instance_association`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_group_instance_association` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `security_group_id` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `security_group_id` (`security_group_id`),
  KEY `security_group_instance_association_instance_uuid_idx` (`instance_uuid`),
  CONSTRAINT `security_group_instance_association_ibfk_1` FOREIGN KEY (`security_group_id`) REFERENCES `security_groups` (`id`),
  CONSTRAINT `security_group_instance_association_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_group_instance_association`
--

LOCK TABLES `security_group_instance_association` WRITE;
/*!40000 ALTER TABLE `security_group_instance_association` DISABLE KEYS */;
/*!40000 ALTER TABLE `security_group_instance_association` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_group_rules`
--

DROP TABLE IF EXISTS `security_group_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_group_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_group_id` int(11) DEFAULT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `group_id` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parent_group_id` (`parent_group_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `security_group_rules_ibfk_1` FOREIGN KEY (`parent_group_id`) REFERENCES `security_groups` (`id`),
  CONSTRAINT `security_group_rules_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `security_groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_group_rules`
--

LOCK TABLES `security_group_rules` WRITE;
/*!40000 ALTER TABLE `security_group_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `security_group_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `security_groups`
--

DROP TABLE IF EXISTS `security_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `security_groups` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_security_groups0project_id0name0deleted` (`project_id`,`name`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `security_groups`
--

LOCK TABLES `security_groups` WRITE;
/*!40000 ALTER TABLE `security_groups` DISABLE KEYS */;
INSERT INTO `security_groups` VALUES ('2017-03-07 14:47:46',NULL,NULL,1,'default','default','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284',0);
/*!40000 ALTER TABLE `security_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `services`
--

DROP TABLE IF EXISTS `services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `services` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) DEFAULT NULL,
  `binary` varchar(255) DEFAULT NULL,
  `topic` varchar(255) DEFAULT NULL,
  `report_count` int(11) NOT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `disabled_reason` varchar(255) DEFAULT NULL,
  `last_seen_up` datetime DEFAULT NULL,
  `forced_down` tinyint(1) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_services0host0topic0deleted` (`host`,`topic`,`deleted`),
  UNIQUE KEY `uniq_services0host0binary0deleted` (`host`,`binary`,`deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `services`
--

LOCK TABLES `services` WRITE;
/*!40000 ALTER TABLE `services` DISABLE KEYS */;
INSERT INTO `services` VALUES ('2017-03-01 12:28:33',NULL,NULL,1,'0.0.0.0','nova-osapi_compute',NULL,0,0,0,NULL,NULL,0,9),('2017-03-01 12:28:34',NULL,NULL,2,'0.0.0.0','nova-metadata',NULL,0,0,0,NULL,NULL,0,9),('2017-03-01 12:28:45','2017-04-18 14:16:53',NULL,3,'controller','nova-consoleauth','consoleauth',10865,0,0,NULL,'2017-04-18 14:16:53',0,9),('2017-03-01 12:28:55','2017-04-18 14:16:53',NULL,4,'controller','nova-scheduler','scheduler',10864,0,0,NULL,'2017-04-18 14:16:53',0,9),('2017-03-01 12:29:00','2017-04-18 14:16:53',NULL,5,'controller','nova-conductor','conductor',10863,0,0,NULL,'2017-04-18 14:16:53',0,9),('2017-03-01 13:19:46','2017-04-02 08:14:46',NULL,7,'compute1','nova-compute','compute',8988,1,0,'AUTO: Connection to libvirt lost: 0','2017-04-02 08:14:41',0,9);
/*!40000 ALTER TABLE `services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_agent_builds`
--

DROP TABLE IF EXISTS `shadow_agent_builds`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_agent_builds` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `hypervisor` varchar(255) DEFAULT NULL,
  `os` varchar(255) DEFAULT NULL,
  `architecture` varchar(255) DEFAULT NULL,
  `version` varchar(255) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `md5hash` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_agent_builds`
--

LOCK TABLES `shadow_agent_builds` WRITE;
/*!40000 ALTER TABLE `shadow_agent_builds` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_agent_builds` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_aggregate_hosts`
--

DROP TABLE IF EXISTS `shadow_aggregate_hosts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_aggregate_hosts` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) DEFAULT NULL,
  `aggregate_id` int(11) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_aggregate_hosts`
--

LOCK TABLES `shadow_aggregate_hosts` WRITE;
/*!40000 ALTER TABLE `shadow_aggregate_hosts` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_aggregate_hosts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_aggregate_metadata`
--

DROP TABLE IF EXISTS `shadow_aggregate_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_aggregate_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `aggregate_id` int(11) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_aggregate_metadata`
--

LOCK TABLES `shadow_aggregate_metadata` WRITE;
/*!40000 ALTER TABLE `shadow_aggregate_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_aggregate_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_aggregates`
--

DROP TABLE IF EXISTS `shadow_aggregates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_aggregates` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_aggregates`
--

LOCK TABLES `shadow_aggregates` WRITE;
/*!40000 ALTER TABLE `shadow_aggregates` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_aggregates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_block_device_mapping`
--

DROP TABLE IF EXISTS `shadow_block_device_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_block_device_mapping` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `device_name` varchar(255) DEFAULT NULL,
  `delete_on_termination` tinyint(1) DEFAULT NULL,
  `snapshot_id` varchar(36) DEFAULT NULL,
  `volume_id` varchar(36) DEFAULT NULL,
  `volume_size` int(11) DEFAULT NULL,
  `no_device` tinyint(1) DEFAULT NULL,
  `connection_info` mediumtext,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `source_type` varchar(255) DEFAULT NULL,
  `destination_type` varchar(255) DEFAULT NULL,
  `guest_format` varchar(255) DEFAULT NULL,
  `device_type` varchar(255) DEFAULT NULL,
  `disk_bus` varchar(255) DEFAULT NULL,
  `boot_index` int(11) DEFAULT NULL,
  `image_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_block_device_mapping`
--

LOCK TABLES `shadow_block_device_mapping` WRITE;
/*!40000 ALTER TABLE `shadow_block_device_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_block_device_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_bw_usage_cache`
--

DROP TABLE IF EXISTS `shadow_bw_usage_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_bw_usage_cache` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `start_period` datetime NOT NULL,
  `last_refreshed` datetime DEFAULT NULL,
  `bw_in` bigint(20) DEFAULT NULL,
  `bw_out` bigint(20) DEFAULT NULL,
  `mac` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `last_ctr_in` bigint(20) DEFAULT NULL,
  `last_ctr_out` bigint(20) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_bw_usage_cache`
--

LOCK TABLES `shadow_bw_usage_cache` WRITE;
/*!40000 ALTER TABLE `shadow_bw_usage_cache` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_bw_usage_cache` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_cells`
--

DROP TABLE IF EXISTS `shadow_cells`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_cells` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `api_url` varchar(255) DEFAULT NULL,
  `weight_offset` float DEFAULT NULL,
  `weight_scale` float DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `is_parent` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `transport_url` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_cells`
--

LOCK TABLES `shadow_cells` WRITE;
/*!40000 ALTER TABLE `shadow_cells` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_cells` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_certificates`
--

DROP TABLE IF EXISTS `shadow_certificates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_certificates` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_certificates`
--

LOCK TABLES `shadow_certificates` WRITE;
/*!40000 ALTER TABLE `shadow_certificates` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_certificates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_compute_nodes`
--

DROP TABLE IF EXISTS `shadow_compute_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_compute_nodes` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `service_id` int(11) DEFAULT NULL,
  `vcpus` int(11) NOT NULL,
  `memory_mb` int(11) NOT NULL,
  `local_gb` int(11) NOT NULL,
  `vcpus_used` int(11) NOT NULL,
  `memory_mb_used` int(11) NOT NULL,
  `local_gb_used` int(11) NOT NULL,
  `hypervisor_type` mediumtext NOT NULL,
  `hypervisor_version` int(11) NOT NULL,
  `cpu_info` mediumtext NOT NULL,
  `disk_available_least` int(11) DEFAULT NULL,
  `free_ram_mb` int(11) DEFAULT NULL,
  `free_disk_gb` int(11) DEFAULT NULL,
  `current_workload` int(11) DEFAULT NULL,
  `running_vms` int(11) DEFAULT NULL,
  `hypervisor_hostname` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `host_ip` varchar(39) DEFAULT NULL,
  `supported_instances` text,
  `pci_stats` text,
  `metrics` text,
  `extra_resources` text,
  `stats` text,
  `numa_topology` text,
  `host` varchar(255) DEFAULT NULL,
  `ram_allocation_ratio` float DEFAULT NULL,
  `cpu_allocation_ratio` float DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `disk_allocation_ratio` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_compute_nodes`
--

LOCK TABLES `shadow_compute_nodes` WRITE;
/*!40000 ALTER TABLE `shadow_compute_nodes` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_compute_nodes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_console_pools`
--

DROP TABLE IF EXISTS `shadow_console_pools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_console_pools` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `console_type` varchar(255) DEFAULT NULL,
  `public_hostname` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `compute_host` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_console_pools`
--

LOCK TABLES `shadow_console_pools` WRITE;
/*!40000 ALTER TABLE `shadow_console_pools` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_console_pools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_consoles`
--

DROP TABLE IF EXISTS `shadow_consoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_consoles` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `port` int(11) DEFAULT NULL,
  `pool_id` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_consoles`
--

LOCK TABLES `shadow_consoles` WRITE;
/*!40000 ALTER TABLE `shadow_consoles` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_consoles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_dns_domains`
--

DROP TABLE IF EXISTS `shadow_dns_domains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_dns_domains` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` tinyint(1) DEFAULT NULL,
  `domain` varchar(255) NOT NULL,
  `scope` varchar(255) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_dns_domains`
--

LOCK TABLES `shadow_dns_domains` WRITE;
/*!40000 ALTER TABLE `shadow_dns_domains` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_dns_domains` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_fixed_ips`
--

DROP TABLE IF EXISTS `shadow_fixed_ips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_fixed_ips` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `network_id` int(11) DEFAULT NULL,
  `allocated` tinyint(1) DEFAULT NULL,
  `leased` tinyint(1) DEFAULT NULL,
  `reserved` tinyint(1) DEFAULT NULL,
  `virtual_interface_id` int(11) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_fixed_ips`
--

LOCK TABLES `shadow_fixed_ips` WRITE;
/*!40000 ALTER TABLE `shadow_fixed_ips` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_fixed_ips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_floating_ips`
--

DROP TABLE IF EXISTS `shadow_floating_ips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_floating_ips` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(39) DEFAULT NULL,
  `fixed_ip_id` int(11) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `auto_assigned` tinyint(1) DEFAULT NULL,
  `pool` varchar(255) DEFAULT NULL,
  `interface` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_floating_ips`
--

LOCK TABLES `shadow_floating_ips` WRITE;
/*!40000 ALTER TABLE `shadow_floating_ips` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_floating_ips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_actions`
--

DROP TABLE IF EXISTS `shadow_instance_actions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_actions` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `action` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `request_id` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_actions`
--

LOCK TABLES `shadow_instance_actions` WRITE;
/*!40000 ALTER TABLE `shadow_instance_actions` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_actions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_actions_events`
--

DROP TABLE IF EXISTS `shadow_instance_actions_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_actions_events` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `event` varchar(255) DEFAULT NULL,
  `action_id` int(11) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `result` varchar(255) DEFAULT NULL,
  `traceback` text,
  `deleted` int(11) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `details` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_actions_events`
--

LOCK TABLES `shadow_instance_actions_events` WRITE;
/*!40000 ALTER TABLE `shadow_instance_actions_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_actions_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_extra`
--

DROP TABLE IF EXISTS `shadow_instance_extra`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_extra` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) NOT NULL,
  `numa_topology` text,
  `pci_requests` text,
  `flavor` text,
  `vcpu_model` text,
  `migration_context` text,
  PRIMARY KEY (`id`),
  KEY `shadow_instance_extra_idx` (`instance_uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_extra`
--

LOCK TABLES `shadow_instance_extra` WRITE;
/*!40000 ALTER TABLE `shadow_instance_extra` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_extra` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_faults`
--

DROP TABLE IF EXISTS `shadow_instance_faults`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_faults` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `code` int(11) NOT NULL,
  `message` varchar(255) DEFAULT NULL,
  `details` mediumtext,
  `host` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_faults`
--

LOCK TABLES `shadow_instance_faults` WRITE;
/*!40000 ALTER TABLE `shadow_instance_faults` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_faults` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_group_member`
--

DROP TABLE IF EXISTS `shadow_instance_group_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_group_member` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_id` varchar(255) DEFAULT NULL,
  `group_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_group_member`
--

LOCK TABLES `shadow_instance_group_member` WRITE;
/*!40000 ALTER TABLE `shadow_instance_group_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_group_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_group_policy`
--

DROP TABLE IF EXISTS `shadow_instance_group_policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_group_policy` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `policy` varchar(255) DEFAULT NULL,
  `group_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_group_policy`
--

LOCK TABLES `shadow_instance_group_policy` WRITE;
/*!40000 ALTER TABLE `shadow_instance_group_policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_group_policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_groups`
--

DROP TABLE IF EXISTS `shadow_instance_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_groups` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_groups`
--

LOCK TABLES `shadow_instance_groups` WRITE;
/*!40000 ALTER TABLE `shadow_instance_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_id_mappings`
--

DROP TABLE IF EXISTS `shadow_instance_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_id_mappings`
--

LOCK TABLES `shadow_instance_id_mappings` WRITE;
/*!40000 ALTER TABLE `shadow_instance_id_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_info_caches`
--

DROP TABLE IF EXISTS `shadow_instance_info_caches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_info_caches` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `network_info` mediumtext,
  `instance_uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_info_caches`
--

LOCK TABLES `shadow_instance_info_caches` WRITE;
/*!40000 ALTER TABLE `shadow_instance_info_caches` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_info_caches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_metadata`
--

DROP TABLE IF EXISTS `shadow_instance_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `key` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_metadata`
--

LOCK TABLES `shadow_instance_metadata` WRITE;
/*!40000 ALTER TABLE `shadow_instance_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_system_metadata`
--

DROP TABLE IF EXISTS `shadow_instance_system_metadata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_system_metadata` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_uuid` varchar(36) NOT NULL,
  `key` varchar(255) NOT NULL,
  `value` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_system_metadata`
--

LOCK TABLES `shadow_instance_system_metadata` WRITE;
/*!40000 ALTER TABLE `shadow_instance_system_metadata` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_system_metadata` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_type_extra_specs`
--

DROP TABLE IF EXISTS `shadow_instance_type_extra_specs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_type_extra_specs` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_type_id` int(11) NOT NULL,
  `key` varchar(255) DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_type_extra_specs`
--

LOCK TABLES `shadow_instance_type_extra_specs` WRITE;
/*!40000 ALTER TABLE `shadow_instance_type_extra_specs` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_type_extra_specs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_type_projects`
--

DROP TABLE IF EXISTS `shadow_instance_type_projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_type_projects` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `instance_type_id` int(11) NOT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_type_projects`
--

LOCK TABLES `shadow_instance_type_projects` WRITE;
/*!40000 ALTER TABLE `shadow_instance_type_projects` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_type_projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instance_types`
--

DROP TABLE IF EXISTS `shadow_instance_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instance_types` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `memory_mb` int(11) NOT NULL,
  `vcpus` int(11) NOT NULL,
  `swap` int(11) NOT NULL,
  `vcpu_weight` int(11) DEFAULT NULL,
  `flavorid` varchar(255) DEFAULT NULL,
  `rxtx_factor` float DEFAULT NULL,
  `root_gb` int(11) DEFAULT NULL,
  `ephemeral_gb` int(11) DEFAULT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `is_public` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instance_types`
--

LOCK TABLES `shadow_instance_types` WRITE;
/*!40000 ALTER TABLE `shadow_instance_types` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instance_types` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_instances`
--

DROP TABLE IF EXISTS `shadow_instances`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_instances` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `internal_id` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `image_ref` varchar(255) DEFAULT NULL,
  `kernel_id` varchar(255) DEFAULT NULL,
  `ramdisk_id` varchar(255) DEFAULT NULL,
  `launch_index` int(11) DEFAULT NULL,
  `key_name` varchar(255) DEFAULT NULL,
  `key_data` mediumtext,
  `power_state` int(11) DEFAULT NULL,
  `vm_state` varchar(255) DEFAULT NULL,
  `memory_mb` int(11) DEFAULT NULL,
  `vcpus` int(11) DEFAULT NULL,
  `hostname` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `user_data` mediumtext,
  `reservation_id` varchar(255) DEFAULT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `launched_at` datetime DEFAULT NULL,
  `terminated_at` datetime DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `display_description` varchar(255) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  `locked` tinyint(1) DEFAULT NULL,
  `os_type` varchar(255) DEFAULT NULL,
  `launched_on` mediumtext,
  `instance_type_id` int(11) DEFAULT NULL,
  `vm_mode` varchar(255) DEFAULT NULL,
  `uuid` varchar(36) NOT NULL,
  `architecture` varchar(255) DEFAULT NULL,
  `root_device_name` varchar(255) DEFAULT NULL,
  `access_ip_v4` varchar(39) DEFAULT NULL,
  `access_ip_v6` varchar(39) DEFAULT NULL,
  `config_drive` varchar(255) DEFAULT NULL,
  `task_state` varchar(255) DEFAULT NULL,
  `default_ephemeral_device` varchar(255) DEFAULT NULL,
  `default_swap_device` varchar(255) DEFAULT NULL,
  `progress` int(11) DEFAULT NULL,
  `auto_disk_config` tinyint(1) DEFAULT NULL,
  `shutdown_terminate` tinyint(1) DEFAULT NULL,
  `disable_terminate` tinyint(1) DEFAULT NULL,
  `root_gb` int(11) DEFAULT NULL,
  `ephemeral_gb` int(11) DEFAULT NULL,
  `cell_name` varchar(255) DEFAULT NULL,
  `node` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `locked_by` enum('owner','admin') DEFAULT NULL,
  `cleaned` int(11) DEFAULT NULL,
  `ephemeral_key_uuid` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_instances`
--

LOCK TABLES `shadow_instances` WRITE;
/*!40000 ALTER TABLE `shadow_instances` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_instances` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_key_pairs`
--

DROP TABLE IF EXISTS `shadow_key_pairs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_key_pairs` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `fingerprint` varchar(255) DEFAULT NULL,
  `public_key` mediumtext,
  `deleted` int(11) DEFAULT NULL,
  `type` enum('ssh','x509') NOT NULL DEFAULT 'ssh',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_key_pairs`
--

LOCK TABLES `shadow_key_pairs` WRITE;
/*!40000 ALTER TABLE `shadow_key_pairs` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_key_pairs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_migrate_version`
--

DROP TABLE IF EXISTS `shadow_migrate_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_migrate_version` (
  `repository_id` varchar(250) NOT NULL,
  `repository_path` text,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`repository_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_migrate_version`
--

LOCK TABLES `shadow_migrate_version` WRITE;
/*!40000 ALTER TABLE `shadow_migrate_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_migrate_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_migrations`
--

DROP TABLE IF EXISTS `shadow_migrations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_migrations` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source_compute` varchar(255) DEFAULT NULL,
  `dest_compute` varchar(255) DEFAULT NULL,
  `dest_host` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `old_instance_type_id` int(11) DEFAULT NULL,
  `new_instance_type_id` int(11) DEFAULT NULL,
  `source_node` varchar(255) DEFAULT NULL,
  `dest_node` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `migration_type` enum('migration','resize','live-migration','evacuation') DEFAULT NULL,
  `hidden` tinyint(1) DEFAULT NULL,
  `memory_total` bigint(20) DEFAULT NULL,
  `memory_processed` bigint(20) DEFAULT NULL,
  `memory_remaining` bigint(20) DEFAULT NULL,
  `disk_total` bigint(20) DEFAULT NULL,
  `disk_processed` bigint(20) DEFAULT NULL,
  `disk_remaining` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_migrations`
--

LOCK TABLES `shadow_migrations` WRITE;
/*!40000 ALTER TABLE `shadow_migrations` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_migrations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_networks`
--

DROP TABLE IF EXISTS `shadow_networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_networks` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `injected` tinyint(1) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `netmask` varchar(39) DEFAULT NULL,
  `bridge` varchar(255) DEFAULT NULL,
  `gateway` varchar(39) DEFAULT NULL,
  `broadcast` varchar(39) DEFAULT NULL,
  `dns1` varchar(39) DEFAULT NULL,
  `vlan` int(11) DEFAULT NULL,
  `vpn_public_address` varchar(39) DEFAULT NULL,
  `vpn_public_port` int(11) DEFAULT NULL,
  `vpn_private_address` varchar(39) DEFAULT NULL,
  `dhcp_start` varchar(39) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `host` varchar(255) DEFAULT NULL,
  `cidr_v6` varchar(43) DEFAULT NULL,
  `gateway_v6` varchar(39) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `netmask_v6` varchar(39) DEFAULT NULL,
  `bridge_interface` varchar(255) DEFAULT NULL,
  `multi_host` tinyint(1) DEFAULT NULL,
  `dns2` varchar(39) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `rxtx_base` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `mtu` int(11) DEFAULT NULL,
  `dhcp_server` varchar(39) DEFAULT NULL,
  `enable_dhcp` tinyint(1) DEFAULT NULL,
  `share_address` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_networks`
--

LOCK TABLES `shadow_networks` WRITE;
/*!40000 ALTER TABLE `shadow_networks` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_pci_devices`
--

DROP TABLE IF EXISTS `shadow_pci_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_pci_devices` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `compute_node_id` int(11) NOT NULL,
  `address` varchar(12) NOT NULL,
  `product_id` varchar(4) DEFAULT NULL,
  `vendor_id` varchar(4) DEFAULT NULL,
  `dev_type` varchar(8) DEFAULT NULL,
  `dev_id` varchar(255) DEFAULT NULL,
  `label` varchar(255) NOT NULL,
  `status` varchar(36) NOT NULL,
  `extra_info` text,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `request_id` varchar(36) DEFAULT NULL,
  `numa_node` int(11) DEFAULT NULL,
  `parent_addr` varchar(12) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_pci_devices`
--

LOCK TABLES `shadow_pci_devices` WRITE;
/*!40000 ALTER TABLE `shadow_pci_devices` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_pci_devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_project_user_quotas`
--

DROP TABLE IF EXISTS `shadow_project_user_quotas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_project_user_quotas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) NOT NULL,
  `project_id` varchar(255) NOT NULL,
  `resource` varchar(255) NOT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_project_user_quotas`
--

LOCK TABLES `shadow_project_user_quotas` WRITE;
/*!40000 ALTER TABLE `shadow_project_user_quotas` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_project_user_quotas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_provider_fw_rules`
--

DROP TABLE IF EXISTS `shadow_provider_fw_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_provider_fw_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protocol` varchar(5) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_provider_fw_rules`
--

LOCK TABLES `shadow_provider_fw_rules` WRITE;
/*!40000 ALTER TABLE `shadow_provider_fw_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_provider_fw_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_quota_classes`
--

DROP TABLE IF EXISTS `shadow_quota_classes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_quota_classes` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_quota_classes`
--

LOCK TABLES `shadow_quota_classes` WRITE;
/*!40000 ALTER TABLE `shadow_quota_classes` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_quota_classes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_quota_usages`
--

DROP TABLE IF EXISTS `shadow_quota_usages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_quota_usages` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `in_use` int(11) NOT NULL,
  `reserved` int(11) NOT NULL,
  `until_refresh` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_quota_usages`
--

LOCK TABLES `shadow_quota_usages` WRITE;
/*!40000 ALTER TABLE `shadow_quota_usages` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_quota_usages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_quotas`
--

DROP TABLE IF EXISTS `shadow_quotas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_quotas` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) NOT NULL,
  `hard_limit` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_quotas`
--

LOCK TABLES `shadow_quotas` WRITE;
/*!40000 ALTER TABLE `shadow_quotas` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_quotas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_reservations`
--

DROP TABLE IF EXISTS `shadow_reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_reservations` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `usage_id` int(11) NOT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `delta` int(11) NOT NULL,
  `expire` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_reservations`
--

LOCK TABLES `shadow_reservations` WRITE;
/*!40000 ALTER TABLE `shadow_reservations` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_s3_images`
--

DROP TABLE IF EXISTS `shadow_s3_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_s3_images` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_s3_images`
--

LOCK TABLES `shadow_s3_images` WRITE;
/*!40000 ALTER TABLE `shadow_s3_images` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_s3_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_security_group_default_rules`
--

DROP TABLE IF EXISTS `shadow_security_group_default_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_security_group_default_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `protocol` varchar(5) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_security_group_default_rules`
--

LOCK TABLES `shadow_security_group_default_rules` WRITE;
/*!40000 ALTER TABLE `shadow_security_group_default_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_security_group_default_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_security_group_instance_association`
--

DROP TABLE IF EXISTS `shadow_security_group_instance_association`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_security_group_instance_association` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `security_group_id` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_security_group_instance_association`
--

LOCK TABLES `shadow_security_group_instance_association` WRITE;
/*!40000 ALTER TABLE `shadow_security_group_instance_association` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_security_group_instance_association` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_security_group_rules`
--

DROP TABLE IF EXISTS `shadow_security_group_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_security_group_rules` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `parent_group_id` int(11) DEFAULT NULL,
  `protocol` varchar(255) DEFAULT NULL,
  `from_port` int(11) DEFAULT NULL,
  `to_port` int(11) DEFAULT NULL,
  `cidr` varchar(43) DEFAULT NULL,
  `group_id` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_security_group_rules`
--

LOCK TABLES `shadow_security_group_rules` WRITE;
/*!40000 ALTER TABLE `shadow_security_group_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_security_group_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_security_groups`
--

DROP TABLE IF EXISTS `shadow_security_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_security_groups` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_security_groups`
--

LOCK TABLES `shadow_security_groups` WRITE;
/*!40000 ALTER TABLE `shadow_security_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_security_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_services`
--

DROP TABLE IF EXISTS `shadow_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_services` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `host` varchar(255) DEFAULT NULL,
  `binary` varchar(255) DEFAULT NULL,
  `topic` varchar(255) DEFAULT NULL,
  `report_count` int(11) NOT NULL,
  `disabled` tinyint(1) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `disabled_reason` varchar(255) DEFAULT NULL,
  `last_seen_up` datetime DEFAULT NULL,
  `forced_down` tinyint(1) DEFAULT NULL,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_services`
--

LOCK TABLES `shadow_services` WRITE;
/*!40000 ALTER TABLE `shadow_services` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_snapshot_id_mappings`
--

DROP TABLE IF EXISTS `shadow_snapshot_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_snapshot_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_snapshot_id_mappings`
--

LOCK TABLES `shadow_snapshot_id_mappings` WRITE;
/*!40000 ALTER TABLE `shadow_snapshot_id_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_snapshot_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_snapshots`
--

DROP TABLE IF EXISTS `shadow_snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_snapshots` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `volume_id` varchar(36) NOT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `progress` varchar(255) DEFAULT NULL,
  `volume_size` int(11) DEFAULT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `display_description` varchar(255) DEFAULT NULL,
  `deleted` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_snapshots`
--

LOCK TABLES `shadow_snapshots` WRITE;
/*!40000 ALTER TABLE `shadow_snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_task_log`
--

DROP TABLE IF EXISTS `shadow_task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_task_log` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_name` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL,
  `period_beginning` datetime NOT NULL,
  `period_ending` datetime NOT NULL,
  `message` varchar(255) NOT NULL,
  `task_items` int(11) DEFAULT NULL,
  `errors` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_task_log`
--

LOCK TABLES `shadow_task_log` WRITE;
/*!40000 ALTER TABLE `shadow_task_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_task_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_virtual_interfaces`
--

DROP TABLE IF EXISTS `shadow_virtual_interfaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_virtual_interfaces` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `network_id` int(11) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_virtual_interfaces`
--

LOCK TABLES `shadow_virtual_interfaces` WRITE;
/*!40000 ALTER TABLE `shadow_virtual_interfaces` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_virtual_interfaces` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_volume_id_mappings`
--

DROP TABLE IF EXISTS `shadow_volume_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_volume_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_volume_id_mappings`
--

LOCK TABLES `shadow_volume_id_mappings` WRITE;
/*!40000 ALTER TABLE `shadow_volume_id_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_volume_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `shadow_volume_usage_cache`
--

DROP TABLE IF EXISTS `shadow_volume_usage_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `shadow_volume_usage_cache` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `volume_id` varchar(36) NOT NULL,
  `tot_last_refreshed` datetime DEFAULT NULL,
  `tot_reads` bigint(20) DEFAULT NULL,
  `tot_read_bytes` bigint(20) DEFAULT NULL,
  `tot_writes` bigint(20) DEFAULT NULL,
  `tot_write_bytes` bigint(20) DEFAULT NULL,
  `curr_last_refreshed` datetime DEFAULT NULL,
  `curr_reads` bigint(20) DEFAULT NULL,
  `curr_read_bytes` bigint(20) DEFAULT NULL,
  `curr_writes` bigint(20) DEFAULT NULL,
  `curr_write_bytes` bigint(20) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `project_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(36) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `shadow_volume_usage_cache`
--

LOCK TABLES `shadow_volume_usage_cache` WRITE;
/*!40000 ALTER TABLE `shadow_volume_usage_cache` DISABLE KEYS */;
/*!40000 ALTER TABLE `shadow_volume_usage_cache` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshot_id_mappings`
--

DROP TABLE IF EXISTS `snapshot_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshot_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshot_id_mappings`
--

LOCK TABLES `snapshot_id_mappings` WRITE;
/*!40000 ALTER TABLE `snapshot_id_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `snapshot_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `snapshots`
--

DROP TABLE IF EXISTS `snapshots`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `snapshots` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `volume_id` varchar(36) NOT NULL,
  `user_id` varchar(255) DEFAULT NULL,
  `project_id` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `progress` varchar(255) DEFAULT NULL,
  `volume_size` int(11) DEFAULT NULL,
  `scheduled_at` datetime DEFAULT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  `display_description` varchar(255) DEFAULT NULL,
  `deleted` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `snapshots`
--

LOCK TABLES `snapshots` WRITE;
/*!40000 ALTER TABLE `snapshots` DISABLE KEYS */;
/*!40000 ALTER TABLE `snapshots` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags` (
  `resource_id` varchar(36) NOT NULL,
  `tag` varchar(80) NOT NULL,
  PRIMARY KEY (`resource_id`,`tag`),
  KEY `tags_tag_idx` (`tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags`
--

LOCK TABLES `tags` WRITE;
/*!40000 ALTER TABLE `tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `task_log`
--

DROP TABLE IF EXISTS `task_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `task_log` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task_name` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL,
  `period_beginning` datetime NOT NULL,
  `period_ending` datetime NOT NULL,
  `message` varchar(255) NOT NULL,
  `task_items` int(11) DEFAULT NULL,
  `errors` int(11) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_task_log0task_name0host0period_beginning0period_ending` (`task_name`,`host`,`period_beginning`,`period_ending`),
  KEY `ix_task_log_period_beginning` (`period_beginning`),
  KEY `ix_task_log_host` (`host`),
  KEY `ix_task_log_period_ending` (`period_ending`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_log`
--

LOCK TABLES `task_log` WRITE;
/*!40000 ALTER TABLE `task_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `virtual_interfaces`
--

DROP TABLE IF EXISTS `virtual_interfaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `virtual_interfaces` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) DEFAULT NULL,
  `network_id` int(11) DEFAULT NULL,
  `uuid` varchar(36) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_virtual_interfaces0address0deleted` (`address`,`deleted`),
  KEY `virtual_interfaces_instance_uuid_fkey` (`instance_uuid`),
  KEY `virtual_interfaces_network_id_idx` (`network_id`),
  KEY `virtual_interfaces_uuid_idx` (`uuid`),
  CONSTRAINT `virtual_interfaces_instance_uuid_fkey` FOREIGN KEY (`instance_uuid`) REFERENCES `instances` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `virtual_interfaces`
--

LOCK TABLES `virtual_interfaces` WRITE;
/*!40000 ALTER TABLE `virtual_interfaces` DISABLE KEYS */;
/*!40000 ALTER TABLE `virtual_interfaces` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volume_id_mappings`
--

DROP TABLE IF EXISTS `volume_id_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume_id_mappings` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(36) NOT NULL,
  `deleted` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volume_id_mappings`
--

LOCK TABLES `volume_id_mappings` WRITE;
/*!40000 ALTER TABLE `volume_id_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `volume_id_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `volume_usage_cache`
--

DROP TABLE IF EXISTS `volume_usage_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `volume_usage_cache` (
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `volume_id` varchar(36) NOT NULL,
  `tot_last_refreshed` datetime DEFAULT NULL,
  `tot_reads` bigint(20) DEFAULT NULL,
  `tot_read_bytes` bigint(20) DEFAULT NULL,
  `tot_writes` bigint(20) DEFAULT NULL,
  `tot_write_bytes` bigint(20) DEFAULT NULL,
  `curr_last_refreshed` datetime DEFAULT NULL,
  `curr_reads` bigint(20) DEFAULT NULL,
  `curr_read_bytes` bigint(20) DEFAULT NULL,
  `curr_writes` bigint(20) DEFAULT NULL,
  `curr_write_bytes` bigint(20) DEFAULT NULL,
  `deleted` int(11) DEFAULT NULL,
  `instance_uuid` varchar(36) DEFAULT NULL,
  `project_id` varchar(36) DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `availability_zone` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `volume_usage_cache`
--

LOCK TABLES `volume_usage_cache` WRITE;
/*!40000 ALTER TABLE `volume_usage_cache` DISABLE KEYS */;
/*!40000 ALTER TABLE `volume_usage_cache` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Current Database: `neutron`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `neutron` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `neutron`;

--
-- Table structure for table `address_scopes`
--

DROP TABLE IF EXISTS `address_scopes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `address_scopes` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `shared` tinyint(1) NOT NULL,
  `ip_version` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_address_scopes_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `address_scopes`
--

LOCK TABLES `address_scopes` WRITE;
/*!40000 ALTER TABLE `address_scopes` DISABLE KEYS */;
/*!40000 ALTER TABLE `address_scopes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `agents`
--

DROP TABLE IF EXISTS `agents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `agents` (
  `id` varchar(36) NOT NULL,
  `agent_type` varchar(255) NOT NULL,
  `binary` varchar(255) NOT NULL,
  `topic` varchar(255) NOT NULL,
  `host` varchar(255) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL,
  `started_at` datetime NOT NULL,
  `heartbeat_timestamp` datetime NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `configurations` varchar(4095) NOT NULL,
  `load` int(11) NOT NULL DEFAULT '0',
  `availability_zone` varchar(255) DEFAULT NULL,
  `resource_versions` varchar(8191) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_agents0agent_type0host` (`agent_type`,`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `agents`
--

LOCK TABLES `agents` WRITE;
/*!40000 ALTER TABLE `agents` DISABLE KEYS */;
INSERT INTO `agents` VALUES ('309b2677-e35f-4ca6-b062-60dcfa3c3aba','Open vSwitch agent','neutron-openvswitch-agent','N/A','controller',1,'2017-03-01 15:59:02','2017-04-18 14:14:27','2017-04-18 14:16:57',NULL,'{\"ovs_hybrid_plug\": true, \"in_distributed_mode\": false, \"datapath_type\": \"system\", \"arp_responder_enabled\": false, \"tunneling_ip\": \"172.16.0.11\", \"vhostuser_socket_dir\": \"/var/run/openvswitch\", \"devices\": 7, \"ovs_capabilities\": {\"datapath_types\": [\"netdev\", \"system\"], \"iface_types\": [\"geneve\", \"gre\", \"internal\", \"ipsec_gre\", \"lisp\", \"patch\", \"stt\", \"system\", \"tap\", \"vxlan\"]}, \"log_agent_heartbeats\": false, \"l2_population\": true, \"tunnel_types\": [\"vxlan\"], \"extensions\": [], \"enable_distributed_routing\": false, \"bridge_mappings\": {\"provider\": \"br-provider\"}}',0,NULL,'{\"QosPolicy\": \"1.0\"}'),('a01c0dc3-8be6-4722-87f5-e2d77edaf33f','L3 agent','neutron-l3-agent','l3_agent','controller',1,'2017-03-01 15:59:41','2017-04-18 14:13:33','2017-04-18 14:16:33',NULL,'{\"router_id\": \"\", \"agent_mode\": \"legacy\", \"gateway_external_network_id\": \"\", \"handle_internal_only_routers\": true, \"routers\": 2, \"interfaces\": 2, \"floating_ips\": 1, \"interface_driver\": \"openvswitch\", \"log_agent_heartbeats\": false, \"external_network_bridge\": \"\", \"ex_gw_ports\": 2}',0,'nova',NULL),('e0e6ed38-5306-4f68-8696-51f68c419465','DHCP agent','neutron-dhcp-agent','dhcp_agent','controller',1,'2017-03-01 15:59:34','2017-04-18 14:14:32','2017-04-18 14:16:32',NULL,'{\"subnets\": 3, \"dhcp_lease_duration\": 86400, \"dhcp_driver\": \"neutron.agent.linux.dhcp.Dnsmasq\", \"networks\": 3, \"log_agent_heartbeats\": false, \"ports\": 9}',3,'nova',NULL),('ee09ac8a-e9c7-4ee7-b06d-ca0db06b5023','Open vSwitch agent','neutron-openvswitch-agent','N/A','compute1',1,'2017-03-01 16:04:49','2017-04-01 07:51:44','2017-04-02 08:14:21',NULL,'{\"ovs_hybrid_plug\": false, \"in_distributed_mode\": false, \"datapath_type\": \"system\", \"arp_responder_enabled\": false, \"tunneling_ip\": \"172.16.0.31\", \"vhostuser_socket_dir\": \"/var/run/openvswitch\", \"devices\": 0, \"ovs_capabilities\": {\"datapath_types\": [\"netdev\", \"system\"], \"iface_types\": [\"geneve\", \"gre\", \"internal\", \"ipsec_gre\", \"lisp\", \"patch\", \"stt\", \"system\", \"tap\", \"vxlan\"]}, \"log_agent_heartbeats\": false, \"l2_population\": true, \"tunnel_types\": [\"vxlan\"], \"extensions\": [], \"enable_distributed_routing\": false, \"bridge_mappings\": {\"provider\": \"br-provider\"}}',0,NULL,'{\"QosPolicy\": \"1.0\"}'),('fbadcf52-b9b9-4813-a504-e3cea1a834df','Metadata agent','neutron-metadata-agent','N/A','controller',1,'2017-03-01 15:59:37','2017-04-18 14:14:32','2017-04-18 14:16:32',NULL,'{\"log_agent_heartbeats\": false, \"nova_metadata_ip\": \"controller\", \"nova_metadata_port\": 8775, \"metadata_proxy_socket\": \"/var/lib/neutron/metadata_proxy\"}',0,NULL,NULL);
/*!40000 ALTER TABLE `agents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alembic_version`
--

DROP TABLE IF EXISTS `alembic_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alembic_version` (
  `version_num` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alembic_version`
--

LOCK TABLES `alembic_version` WRITE;
/*!40000 ALTER TABLE `alembic_version` DISABLE KEYS */;
INSERT INTO `alembic_version` VALUES ('4ffceebfcdc');
/*!40000 ALTER TABLE `alembic_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `alembic_version_fwaas`
--

DROP TABLE IF EXISTS `alembic_version_fwaas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `alembic_version_fwaas` (
  `version_num` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `alembic_version_fwaas`
--

LOCK TABLES `alembic_version_fwaas` WRITE;
/*!40000 ALTER TABLE `alembic_version_fwaas` DISABLE KEYS */;
INSERT INTO `alembic_version_fwaas` VALUES ('458aa42b14b'),('4b47ea298795');
/*!40000 ALTER TABLE `alembic_version_fwaas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `allowedaddresspairs`
--

DROP TABLE IF EXISTS `allowedaddresspairs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `allowedaddresspairs` (
  `port_id` varchar(36) NOT NULL,
  `mac_address` varchar(32) NOT NULL,
  `ip_address` varchar(64) NOT NULL,
  PRIMARY KEY (`port_id`,`mac_address`,`ip_address`),
  CONSTRAINT `allowedaddresspairs_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `allowedaddresspairs`
--

LOCK TABLES `allowedaddresspairs` WRITE;
/*!40000 ALTER TABLE `allowedaddresspairs` DISABLE KEYS */;
/*!40000 ALTER TABLE `allowedaddresspairs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `arista_provisioned_nets`
--

DROP TABLE IF EXISTS `arista_provisioned_nets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `arista_provisioned_nets` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  `segmentation_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_arista_provisioned_nets_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arista_provisioned_nets`
--

LOCK TABLES `arista_provisioned_nets` WRITE;
/*!40000 ALTER TABLE `arista_provisioned_nets` DISABLE KEYS */;
/*!40000 ALTER TABLE `arista_provisioned_nets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `arista_provisioned_tenants`
--

DROP TABLE IF EXISTS `arista_provisioned_tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `arista_provisioned_tenants` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_arista_provisioned_tenants_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arista_provisioned_tenants`
--

LOCK TABLES `arista_provisioned_tenants` WRITE;
/*!40000 ALTER TABLE `arista_provisioned_tenants` DISABLE KEYS */;
/*!40000 ALTER TABLE `arista_provisioned_tenants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `arista_provisioned_vms`
--

DROP TABLE IF EXISTS `arista_provisioned_vms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `arista_provisioned_vms` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `vm_id` varchar(255) DEFAULT NULL,
  `host_id` varchar(255) DEFAULT NULL,
  `port_id` varchar(36) DEFAULT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_arista_provisioned_vms_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arista_provisioned_vms`
--

LOCK TABLES `arista_provisioned_vms` WRITE;
/*!40000 ALTER TABLE `arista_provisioned_vms` DISABLE KEYS */;
/*!40000 ALTER TABLE `arista_provisioned_vms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auto_allocated_topologies`
--

DROP TABLE IF EXISTS `auto_allocated_topologies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `auto_allocated_topologies` (
  `tenant_id` varchar(255) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`tenant_id`),
  KEY `network_id` (`network_id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `auto_allocated_topologies_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `auto_allocated_topologies_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auto_allocated_topologies`
--

LOCK TABLES `auto_allocated_topologies` WRITE;
/*!40000 ALTER TABLE `auto_allocated_topologies` DISABLE KEYS */;
/*!40000 ALTER TABLE `auto_allocated_topologies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bgp_peers`
--

DROP TABLE IF EXISTS `bgp_peers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bgp_peers` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `auth_type` varchar(16) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `peer_ip` varchar(64) NOT NULL,
  `remote_as` int(11) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_bgp_peers_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bgp_peers`
--

LOCK TABLES `bgp_peers` WRITE;
/*!40000 ALTER TABLE `bgp_peers` DISABLE KEYS */;
/*!40000 ALTER TABLE `bgp_peers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bgp_speaker_dragent_bindings`
--

DROP TABLE IF EXISTS `bgp_speaker_dragent_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bgp_speaker_dragent_bindings` (
  `agent_id` varchar(36) NOT NULL,
  `bgp_speaker_id` varchar(36) NOT NULL,
  PRIMARY KEY (`agent_id`),
  KEY `bgp_speaker_id` (`bgp_speaker_id`),
  CONSTRAINT `bgp_speaker_dragent_bindings_ibfk_1` FOREIGN KEY (`agent_id`) REFERENCES `agents` (`id`) ON DELETE CASCADE,
  CONSTRAINT `bgp_speaker_dragent_bindings_ibfk_2` FOREIGN KEY (`bgp_speaker_id`) REFERENCES `bgp_speakers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bgp_speaker_dragent_bindings`
--

LOCK TABLES `bgp_speaker_dragent_bindings` WRITE;
/*!40000 ALTER TABLE `bgp_speaker_dragent_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `bgp_speaker_dragent_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bgp_speaker_network_bindings`
--

DROP TABLE IF EXISTS `bgp_speaker_network_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bgp_speaker_network_bindings` (
  `bgp_speaker_id` varchar(36) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  `ip_version` int(11) NOT NULL,
  PRIMARY KEY (`network_id`,`bgp_speaker_id`,`ip_version`),
  KEY `bgp_speaker_id` (`bgp_speaker_id`),
  CONSTRAINT `bgp_speaker_network_bindings_ibfk_1` FOREIGN KEY (`bgp_speaker_id`) REFERENCES `bgp_speakers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `bgp_speaker_network_bindings_ibfk_2` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bgp_speaker_network_bindings`
--

LOCK TABLES `bgp_speaker_network_bindings` WRITE;
/*!40000 ALTER TABLE `bgp_speaker_network_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `bgp_speaker_network_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bgp_speaker_peer_bindings`
--

DROP TABLE IF EXISTS `bgp_speaker_peer_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bgp_speaker_peer_bindings` (
  `bgp_speaker_id` varchar(36) NOT NULL,
  `bgp_peer_id` varchar(36) NOT NULL,
  PRIMARY KEY (`bgp_speaker_id`,`bgp_peer_id`),
  KEY `bgp_peer_id` (`bgp_peer_id`),
  CONSTRAINT `bgp_speaker_peer_bindings_ibfk_1` FOREIGN KEY (`bgp_speaker_id`) REFERENCES `bgp_speakers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `bgp_speaker_peer_bindings_ibfk_2` FOREIGN KEY (`bgp_peer_id`) REFERENCES `bgp_peers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bgp_speaker_peer_bindings`
--

LOCK TABLES `bgp_speaker_peer_bindings` WRITE;
/*!40000 ALTER TABLE `bgp_speaker_peer_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `bgp_speaker_peer_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `bgp_speakers`
--

DROP TABLE IF EXISTS `bgp_speakers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bgp_speakers` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `local_as` int(11) NOT NULL,
  `ip_version` int(11) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `advertise_floating_ip_host_routes` tinyint(1) NOT NULL,
  `advertise_tenant_networks` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_bgp_speakers_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bgp_speakers`
--

LOCK TABLES `bgp_speakers` WRITE;
/*!40000 ALTER TABLE `bgp_speakers` DISABLE KEYS */;
/*!40000 ALTER TABLE `bgp_speakers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `brocadenetworks`
--

DROP TABLE IF EXISTS `brocadenetworks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `brocadenetworks` (
  `id` varchar(36) NOT NULL,
  `vlan` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `brocadenetworks`
--

LOCK TABLES `brocadenetworks` WRITE;
/*!40000 ALTER TABLE `brocadenetworks` DISABLE KEYS */;
/*!40000 ALTER TABLE `brocadenetworks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `brocadeports`
--

DROP TABLE IF EXISTS `brocadeports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `brocadeports` (
  `port_id` varchar(36) NOT NULL DEFAULT '',
  `network_id` varchar(36) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `physical_interface` varchar(36) DEFAULT NULL,
  `vlan_id` varchar(36) DEFAULT NULL,
  `tenant_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`port_id`),
  KEY `network_id` (`network_id`),
  CONSTRAINT `brocadeports_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `brocadenetworks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `brocadeports`
--

LOCK TABLES `brocadeports` WRITE;
/*!40000 ALTER TABLE `brocadeports` DISABLE KEYS */;
/*!40000 ALTER TABLE `brocadeports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_csr_identifier_map`
--

DROP TABLE IF EXISTS `cisco_csr_identifier_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_csr_identifier_map` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `ipsec_site_conn_id` varchar(36) NOT NULL,
  `csr_tunnel_id` int(11) NOT NULL,
  `csr_ike_policy_id` int(11) NOT NULL,
  `csr_ipsec_policy_id` int(11) NOT NULL,
  PRIMARY KEY (`ipsec_site_conn_id`),
  CONSTRAINT `cisco_csr_identifier_map_ibfk_1` FOREIGN KEY (`ipsec_site_conn_id`) REFERENCES `ipsec_site_connections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_csr_identifier_map`
--

LOCK TABLES `cisco_csr_identifier_map` WRITE;
/*!40000 ALTER TABLE `cisco_csr_identifier_map` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_csr_identifier_map` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_firewall_associations`
--

DROP TABLE IF EXISTS `cisco_firewall_associations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_firewall_associations` (
  `fw_id` varchar(36) NOT NULL,
  `port_id` varchar(36) DEFAULT NULL,
  `direction` varchar(16) DEFAULT NULL,
  `acl_id` varchar(36) DEFAULT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`fw_id`),
  KEY `port_id` (`port_id`),
  CONSTRAINT `cisco_firewall_associations_ibfk_1` FOREIGN KEY (`fw_id`) REFERENCES `firewalls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cisco_firewall_associations_ibfk_2` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_firewall_associations`
--

LOCK TABLES `cisco_firewall_associations` WRITE;
/*!40000 ALTER TABLE `cisco_firewall_associations` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_firewall_associations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_hosting_devices`
--

DROP TABLE IF EXISTS `cisco_hosting_devices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_hosting_devices` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `complementary_id` varchar(36) DEFAULT NULL,
  `device_id` varchar(255) DEFAULT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `management_port_id` varchar(36) DEFAULT NULL,
  `protocol_port` int(11) DEFAULT NULL,
  `cfg_agent_id` varchar(36) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `status` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cfg_agent_id` (`cfg_agent_id`),
  KEY `management_port_id` (`management_port_id`),
  KEY `ix_cisco_hosting_devices_tenant_id` (`tenant_id`),
  CONSTRAINT `cisco_hosting_devices_ibfk_1` FOREIGN KEY (`cfg_agent_id`) REFERENCES `agents` (`id`),
  CONSTRAINT `cisco_hosting_devices_ibfk_2` FOREIGN KEY (`management_port_id`) REFERENCES `ports` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_hosting_devices`
--

LOCK TABLES `cisco_hosting_devices` WRITE;
/*!40000 ALTER TABLE `cisco_hosting_devices` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_hosting_devices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_apic_contracts`
--

DROP TABLE IF EXISTS `cisco_ml2_apic_contracts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_apic_contracts` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `router_id` varchar(36) NOT NULL,
  PRIMARY KEY (`router_id`),
  KEY `ix_cisco_ml2_apic_contracts_tenant_id` (`tenant_id`),
  CONSTRAINT `cisco_ml2_apic_contracts_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_apic_contracts`
--

LOCK TABLES `cisco_ml2_apic_contracts` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_apic_contracts` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_apic_contracts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_apic_host_links`
--

DROP TABLE IF EXISTS `cisco_ml2_apic_host_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_apic_host_links` (
  `host` varchar(255) NOT NULL,
  `ifname` varchar(64) NOT NULL,
  `ifmac` varchar(32) DEFAULT NULL,
  `swid` varchar(32) NOT NULL,
  `module` varchar(32) NOT NULL,
  `port` varchar(32) NOT NULL,
  PRIMARY KEY (`host`,`ifname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_apic_host_links`
--

LOCK TABLES `cisco_ml2_apic_host_links` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_apic_host_links` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_apic_host_links` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_apic_names`
--

DROP TABLE IF EXISTS `cisco_ml2_apic_names`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_apic_names` (
  `neutron_id` varchar(36) NOT NULL,
  `neutron_type` varchar(32) NOT NULL,
  `apic_name` varchar(255) NOT NULL,
  PRIMARY KEY (`neutron_id`,`neutron_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_apic_names`
--

LOCK TABLES `cisco_ml2_apic_names` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_apic_names` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_apic_names` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_network_bindings`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_network_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_network_bindings` (
  `network_id` varchar(36) NOT NULL,
  `network_type` varchar(32) NOT NULL,
  `segmentation_id` int(11) DEFAULT NULL,
  `profile_id` varchar(36) NOT NULL,
  PRIMARY KEY (`network_id`),
  KEY `profile_id` (`profile_id`),
  CONSTRAINT `cisco_ml2_n1kv_network_bindings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cisco_ml2_n1kv_network_bindings_ibfk_2` FOREIGN KEY (`profile_id`) REFERENCES `cisco_ml2_n1kv_network_profiles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_network_bindings`
--

LOCK TABLES `cisco_ml2_n1kv_network_bindings` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_network_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_network_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_network_profiles`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_network_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_network_profiles` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `segment_type` enum('vlan','vxlan') NOT NULL,
  `segment_range` varchar(255) DEFAULT NULL,
  `multicast_ip_index` int(11) DEFAULT NULL,
  `multicast_ip_range` varchar(255) DEFAULT NULL,
  `sub_type` varchar(255) DEFAULT NULL,
  `physical_network` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_network_profiles`
--

LOCK TABLES `cisco_ml2_n1kv_network_profiles` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_network_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_network_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_policy_profiles`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_policy_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_policy_profiles` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) NOT NULL,
  `vsm_ip` varchar(16) NOT NULL,
  PRIMARY KEY (`id`,`vsm_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_policy_profiles`
--

LOCK TABLES `cisco_ml2_n1kv_policy_profiles` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_policy_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_policy_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_port_bindings`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_port_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_port_bindings` (
  `port_id` varchar(36) NOT NULL,
  `profile_id` varchar(36) NOT NULL,
  PRIMARY KEY (`port_id`),
  CONSTRAINT `cisco_ml2_n1kv_port_bindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_port_bindings`
--

LOCK TABLES `cisco_ml2_n1kv_port_bindings` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_port_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_port_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_profile_bindings`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_profile_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_profile_bindings` (
  `profile_type` enum('network','policy') DEFAULT NULL,
  `tenant_id` varchar(36) NOT NULL DEFAULT 'tenant_id_not_set',
  `profile_id` varchar(36) NOT NULL,
  PRIMARY KEY (`tenant_id`,`profile_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_profile_bindings`
--

LOCK TABLES `cisco_ml2_n1kv_profile_bindings` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_profile_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_profile_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_vlan_allocations`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_vlan_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_vlan_allocations` (
  `physical_network` varchar(64) NOT NULL,
  `vlan_id` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL,
  `network_profile_id` varchar(36) NOT NULL,
  PRIMARY KEY (`physical_network`,`vlan_id`),
  KEY `network_profile_id` (`network_profile_id`),
  CONSTRAINT `cisco_ml2_n1kv_vlan_allocations_ibfk_1` FOREIGN KEY (`network_profile_id`) REFERENCES `cisco_ml2_n1kv_network_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_vlan_allocations`
--

LOCK TABLES `cisco_ml2_n1kv_vlan_allocations` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_vlan_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_vlan_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_n1kv_vxlan_allocations`
--

DROP TABLE IF EXISTS `cisco_ml2_n1kv_vxlan_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_n1kv_vxlan_allocations` (
  `vxlan_id` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL,
  `network_profile_id` varchar(36) NOT NULL,
  PRIMARY KEY (`vxlan_id`),
  KEY `network_profile_id` (`network_profile_id`),
  CONSTRAINT `cisco_ml2_n1kv_vxlan_allocations_ibfk_1` FOREIGN KEY (`network_profile_id`) REFERENCES `cisco_ml2_n1kv_network_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_n1kv_vxlan_allocations`
--

LOCK TABLES `cisco_ml2_n1kv_vxlan_allocations` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_vxlan_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_n1kv_vxlan_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_nexus_nve`
--

DROP TABLE IF EXISTS `cisco_ml2_nexus_nve`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_nexus_nve` (
  `vni` int(11) NOT NULL AUTO_INCREMENT,
  `switch_ip` varchar(255) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `mcast_group` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`vni`,`switch_ip`,`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_nexus_nve`
--

LOCK TABLES `cisco_ml2_nexus_nve` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_nexus_nve` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_nexus_nve` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_ml2_nexusport_bindings`
--

DROP TABLE IF EXISTS `cisco_ml2_nexusport_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_ml2_nexusport_bindings` (
  `binding_id` int(11) NOT NULL AUTO_INCREMENT,
  `port_id` varchar(255) DEFAULT NULL,
  `vlan_id` int(11) NOT NULL,
  `switch_ip` varchar(255) DEFAULT NULL,
  `instance_id` varchar(255) DEFAULT NULL,
  `vni` int(11) DEFAULT NULL,
  `is_provider_vlan` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`binding_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_ml2_nexusport_bindings`
--

LOCK TABLES `cisco_ml2_nexusport_bindings` WRITE;
/*!40000 ALTER TABLE `cisco_ml2_nexusport_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_ml2_nexusport_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_port_mappings`
--

DROP TABLE IF EXISTS `cisco_port_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_port_mappings` (
  `logical_resource_id` varchar(36) NOT NULL,
  `logical_port_id` varchar(36) NOT NULL,
  `port_type` varchar(32) DEFAULT NULL,
  `network_type` varchar(32) DEFAULT NULL,
  `hosting_port_id` varchar(36) DEFAULT NULL,
  `segmentation_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`logical_resource_id`,`logical_port_id`),
  KEY `hosting_port_id` (`hosting_port_id`),
  KEY `logical_port_id` (`logical_port_id`),
  CONSTRAINT `cisco_port_mappings_ibfk_1` FOREIGN KEY (`hosting_port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `cisco_port_mappings_ibfk_2` FOREIGN KEY (`logical_port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_port_mappings`
--

LOCK TABLES `cisco_port_mappings` WRITE;
/*!40000 ALTER TABLE `cisco_port_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_port_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cisco_router_mappings`
--

DROP TABLE IF EXISTS `cisco_router_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cisco_router_mappings` (
  `router_id` varchar(36) NOT NULL,
  `auto_schedule` tinyint(1) NOT NULL,
  `hosting_device_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`router_id`),
  KEY `hosting_device_id` (`hosting_device_id`),
  CONSTRAINT `cisco_router_mappings_ibfk_1` FOREIGN KEY (`hosting_device_id`) REFERENCES `cisco_hosting_devices` (`id`) ON DELETE SET NULL,
  CONSTRAINT `cisco_router_mappings_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cisco_router_mappings`
--

LOCK TABLES `cisco_router_mappings` WRITE;
/*!40000 ALTER TABLE `cisco_router_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `cisco_router_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `consistencyhashes`
--

DROP TABLE IF EXISTS `consistencyhashes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `consistencyhashes` (
  `hash_id` varchar(255) NOT NULL,
  `hash` varchar(255) NOT NULL,
  PRIMARY KEY (`hash_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consistencyhashes`
--

LOCK TABLES `consistencyhashes` WRITE;
/*!40000 ALTER TABLE `consistencyhashes` DISABLE KEYS */;
/*!40000 ALTER TABLE `consistencyhashes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `default_security_group`
--

DROP TABLE IF EXISTS `default_security_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `default_security_group` (
  `tenant_id` varchar(255) NOT NULL,
  `security_group_id` varchar(36) NOT NULL,
  PRIMARY KEY (`tenant_id`),
  KEY `security_group_id` (`security_group_id`),
  CONSTRAINT `default_security_group_ibfk_1` FOREIGN KEY (`security_group_id`) REFERENCES `securitygroups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `default_security_group`
--

LOCK TABLES `default_security_group` WRITE;
/*!40000 ALTER TABLE `default_security_group` DISABLE KEYS */;
INSERT INTO `default_security_group` VALUES ('b66fea8bb53a4b76bcda394c67284c53','08e1ed01-9c9a-448d-b465-87c99ca43d20'),('68bed08176254214900d5d6112eb3284','1fd2dfa5-6205-4894-abcb-44850f8c9600'),('391750905a7f491d85932ad58e809362','869cf42b-1d82-40e2-a975-adde0ce3e689'),('52d692239a014c9a9960ce6b64d25dc8','eee365de-cd10-4680-b1b9-29c4d35f1829');
/*!40000 ALTER TABLE `default_security_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dnsnameservers`
--

DROP TABLE IF EXISTS `dnsnameservers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dnsnameservers` (
  `address` varchar(128) NOT NULL,
  `subnet_id` varchar(36) NOT NULL,
  `order` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`address`,`subnet_id`),
  KEY `subnet_id` (`subnet_id`),
  CONSTRAINT `dnsnameservers_ibfk_1` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dnsnameservers`
--

LOCK TABLES `dnsnameservers` WRITE;
/*!40000 ALTER TABLE `dnsnameservers` DISABLE KEYS */;
INSERT INTO `dnsnameservers` VALUES ('8.8.8.8','029772dd-bb4f-41f8-8dd6-831f7bbc540a',0),('8.8.8.8','4160ef6e-0301-440b-a602-93a2c9080025',0),('8.8.8.8','d9b48087-72ed-4c99-95cd-466f46b0c361',0);
/*!40000 ALTER TABLE `dnsnameservers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dvr_host_macs`
--

DROP TABLE IF EXISTS `dvr_host_macs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dvr_host_macs` (
  `host` varchar(255) NOT NULL,
  `mac_address` varchar(32) NOT NULL,
  PRIMARY KEY (`host`),
  UNIQUE KEY `mac_address` (`mac_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dvr_host_macs`
--

LOCK TABLES `dvr_host_macs` WRITE;
/*!40000 ALTER TABLE `dvr_host_macs` DISABLE KEYS */;
/*!40000 ALTER TABLE `dvr_host_macs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `externalnetworks`
--

DROP TABLE IF EXISTS `externalnetworks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `externalnetworks` (
  `network_id` varchar(36) NOT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`network_id`),
  CONSTRAINT `externalnetworks_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `externalnetworks`
--

LOCK TABLES `externalnetworks` WRITE;
/*!40000 ALTER TABLE `externalnetworks` DISABLE KEYS */;
INSERT INTO `externalnetworks` VALUES ('0267647d-80ff-4e2a-afbd-76ade85fa731',0);
/*!40000 ALTER TABLE `externalnetworks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `extradhcpopts`
--

DROP TABLE IF EXISTS `extradhcpopts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `extradhcpopts` (
  `id` varchar(36) NOT NULL,
  `port_id` varchar(36) NOT NULL,
  `opt_name` varchar(64) NOT NULL,
  `opt_value` varchar(255) NOT NULL,
  `ip_version` int(11) NOT NULL DEFAULT '4',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_extradhcpopts0portid0optname0ipversion` (`port_id`,`opt_name`,`ip_version`),
  CONSTRAINT `extradhcpopts_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `extradhcpopts`
--

LOCK TABLES `extradhcpopts` WRITE;
/*!40000 ALTER TABLE `extradhcpopts` DISABLE KEYS */;
/*!40000 ALTER TABLE `extradhcpopts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `firewall_policies`
--

DROP TABLE IF EXISTS `firewall_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `firewall_policies` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `shared` tinyint(1) DEFAULT NULL,
  `audited` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_firewall_policies_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `firewall_policies`
--

LOCK TABLES `firewall_policies` WRITE;
/*!40000 ALTER TABLE `firewall_policies` DISABLE KEYS */;
/*!40000 ALTER TABLE `firewall_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `firewall_router_associations`
--

DROP TABLE IF EXISTS `firewall_router_associations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `firewall_router_associations` (
  `fw_id` varchar(36) NOT NULL,
  `router_id` varchar(36) NOT NULL,
  PRIMARY KEY (`fw_id`,`router_id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `firewall_router_associations_ibfk_1` FOREIGN KEY (`fw_id`) REFERENCES `firewalls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `firewall_router_associations_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `firewall_router_associations`
--

LOCK TABLES `firewall_router_associations` WRITE;
/*!40000 ALTER TABLE `firewall_router_associations` DISABLE KEYS */;
/*!40000 ALTER TABLE `firewall_router_associations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `firewall_rules`
--

DROP TABLE IF EXISTS `firewall_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `firewall_rules` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `firewall_policy_id` varchar(36) DEFAULT NULL,
  `shared` tinyint(1) DEFAULT NULL,
  `protocol` varchar(40) DEFAULT NULL,
  `ip_version` int(11) NOT NULL,
  `source_ip_address` varchar(46) DEFAULT NULL,
  `destination_ip_address` varchar(46) DEFAULT NULL,
  `source_port_range_min` int(11) DEFAULT NULL,
  `source_port_range_max` int(11) DEFAULT NULL,
  `destination_port_range_min` int(11) DEFAULT NULL,
  `destination_port_range_max` int(11) DEFAULT NULL,
  `action` enum('allow','deny','reject') DEFAULT NULL,
  `enabled` tinyint(1) DEFAULT NULL,
  `position` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `firewall_rules_ibfk_1` (`firewall_policy_id`),
  KEY `ix_firewall_rules_tenant_id` (`tenant_id`),
  CONSTRAINT `firewall_rules_ibfk_1` FOREIGN KEY (`firewall_policy_id`) REFERENCES `firewall_policies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `firewall_rules`
--

LOCK TABLES `firewall_rules` WRITE;
/*!40000 ALTER TABLE `firewall_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `firewall_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `firewalls`
--

DROP TABLE IF EXISTS `firewalls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `firewalls` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `shared` tinyint(1) DEFAULT NULL,
  `admin_state_up` tinyint(1) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `firewall_policy_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `firewalls_ibfk_1` (`firewall_policy_id`),
  KEY `ix_firewalls_tenant_id` (`tenant_id`),
  CONSTRAINT `firewalls_ibfk_1` FOREIGN KEY (`firewall_policy_id`) REFERENCES `firewall_policies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `firewalls`
--

LOCK TABLES `firewalls` WRITE;
/*!40000 ALTER TABLE `firewalls` DISABLE KEYS */;
/*!40000 ALTER TABLE `firewalls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flavors`
--

DROP TABLE IF EXISTS `flavors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flavors` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `service_type` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flavors`
--

LOCK TABLES `flavors` WRITE;
/*!40000 ALTER TABLE `flavors` DISABLE KEYS */;
/*!40000 ALTER TABLE `flavors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flavorserviceprofilebindings`
--

DROP TABLE IF EXISTS `flavorserviceprofilebindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `flavorserviceprofilebindings` (
  `service_profile_id` varchar(36) NOT NULL,
  `flavor_id` varchar(36) NOT NULL,
  PRIMARY KEY (`service_profile_id`,`flavor_id`),
  KEY `flavorserviceprofilebindings_ibfk_2` (`flavor_id`),
  CONSTRAINT `flavorserviceprofilebindings_ibfk_1` FOREIGN KEY (`service_profile_id`) REFERENCES `serviceprofiles` (`id`) ON DELETE CASCADE,
  CONSTRAINT `flavorserviceprofilebindings_ibfk_2` FOREIGN KEY (`flavor_id`) REFERENCES `flavors` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flavorserviceprofilebindings`
--

LOCK TABLES `flavorserviceprofilebindings` WRITE;
/*!40000 ALTER TABLE `flavorserviceprofilebindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `flavorserviceprofilebindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `floatingipdnses`
--

DROP TABLE IF EXISTS `floatingipdnses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `floatingipdnses` (
  `floatingip_id` varchar(36) NOT NULL,
  `dns_name` varchar(255) NOT NULL,
  `dns_domain` varchar(255) NOT NULL,
  `published_dns_name` varchar(255) NOT NULL,
  `published_dns_domain` varchar(255) NOT NULL,
  PRIMARY KEY (`floatingip_id`),
  KEY `ix_floatingipdnses_floatingip_id` (`floatingip_id`),
  CONSTRAINT `floatingipdnses_ibfk_1` FOREIGN KEY (`floatingip_id`) REFERENCES `floatingips` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `floatingipdnses`
--

LOCK TABLES `floatingipdnses` WRITE;
/*!40000 ALTER TABLE `floatingipdnses` DISABLE KEYS */;
/*!40000 ALTER TABLE `floatingipdnses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `floatingips`
--

DROP TABLE IF EXISTS `floatingips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `floatingips` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `floating_ip_address` varchar(64) NOT NULL,
  `floating_network_id` varchar(36) NOT NULL,
  `floating_port_id` varchar(36) NOT NULL,
  `fixed_port_id` varchar(36) DEFAULT NULL,
  `fixed_ip_address` varchar(64) DEFAULT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  `last_known_router_id` varchar(36) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_floatingips0standard_attr_id` (`standard_attr_id`),
  KEY `fixed_port_id` (`fixed_port_id`),
  KEY `floating_port_id` (`floating_port_id`),
  KEY `router_id` (`router_id`),
  KEY `ix_floatingips_tenant_id` (`tenant_id`),
  CONSTRAINT `floatingips_ibfk_1` FOREIGN KEY (`fixed_port_id`) REFERENCES `ports` (`id`),
  CONSTRAINT `floatingips_ibfk_2` FOREIGN KEY (`floating_port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `floatingips_ibfk_3` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`),
  CONSTRAINT `floatingips_ibfk_4` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `floatingips`
--

LOCK TABLES `floatingips` WRITE;
/*!40000 ALTER TABLE `floatingips` DISABLE KEYS */;
INSERT INTO `floatingips` VALUES ('68bed08176254214900d5d6112eb3284','c3658951-4d15-4335-ba60-4efc45524867','203.0.113.103','0267647d-80ff-4e2a-afbd-76ade85fa731','465ef707-f517-43ab-b2b8-eeb68f9d3eeb','737565c0-2c6d-4bab-9985-8f3a1c069881','192.168.13.3','b77acd74-91f9-49d5-b478-123726ec4cd8',NULL,'ACTIVE',42);
/*!40000 ALTER TABLE `floatingips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ha_router_agent_port_bindings`
--

DROP TABLE IF EXISTS `ha_router_agent_port_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ha_router_agent_port_bindings` (
  `port_id` varchar(36) NOT NULL,
  `router_id` varchar(36) NOT NULL,
  `l3_agent_id` varchar(36) DEFAULT NULL,
  `state` enum('active','standby') DEFAULT 'standby',
  PRIMARY KEY (`port_id`),
  UNIQUE KEY `uniq_ha_router_agent_port_bindings0port_id0l3_agent_id` (`router_id`,`l3_agent_id`),
  KEY `l3_agent_id` (`l3_agent_id`),
  CONSTRAINT `ha_router_agent_port_bindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ha_router_agent_port_bindings_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ha_router_agent_port_bindings_ibfk_3` FOREIGN KEY (`l3_agent_id`) REFERENCES `agents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ha_router_agent_port_bindings`
--

LOCK TABLES `ha_router_agent_port_bindings` WRITE;
/*!40000 ALTER TABLE `ha_router_agent_port_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `ha_router_agent_port_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ha_router_networks`
--

DROP TABLE IF EXISTS `ha_router_networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ha_router_networks` (
  `tenant_id` varchar(255) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  PRIMARY KEY (`tenant_id`,`network_id`),
  KEY `network_id` (`network_id`),
  CONSTRAINT `ha_router_networks_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ha_router_networks`
--

LOCK TABLES `ha_router_networks` WRITE;
/*!40000 ALTER TABLE `ha_router_networks` DISABLE KEYS */;
/*!40000 ALTER TABLE `ha_router_networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ha_router_vrid_allocations`
--

DROP TABLE IF EXISTS `ha_router_vrid_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ha_router_vrid_allocations` (
  `network_id` varchar(36) NOT NULL,
  `vr_id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`network_id`,`vr_id`),
  KEY `idx_autoinc_vr_id` (`vr_id`),
  CONSTRAINT `ha_router_vrid_allocations_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ha_router_vrid_allocations`
--

LOCK TABLES `ha_router_vrid_allocations` WRITE;
/*!40000 ALTER TABLE `ha_router_vrid_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ha_router_vrid_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `healthmonitors`
--

DROP TABLE IF EXISTS `healthmonitors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `healthmonitors` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `type` enum('PING','TCP','HTTP','HTTPS') NOT NULL,
  `delay` int(11) NOT NULL,
  `timeout` int(11) NOT NULL,
  `max_retries` int(11) NOT NULL,
  `http_method` varchar(16) DEFAULT NULL,
  `url_path` varchar(255) DEFAULT NULL,
  `expected_codes` varchar(64) DEFAULT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `healthmonitors`
--

LOCK TABLES `healthmonitors` WRITE;
/*!40000 ALTER TABLE `healthmonitors` DISABLE KEYS */;
/*!40000 ALTER TABLE `healthmonitors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ikepolicies`
--

DROP TABLE IF EXISTS `ikepolicies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ikepolicies` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `auth_algorithm` enum('sha1') NOT NULL,
  `encryption_algorithm` enum('3des','aes-128','aes-256','aes-192') NOT NULL,
  `phase1_negotiation_mode` enum('main') NOT NULL,
  `lifetime_units` enum('seconds','kilobytes') NOT NULL,
  `lifetime_value` int(11) NOT NULL,
  `ike_version` enum('v1','v2') NOT NULL,
  `pfs` enum('group2','group5','group14') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ikepolicies`
--

LOCK TABLES `ikepolicies` WRITE;
/*!40000 ALTER TABLE `ikepolicies` DISABLE KEYS */;
/*!40000 ALTER TABLE `ikepolicies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipallocationpools`
--

DROP TABLE IF EXISTS `ipallocationpools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipallocationpools` (
  `id` varchar(36) NOT NULL,
  `subnet_id` varchar(36) DEFAULT NULL,
  `first_ip` varchar(64) NOT NULL,
  `last_ip` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `subnet_id` (`subnet_id`),
  CONSTRAINT `ipallocationpools_ibfk_1` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipallocationpools`
--

LOCK TABLES `ipallocationpools` WRITE;
/*!40000 ALTER TABLE `ipallocationpools` DISABLE KEYS */;
INSERT INTO `ipallocationpools` VALUES ('24c49fad-d318-4b53-a710-f8b9066d572c','029772dd-bb4f-41f8-8dd6-831f7bbc540a','192.168.13.2','192.168.13.254'),('d3471cf7-00ee-48aa-bbfa-70410a6b029c','d9b48087-72ed-4c99-95cd-466f46b0c361','203.0.113.100','203.0.113.200'),('fbf07ee0-8681-4f30-9256-892cdfd04d4c','4160ef6e-0301-440b-a602-93a2c9080025','192.168.13.2','192.168.13.254');
/*!40000 ALTER TABLE `ipallocationpools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipallocations`
--

DROP TABLE IF EXISTS `ipallocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipallocations` (
  `port_id` varchar(36) DEFAULT NULL,
  `ip_address` varchar(64) NOT NULL,
  `subnet_id` varchar(36) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  PRIMARY KEY (`ip_address`,`subnet_id`,`network_id`),
  KEY `network_id` (`network_id`),
  KEY `port_id` (`port_id`),
  KEY `subnet_id` (`subnet_id`),
  CONSTRAINT `ipallocations_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ipallocations_ibfk_2` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ipallocations_ibfk_3` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipallocations`
--

LOCK TABLES `ipallocations` WRITE;
/*!40000 ALTER TABLE `ipallocations` DISABLE KEYS */;
INSERT INTO `ipallocations` VALUES ('09d66f0a-4686-412b-9cf8-7874a1d0ef30','203.0.113.100','d9b48087-72ed-4c99-95cd-466f46b0c361','0267647d-80ff-4e2a-afbd-76ade85fa731'),('1bffddfb-0afe-4d78-b421-b88d9d8e921b','192.168.13.2','4160ef6e-0301-440b-a602-93a2c9080025','c308c5a6-3682-46e2-b633-6685d7b34b0e'),('465ef707-f517-43ab-b2b8-eeb68f9d3eeb','203.0.113.103','d9b48087-72ed-4c99-95cd-466f46b0c361','0267647d-80ff-4e2a-afbd-76ade85fa731'),('5589876b-8c66-4bfb-a465-c6509a277972','203.0.113.102','d9b48087-72ed-4c99-95cd-466f46b0c361','0267647d-80ff-4e2a-afbd-76ade85fa731'),('5a347f2e-bcec-4dd8-b09d-318601894fc2','203.0.113.101','d9b48087-72ed-4c99-95cd-466f46b0c361','0267647d-80ff-4e2a-afbd-76ade85fa731'),('6a98a347-8752-4f71-b485-257f6f1a2d04','192.168.13.1','029772dd-bb4f-41f8-8dd6-831f7bbc540a','a19a8724-c0e4-4944-a636-f4a166537786'),('737565c0-2c6d-4bab-9985-8f3a1c069881','192.168.13.3','029772dd-bb4f-41f8-8dd6-831f7bbc540a','a19a8724-c0e4-4944-a636-f4a166537786'),('a61e24db-b68a-4df2-8af5-17a3295a0b8c','192.168.13.2','029772dd-bb4f-41f8-8dd6-831f7bbc540a','a19a8724-c0e4-4944-a636-f4a166537786'),('fb358de8-c4c7-4f86-822a-bd374ecb792a','192.168.13.1','4160ef6e-0301-440b-a602-93a2c9080025','c308c5a6-3682-46e2-b633-6685d7b34b0e');
/*!40000 ALTER TABLE `ipallocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipamallocationpools`
--

DROP TABLE IF EXISTS `ipamallocationpools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipamallocationpools` (
  `id` varchar(36) NOT NULL,
  `ipam_subnet_id` varchar(36) NOT NULL,
  `first_ip` varchar(64) NOT NULL,
  `last_ip` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `ipam_subnet_id` (`ipam_subnet_id`),
  CONSTRAINT `ipamallocationpools_ibfk_1` FOREIGN KEY (`ipam_subnet_id`) REFERENCES `ipamsubnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipamallocationpools`
--

LOCK TABLES `ipamallocationpools` WRITE;
/*!40000 ALTER TABLE `ipamallocationpools` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipamallocationpools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipamallocations`
--

DROP TABLE IF EXISTS `ipamallocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipamallocations` (
  `ip_address` varchar(64) NOT NULL,
  `status` varchar(36) DEFAULT NULL,
  `ipam_subnet_id` varchar(36) NOT NULL,
  PRIMARY KEY (`ip_address`,`ipam_subnet_id`),
  KEY `ipam_subnet_id` (`ipam_subnet_id`),
  CONSTRAINT `ipamallocations_ibfk_1` FOREIGN KEY (`ipam_subnet_id`) REFERENCES `ipamsubnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipamallocations`
--

LOCK TABLES `ipamallocations` WRITE;
/*!40000 ALTER TABLE `ipamallocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipamallocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipamavailabilityranges`
--

DROP TABLE IF EXISTS `ipamavailabilityranges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipamavailabilityranges` (
  `allocation_pool_id` varchar(36) NOT NULL,
  `first_ip` varchar(64) NOT NULL,
  `last_ip` varchar(64) NOT NULL,
  PRIMARY KEY (`allocation_pool_id`,`first_ip`,`last_ip`),
  KEY `ix_ipamavailabilityranges_first_ip_allocation_pool_id` (`first_ip`,`allocation_pool_id`),
  KEY `ix_ipamavailabilityranges_last_ip_allocation_pool_id` (`last_ip`,`allocation_pool_id`),
  CONSTRAINT `ipamavailabilityranges_ibfk_1` FOREIGN KEY (`allocation_pool_id`) REFERENCES `ipamallocationpools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipamavailabilityranges`
--

LOCK TABLES `ipamavailabilityranges` WRITE;
/*!40000 ALTER TABLE `ipamavailabilityranges` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipamavailabilityranges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipamsubnets`
--

DROP TABLE IF EXISTS `ipamsubnets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipamsubnets` (
  `id` varchar(36) NOT NULL,
  `neutron_subnet_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipamsubnets`
--

LOCK TABLES `ipamsubnets` WRITE;
/*!40000 ALTER TABLE `ipamsubnets` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipamsubnets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipavailabilityranges`
--

DROP TABLE IF EXISTS `ipavailabilityranges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipavailabilityranges` (
  `allocation_pool_id` varchar(36) NOT NULL,
  `first_ip` varchar(64) NOT NULL,
  `last_ip` varchar(64) NOT NULL,
  PRIMARY KEY (`allocation_pool_id`,`first_ip`,`last_ip`),
  UNIQUE KEY `uniq_ipavailabilityranges0first_ip0allocation_pool_id` (`first_ip`,`allocation_pool_id`),
  UNIQUE KEY `uniq_ipavailabilityranges0last_ip0allocation_pool_id` (`last_ip`,`allocation_pool_id`),
  CONSTRAINT `ipavailabilityranges_ibfk_1` FOREIGN KEY (`allocation_pool_id`) REFERENCES `ipallocationpools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipavailabilityranges`
--

LOCK TABLES `ipavailabilityranges` WRITE;
/*!40000 ALTER TABLE `ipavailabilityranges` DISABLE KEYS */;
INSERT INTO `ipavailabilityranges` VALUES ('24c49fad-d318-4b53-a710-f8b9066d572c','192.168.13.4','192.168.13.254'),('d3471cf7-00ee-48aa-bbfa-70410a6b029c','203.0.113.104','203.0.113.200'),('fbf07ee0-8681-4f30-9256-892cdfd04d4c','192.168.13.3','192.168.13.254');
/*!40000 ALTER TABLE `ipavailabilityranges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipsec_site_connections`
--

DROP TABLE IF EXISTS `ipsec_site_connections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipsec_site_connections` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `peer_address` varchar(255) NOT NULL,
  `peer_id` varchar(255) NOT NULL,
  `route_mode` varchar(8) NOT NULL,
  `mtu` int(11) NOT NULL,
  `initiator` enum('bi-directional','response-only') NOT NULL,
  `auth_mode` varchar(16) NOT NULL,
  `psk` varchar(255) NOT NULL,
  `dpd_action` enum('hold','clear','restart','disabled','restart-by-peer') NOT NULL,
  `dpd_interval` int(11) NOT NULL,
  `dpd_timeout` int(11) NOT NULL,
  `status` varchar(16) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `vpnservice_id` varchar(36) NOT NULL,
  `ipsecpolicy_id` varchar(36) NOT NULL,
  `ikepolicy_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `vpnservice_id` (`vpnservice_id`),
  KEY `ipsecpolicy_id` (`ipsecpolicy_id`),
  KEY `ikepolicy_id` (`ikepolicy_id`),
  CONSTRAINT `ipsec_site_connections_ibfk_1` FOREIGN KEY (`vpnservice_id`) REFERENCES `vpnservices` (`id`),
  CONSTRAINT `ipsec_site_connections_ibfk_2` FOREIGN KEY (`ipsecpolicy_id`) REFERENCES `ipsecpolicies` (`id`),
  CONSTRAINT `ipsec_site_connections_ibfk_3` FOREIGN KEY (`ikepolicy_id`) REFERENCES `ikepolicies` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipsec_site_connections`
--

LOCK TABLES `ipsec_site_connections` WRITE;
/*!40000 ALTER TABLE `ipsec_site_connections` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipsec_site_connections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipsecpeercidrs`
--

DROP TABLE IF EXISTS `ipsecpeercidrs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipsecpeercidrs` (
  `cidr` varchar(32) NOT NULL,
  `ipsec_site_connection_id` varchar(36) NOT NULL,
  PRIMARY KEY (`cidr`,`ipsec_site_connection_id`),
  KEY `ipsec_site_connection_id` (`ipsec_site_connection_id`),
  CONSTRAINT `ipsecpeercidrs_ibfk_1` FOREIGN KEY (`ipsec_site_connection_id`) REFERENCES `ipsec_site_connections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipsecpeercidrs`
--

LOCK TABLES `ipsecpeercidrs` WRITE;
/*!40000 ALTER TABLE `ipsecpeercidrs` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipsecpeercidrs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ipsecpolicies`
--

DROP TABLE IF EXISTS `ipsecpolicies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ipsecpolicies` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `transform_protocol` enum('esp','ah','ah-esp') NOT NULL,
  `auth_algorithm` enum('sha1') NOT NULL,
  `encryption_algorithm` enum('3des','aes-128','aes-256','aes-192') NOT NULL,
  `encapsulation_mode` enum('tunnel','transport') NOT NULL,
  `lifetime_units` enum('seconds','kilobytes') NOT NULL,
  `lifetime_value` int(11) NOT NULL,
  `pfs` enum('group2','group5','group14') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipsecpolicies`
--

LOCK TABLES `ipsecpolicies` WRITE;
/*!40000 ALTER TABLE `ipsecpolicies` DISABLE KEYS */;
/*!40000 ALTER TABLE `ipsecpolicies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lsn`
--

DROP TABLE IF EXISTS `lsn`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lsn` (
  `net_id` varchar(36) NOT NULL,
  `lsn_id` varchar(36) NOT NULL,
  PRIMARY KEY (`lsn_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lsn`
--

LOCK TABLES `lsn` WRITE;
/*!40000 ALTER TABLE `lsn` DISABLE KEYS */;
/*!40000 ALTER TABLE `lsn` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lsn_port`
--

DROP TABLE IF EXISTS `lsn_port`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lsn_port` (
  `lsn_port_id` varchar(36) NOT NULL,
  `lsn_id` varchar(36) NOT NULL,
  `sub_id` varchar(36) NOT NULL,
  `mac_addr` varchar(32) NOT NULL,
  PRIMARY KEY (`lsn_port_id`),
  UNIQUE KEY `sub_id` (`sub_id`),
  UNIQUE KEY `mac_addr` (`mac_addr`),
  KEY `lsn_id` (`lsn_id`),
  CONSTRAINT `lsn_port_ibfk_1` FOREIGN KEY (`lsn_id`) REFERENCES `lsn` (`lsn_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lsn_port`
--

LOCK TABLES `lsn_port` WRITE;
/*!40000 ALTER TABLE `lsn_port` DISABLE KEYS */;
/*!40000 ALTER TABLE `lsn_port` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maclearningstates`
--

DROP TABLE IF EXISTS `maclearningstates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `maclearningstates` (
  `port_id` varchar(36) NOT NULL,
  `mac_learning_enabled` tinyint(1) NOT NULL,
  PRIMARY KEY (`port_id`),
  CONSTRAINT `maclearningstates_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maclearningstates`
--

LOCK TABLES `maclearningstates` WRITE;
/*!40000 ALTER TABLE `maclearningstates` DISABLE KEYS */;
/*!40000 ALTER TABLE `maclearningstates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `members` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `pool_id` varchar(36) NOT NULL,
  `address` varchar(64) NOT NULL,
  `protocol_port` int(11) NOT NULL,
  `weight` int(11) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_member0pool_id0address0port` (`pool_id`,`address`,`protocol_port`),
  CONSTRAINT `members_ibfk_1` FOREIGN KEY (`pool_id`) REFERENCES `pools` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `members`
--

LOCK TABLES `members` WRITE;
/*!40000 ALTER TABLE `members` DISABLE KEYS */;
/*!40000 ALTER TABLE `members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `meteringlabelrules`
--

DROP TABLE IF EXISTS `meteringlabelrules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meteringlabelrules` (
  `id` varchar(36) NOT NULL,
  `direction` enum('ingress','egress') DEFAULT NULL,
  `remote_ip_prefix` varchar(64) DEFAULT NULL,
  `metering_label_id` varchar(36) NOT NULL,
  `excluded` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `metering_label_id` (`metering_label_id`),
  CONSTRAINT `meteringlabelrules_ibfk_1` FOREIGN KEY (`metering_label_id`) REFERENCES `meteringlabels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `meteringlabelrules`
--

LOCK TABLES `meteringlabelrules` WRITE;
/*!40000 ALTER TABLE `meteringlabelrules` DISABLE KEYS */;
/*!40000 ALTER TABLE `meteringlabelrules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `meteringlabels`
--

DROP TABLE IF EXISTS `meteringlabels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `meteringlabels` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `shared` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `ix_meteringlabels_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `meteringlabels`
--

LOCK TABLES `meteringlabels` WRITE;
/*!40000 ALTER TABLE `meteringlabels` DISABLE KEYS */;
/*!40000 ALTER TABLE `meteringlabels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_brocadenetworks`
--

DROP TABLE IF EXISTS `ml2_brocadenetworks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_brocadenetworks` (
  `id` varchar(36) NOT NULL,
  `vlan` varchar(10) DEFAULT NULL,
  `segment_id` varchar(36) DEFAULT NULL,
  `network_type` varchar(10) DEFAULT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_ml2_brocadenetworks_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_brocadenetworks`
--

LOCK TABLES `ml2_brocadenetworks` WRITE;
/*!40000 ALTER TABLE `ml2_brocadenetworks` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_brocadenetworks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_brocadeports`
--

DROP TABLE IF EXISTS `ml2_brocadeports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_brocadeports` (
  `id` varchar(36) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `physical_interface` varchar(36) DEFAULT NULL,
  `vlan_id` varchar(36) DEFAULT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `network_id` (`network_id`),
  KEY `ix_ml2_brocadeports_tenant_id` (`tenant_id`),
  CONSTRAINT `ml2_brocadeports_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `ml2_brocadenetworks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_brocadeports`
--

LOCK TABLES `ml2_brocadeports` WRITE;
/*!40000 ALTER TABLE `ml2_brocadeports` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_brocadeports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_dvr_port_bindings`
--

DROP TABLE IF EXISTS `ml2_dvr_port_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_dvr_port_bindings` (
  `port_id` varchar(36) NOT NULL,
  `host` varchar(255) NOT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  `vif_type` varchar(64) NOT NULL,
  `vif_details` varchar(4095) NOT NULL DEFAULT '',
  `vnic_type` varchar(64) NOT NULL DEFAULT 'normal',
  `profile` varchar(4095) NOT NULL DEFAULT '',
  `status` varchar(16) NOT NULL,
  PRIMARY KEY (`port_id`,`host`),
  CONSTRAINT `ml2_dvr_port_bindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_dvr_port_bindings`
--

LOCK TABLES `ml2_dvr_port_bindings` WRITE;
/*!40000 ALTER TABLE `ml2_dvr_port_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_dvr_port_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_flat_allocations`
--

DROP TABLE IF EXISTS `ml2_flat_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_flat_allocations` (
  `physical_network` varchar(64) NOT NULL,
  PRIMARY KEY (`physical_network`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_flat_allocations`
--

LOCK TABLES `ml2_flat_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_flat_allocations` DISABLE KEYS */;
INSERT INTO `ml2_flat_allocations` VALUES ('provider');
/*!40000 ALTER TABLE `ml2_flat_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_geneve_allocations`
--

DROP TABLE IF EXISTS `ml2_geneve_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_geneve_allocations` (
  `geneve_vni` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`geneve_vni`),
  KEY `ix_ml2_geneve_allocations_allocated` (`allocated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_geneve_allocations`
--

LOCK TABLES `ml2_geneve_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_geneve_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_geneve_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_geneve_endpoints`
--

DROP TABLE IF EXISTS `ml2_geneve_endpoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_geneve_endpoints` (
  `ip_address` varchar(64) NOT NULL,
  `host` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ip_address`),
  UNIQUE KEY `unique_ml2_geneve_endpoints0host` (`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_geneve_endpoints`
--

LOCK TABLES `ml2_geneve_endpoints` WRITE;
/*!40000 ALTER TABLE `ml2_geneve_endpoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_geneve_endpoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_gre_allocations`
--

DROP TABLE IF EXISTS `ml2_gre_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_gre_allocations` (
  `gre_id` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`gre_id`),
  KEY `ix_ml2_gre_allocations_allocated` (`allocated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_gre_allocations`
--

LOCK TABLES `ml2_gre_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_gre_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_gre_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_gre_endpoints`
--

DROP TABLE IF EXISTS `ml2_gre_endpoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_gre_endpoints` (
  `ip_address` varchar(64) NOT NULL,
  `host` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ip_address`),
  UNIQUE KEY `unique_ml2_gre_endpoints0host` (`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_gre_endpoints`
--

LOCK TABLES `ml2_gre_endpoints` WRITE;
/*!40000 ALTER TABLE `ml2_gre_endpoints` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_gre_endpoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_network_segments`
--

DROP TABLE IF EXISTS `ml2_network_segments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_network_segments` (
  `id` varchar(36) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  `network_type` varchar(32) NOT NULL,
  `physical_network` varchar(64) DEFAULT NULL,
  `segmentation_id` int(11) DEFAULT NULL,
  `is_dynamic` tinyint(1) NOT NULL DEFAULT '0',
  `segment_index` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `network_id` (`network_id`),
  CONSTRAINT `ml2_network_segments_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_network_segments`
--

LOCK TABLES `ml2_network_segments` WRITE;
/*!40000 ALTER TABLE `ml2_network_segments` DISABLE KEYS */;
INSERT INTO `ml2_network_segments` VALUES ('0168d37d-9176-4b4a-b4fe-07f675285a93','0267647d-80ff-4e2a-afbd-76ade85fa731','flat','provider',NULL,0,0),('33baf491-345e-4b7d-b716-71ef86bf4370','a19a8724-c0e4-4944-a636-f4a166537786','vxlan',NULL,81,0,0),('79f955b0-bcc2-4686-bf95-63758720c3ea','c308c5a6-3682-46e2-b633-6685d7b34b0e','vxlan',NULL,2,0,0);
/*!40000 ALTER TABLE `ml2_network_segments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_nexus_vxlan_allocations`
--

DROP TABLE IF EXISTS `ml2_nexus_vxlan_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_nexus_vxlan_allocations` (
  `vxlan_vni` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`vxlan_vni`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_nexus_vxlan_allocations`
--

LOCK TABLES `ml2_nexus_vxlan_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_nexus_vxlan_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_nexus_vxlan_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_nexus_vxlan_mcast_groups`
--

DROP TABLE IF EXISTS `ml2_nexus_vxlan_mcast_groups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_nexus_vxlan_mcast_groups` (
  `id` varchar(36) NOT NULL,
  `mcast_group` varchar(64) NOT NULL,
  `associated_vni` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `associated_vni` (`associated_vni`),
  CONSTRAINT `ml2_nexus_vxlan_mcast_groups_ibfk_1` FOREIGN KEY (`associated_vni`) REFERENCES `ml2_nexus_vxlan_allocations` (`vxlan_vni`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_nexus_vxlan_mcast_groups`
--

LOCK TABLES `ml2_nexus_vxlan_mcast_groups` WRITE;
/*!40000 ALTER TABLE `ml2_nexus_vxlan_mcast_groups` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_nexus_vxlan_mcast_groups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_port_binding_levels`
--

DROP TABLE IF EXISTS `ml2_port_binding_levels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_port_binding_levels` (
  `port_id` varchar(36) NOT NULL,
  `host` varchar(255) NOT NULL,
  `level` int(11) NOT NULL,
  `driver` varchar(64) DEFAULT NULL,
  `segment_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`port_id`,`host`,`level`),
  KEY `segment_id` (`segment_id`),
  CONSTRAINT `ml2_port_binding_levels_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ml2_port_binding_levels_ibfk_2` FOREIGN KEY (`segment_id`) REFERENCES `ml2_network_segments` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_port_binding_levels`
--

LOCK TABLES `ml2_port_binding_levels` WRITE;
/*!40000 ALTER TABLE `ml2_port_binding_levels` DISABLE KEYS */;
INSERT INTO `ml2_port_binding_levels` VALUES ('09d66f0a-4686-412b-9cf8-7874a1d0ef30','controller',0,'openvswitch','0168d37d-9176-4b4a-b4fe-07f675285a93'),('1bffddfb-0afe-4d78-b421-b88d9d8e921b','controller',0,'openvswitch','79f955b0-bcc2-4686-bf95-63758720c3ea'),('5589876b-8c66-4bfb-a465-c6509a277972','controller',0,'openvswitch','0168d37d-9176-4b4a-b4fe-07f675285a93'),('5a347f2e-bcec-4dd8-b09d-318601894fc2','controller',0,'openvswitch','0168d37d-9176-4b4a-b4fe-07f675285a93'),('6a98a347-8752-4f71-b485-257f6f1a2d04','controller',0,'openvswitch','33baf491-345e-4b7d-b716-71ef86bf4370'),('737565c0-2c6d-4bab-9985-8f3a1c069881','compute1',0,'openvswitch','33baf491-345e-4b7d-b716-71ef86bf4370'),('a61e24db-b68a-4df2-8af5-17a3295a0b8c','controller',0,'openvswitch','33baf491-345e-4b7d-b716-71ef86bf4370'),('fb358de8-c4c7-4f86-822a-bd374ecb792a','controller',0,'openvswitch','79f955b0-bcc2-4686-bf95-63758720c3ea');
/*!40000 ALTER TABLE `ml2_port_binding_levels` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_port_bindings`
--

DROP TABLE IF EXISTS `ml2_port_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_port_bindings` (
  `port_id` varchar(36) NOT NULL,
  `host` varchar(255) NOT NULL DEFAULT '',
  `vif_type` varchar(64) NOT NULL,
  `vnic_type` varchar(64) NOT NULL DEFAULT 'normal',
  `profile` varchar(4095) NOT NULL DEFAULT '',
  `vif_details` varchar(4095) NOT NULL DEFAULT '',
  PRIMARY KEY (`port_id`),
  CONSTRAINT `ml2_port_bindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_port_bindings`
--

LOCK TABLES `ml2_port_bindings` WRITE;
/*!40000 ALTER TABLE `ml2_port_bindings` DISABLE KEYS */;
INSERT INTO `ml2_port_bindings` VALUES ('09d66f0a-4686-412b-9cf8-7874a1d0ef30','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('1bffddfb-0afe-4d78-b421-b88d9d8e921b','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('465ef707-f517-43ab-b2b8-eeb68f9d3eeb','','unbound','normal','',''),('5589876b-8c66-4bfb-a465-c6509a277972','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('5a347f2e-bcec-4dd8-b09d-318601894fc2','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('6a98a347-8752-4f71-b485-257f6f1a2d04','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('737565c0-2c6d-4bab-9985-8f3a1c069881','compute1','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": false}'),('a61e24db-b68a-4df2-8af5-17a3295a0b8c','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}'),('fb358de8-c4c7-4f86-822a-bd374ecb792a','controller','ovs','normal','','{\"port_filter\": true, \"ovs_hybrid_plug\": true}');
/*!40000 ALTER TABLE `ml2_port_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_ucsm_port_profiles`
--

DROP TABLE IF EXISTS `ml2_ucsm_port_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_ucsm_port_profiles` (
  `vlan_id` int(11) NOT NULL AUTO_INCREMENT,
  `profile_id` varchar(64) NOT NULL,
  `created_on_ucs` tinyint(1) NOT NULL,
  PRIMARY KEY (`vlan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_ucsm_port_profiles`
--

LOCK TABLES `ml2_ucsm_port_profiles` WRITE;
/*!40000 ALTER TABLE `ml2_ucsm_port_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_ucsm_port_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_vlan_allocations`
--

DROP TABLE IF EXISTS `ml2_vlan_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_vlan_allocations` (
  `physical_network` varchar(64) NOT NULL,
  `vlan_id` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL,
  PRIMARY KEY (`physical_network`,`vlan_id`),
  KEY `ix_ml2_vlan_allocations_physical_network_allocated` (`physical_network`,`allocated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_vlan_allocations`
--

LOCK TABLES `ml2_vlan_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_vlan_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `ml2_vlan_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_vxlan_allocations`
--

DROP TABLE IF EXISTS `ml2_vxlan_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_vxlan_allocations` (
  `vxlan_vni` int(11) NOT NULL,
  `allocated` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`vxlan_vni`),
  KEY `ix_ml2_vxlan_allocations_allocated` (`allocated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_vxlan_allocations`
--

LOCK TABLES `ml2_vxlan_allocations` WRITE;
/*!40000 ALTER TABLE `ml2_vxlan_allocations` DISABLE KEYS */;
INSERT INTO `ml2_vxlan_allocations` VALUES (1,0),(3,0),(4,0),(5,0),(6,0),(7,0),(8,0),(9,0),(10,0),(11,0),(12,0),(13,0),(14,0),(15,0),(16,0),(17,0),(18,0),(19,0),(20,0),(21,0),(22,0),(23,0),(24,0),(25,0),(26,0),(27,0),(28,0),(29,0),(30,0),(31,0),(32,0),(33,0),(34,0),(35,0),(36,0),(37,0),(38,0),(39,0),(40,0),(41,0),(42,0),(43,0),(44,0),(45,0),(46,0),(47,0),(48,0),(49,0),(50,0),(51,0),(52,0),(53,0),(54,0),(55,0),(56,0),(57,0),(58,0),(59,0),(60,0),(61,0),(62,0),(63,0),(64,0),(65,0),(66,0),(67,0),(68,0),(69,0),(70,0),(71,0),(72,0),(73,0),(74,0),(75,0),(76,0),(77,0),(78,0),(79,0),(80,0),(82,0),(83,0),(84,0),(85,0),(86,0),(87,0),(88,0),(89,0),(90,0),(91,0),(92,0),(93,0),(94,0),(95,0),(96,0),(97,0),(98,0),(99,0),(100,0),(101,0),(102,0),(103,0),(104,0),(105,0),(106,0),(107,0),(108,0),(109,0),(110,0),(111,0),(112,0),(113,0),(114,0),(115,0),(116,0),(117,0),(118,0),(119,0),(120,0),(121,0),(122,0),(123,0),(124,0),(125,0),(126,0),(127,0),(128,0),(129,0),(130,0),(131,0),(132,0),(133,0),(134,0),(135,0),(136,0),(137,0),(138,0),(139,0),(140,0),(141,0),(142,0),(143,0),(144,0),(145,0),(146,0),(147,0),(148,0),(149,0),(150,0),(151,0),(152,0),(153,0),(154,0),(155,0),(156,0),(157,0),(158,0),(159,0),(160,0),(161,0),(162,0),(163,0),(164,0),(165,0),(166,0),(167,0),(168,0),(169,0),(170,0),(171,0),(172,0),(173,0),(174,0),(175,0),(176,0),(177,0),(178,0),(179,0),(180,0),(181,0),(182,0),(183,0),(184,0),(185,0),(186,0),(187,0),(188,0),(189,0),(190,0),(191,0),(192,0),(193,0),(194,0),(195,0),(196,0),(197,0),(198,0),(199,0),(200,0),(201,0),(202,0),(203,0),(204,0),(205,0),(206,0),(207,0),(208,0),(209,0),(210,0),(211,0),(212,0),(213,0),(214,0),(215,0),(216,0),(217,0),(218,0),(219,0),(220,0),(221,0),(222,0),(223,0),(224,0),(225,0),(226,0),(227,0),(228,0),(229,0),(230,0),(231,0),(232,0),(233,0),(234,0),(235,0),(236,0),(237,0),(238,0),(239,0),(240,0),(241,0),(242,0),(243,0),(244,0),(245,0),(246,0),(247,0),(248,0),(249,0),(250,0),(251,0),(252,0),(253,0),(254,0),(255,0),(256,0),(257,0),(258,0),(259,0),(260,0),(261,0),(262,0),(263,0),(264,0),(265,0),(266,0),(267,0),(268,0),(269,0),(270,0),(271,0),(272,0),(273,0),(274,0),(275,0),(276,0),(277,0),(278,0),(279,0),(280,0),(281,0),(282,0),(283,0),(284,0),(285,0),(286,0),(287,0),(288,0),(289,0),(290,0),(291,0),(292,0),(293,0),(294,0),(295,0),(296,0),(297,0),(298,0),(299,0),(300,0),(301,0),(302,0),(303,0),(304,0),(305,0),(306,0),(307,0),(308,0),(309,0),(310,0),(311,0),(312,0),(313,0),(314,0),(315,0),(316,0),(317,0),(318,0),(319,0),(320,0),(321,0),(322,0),(323,0),(324,0),(325,0),(326,0),(327,0),(328,0),(329,0),(330,0),(331,0),(332,0),(333,0),(334,0),(335,0),(336,0),(337,0),(338,0),(339,0),(340,0),(341,0),(342,0),(343,0),(344,0),(345,0),(346,0),(347,0),(348,0),(349,0),(350,0),(351,0),(352,0),(353,0),(354,0),(355,0),(356,0),(357,0),(358,0),(359,0),(360,0),(361,0),(362,0),(363,0),(364,0),(365,0),(366,0),(367,0),(368,0),(369,0),(370,0),(371,0),(372,0),(373,0),(374,0),(375,0),(376,0),(377,0),(378,0),(379,0),(380,0),(381,0),(382,0),(383,0),(384,0),(385,0),(386,0),(387,0),(388,0),(389,0),(390,0),(391,0),(392,0),(393,0),(394,0),(395,0),(396,0),(397,0),(398,0),(399,0),(400,0),(401,0),(402,0),(403,0),(404,0),(405,0),(406,0),(407,0),(408,0),(409,0),(410,0),(411,0),(412,0),(413,0),(414,0),(415,0),(416,0),(417,0),(418,0),(419,0),(420,0),(421,0),(422,0),(423,0),(424,0),(425,0),(426,0),(427,0),(428,0),(429,0),(430,0),(431,0),(432,0),(433,0),(434,0),(435,0),(436,0),(437,0),(438,0),(439,0),(440,0),(441,0),(442,0),(443,0),(444,0),(445,0),(446,0),(447,0),(448,0),(449,0),(450,0),(451,0),(452,0),(453,0),(454,0),(455,0),(456,0),(457,0),(458,0),(459,0),(460,0),(461,0),(462,0),(463,0),(464,0),(465,0),(466,0),(467,0),(468,0),(469,0),(470,0),(471,0),(472,0),(473,0),(474,0),(475,0),(476,0),(477,0),(478,0),(479,0),(480,0),(481,0),(482,0),(483,0),(484,0),(485,0),(486,0),(487,0),(488,0),(489,0),(490,0),(491,0),(492,0),(493,0),(494,0),(495,0),(496,0),(497,0),(498,0),(499,0),(500,0),(501,0),(502,0),(503,0),(504,0),(505,0),(506,0),(507,0),(508,0),(509,0),(510,0),(511,0),(512,0),(513,0),(514,0),(515,0),(516,0),(517,0),(518,0),(519,0),(520,0),(521,0),(522,0),(523,0),(524,0),(525,0),(526,0),(527,0),(528,0),(529,0),(530,0),(531,0),(532,0),(533,0),(534,0),(535,0),(536,0),(537,0),(538,0),(539,0),(540,0),(541,0),(542,0),(543,0),(544,0),(545,0),(546,0),(547,0),(548,0),(549,0),(550,0),(551,0),(552,0),(553,0),(554,0),(555,0),(556,0),(557,0),(558,0),(559,0),(560,0),(561,0),(562,0),(563,0),(564,0),(565,0),(566,0),(567,0),(568,0),(569,0),(570,0),(571,0),(572,0),(573,0),(574,0),(575,0),(576,0),(577,0),(578,0),(579,0),(580,0),(581,0),(582,0),(583,0),(584,0),(585,0),(586,0),(587,0),(588,0),(589,0),(590,0),(591,0),(592,0),(593,0),(594,0),(595,0),(596,0),(597,0),(598,0),(599,0),(600,0),(601,0),(602,0),(603,0),(604,0),(605,0),(606,0),(607,0),(608,0),(609,0),(610,0),(611,0),(612,0),(613,0),(614,0),(615,0),(616,0),(617,0),(618,0),(619,0),(620,0),(621,0),(622,0),(623,0),(624,0),(625,0),(626,0),(627,0),(628,0),(629,0),(630,0),(631,0),(632,0),(633,0),(634,0),(635,0),(636,0),(637,0),(638,0),(639,0),(640,0),(641,0),(642,0),(643,0),(644,0),(645,0),(646,0),(647,0),(648,0),(649,0),(650,0),(651,0),(652,0),(653,0),(654,0),(655,0),(656,0),(657,0),(658,0),(659,0),(660,0),(661,0),(662,0),(663,0),(664,0),(665,0),(666,0),(667,0),(668,0),(669,0),(670,0),(671,0),(672,0),(673,0),(674,0),(675,0),(676,0),(677,0),(678,0),(679,0),(680,0),(681,0),(682,0),(683,0),(684,0),(685,0),(686,0),(687,0),(688,0),(689,0),(690,0),(691,0),(692,0),(693,0),(694,0),(695,0),(696,0),(697,0),(698,0),(699,0),(700,0),(701,0),(702,0),(703,0),(704,0),(705,0),(706,0),(707,0),(708,0),(709,0),(710,0),(711,0),(712,0),(713,0),(714,0),(715,0),(716,0),(717,0),(718,0),(719,0),(720,0),(721,0),(722,0),(723,0),(724,0),(725,0),(726,0),(727,0),(728,0),(729,0),(730,0),(731,0),(732,0),(733,0),(734,0),(735,0),(736,0),(737,0),(738,0),(739,0),(740,0),(741,0),(742,0),(743,0),(744,0),(745,0),(746,0),(747,0),(748,0),(749,0),(750,0),(751,0),(752,0),(753,0),(754,0),(755,0),(756,0),(757,0),(758,0),(759,0),(760,0),(761,0),(762,0),(763,0),(764,0),(765,0),(766,0),(767,0),(768,0),(769,0),(770,0),(771,0),(772,0),(773,0),(774,0),(775,0),(776,0),(777,0),(778,0),(779,0),(780,0),(781,0),(782,0),(783,0),(784,0),(785,0),(786,0),(787,0),(788,0),(789,0),(790,0),(791,0),(792,0),(793,0),(794,0),(795,0),(796,0),(797,0),(798,0),(799,0),(800,0),(801,0),(802,0),(803,0),(804,0),(805,0),(806,0),(807,0),(808,0),(809,0),(810,0),(811,0),(812,0),(813,0),(814,0),(815,0),(816,0),(817,0),(818,0),(819,0),(820,0),(821,0),(822,0),(823,0),(824,0),(825,0),(826,0),(827,0),(828,0),(829,0),(830,0),(831,0),(832,0),(833,0),(834,0),(835,0),(836,0),(837,0),(838,0),(839,0),(840,0),(841,0),(842,0),(843,0),(844,0),(845,0),(846,0),(847,0),(848,0),(849,0),(850,0),(851,0),(852,0),(853,0),(854,0),(855,0),(856,0),(857,0),(858,0),(859,0),(860,0),(861,0),(862,0),(863,0),(864,0),(865,0),(866,0),(867,0),(868,0),(869,0),(870,0),(871,0),(872,0),(873,0),(874,0),(875,0),(876,0),(877,0),(878,0),(879,0),(880,0),(881,0),(882,0),(883,0),(884,0),(885,0),(886,0),(887,0),(888,0),(889,0),(890,0),(891,0),(892,0),(893,0),(894,0),(895,0),(896,0),(897,0),(898,0),(899,0),(900,0),(901,0),(902,0),(903,0),(904,0),(905,0),(906,0),(907,0),(908,0),(909,0),(910,0),(911,0),(912,0),(913,0),(914,0),(915,0),(916,0),(917,0),(918,0),(919,0),(920,0),(921,0),(922,0),(923,0),(924,0),(925,0),(926,0),(927,0),(928,0),(929,0),(930,0),(931,0),(932,0),(933,0),(934,0),(935,0),(936,0),(937,0),(938,0),(939,0),(940,0),(941,0),(942,0),(943,0),(944,0),(945,0),(946,0),(947,0),(948,0),(949,0),(950,0),(951,0),(952,0),(953,0),(954,0),(955,0),(956,0),(957,0),(958,0),(959,0),(960,0),(961,0),(962,0),(963,0),(964,0),(965,0),(966,0),(967,0),(968,0),(969,0),(970,0),(971,0),(972,0),(973,0),(974,0),(975,0),(976,0),(977,0),(978,0),(979,0),(980,0),(981,0),(982,0),(983,0),(984,0),(985,0),(986,0),(987,0),(988,0),(989,0),(990,0),(991,0),(992,0),(993,0),(994,0),(995,0),(996,0),(997,0),(998,0),(999,0),(1000,0),(2,1),(81,1);
/*!40000 ALTER TABLE `ml2_vxlan_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ml2_vxlan_endpoints`
--

DROP TABLE IF EXISTS `ml2_vxlan_endpoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ml2_vxlan_endpoints` (
  `ip_address` varchar(64) NOT NULL,
  `udp_port` int(11) NOT NULL,
  `host` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ip_address`),
  UNIQUE KEY `unique_ml2_vxlan_endpoints0host` (`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ml2_vxlan_endpoints`
--

LOCK TABLES `ml2_vxlan_endpoints` WRITE;
/*!40000 ALTER TABLE `ml2_vxlan_endpoints` DISABLE KEYS */;
INSERT INTO `ml2_vxlan_endpoints` VALUES ('172.16.0.11',4789,'controller'),('172.16.0.31',4789,'compute1');
/*!40000 ALTER TABLE `ml2_vxlan_endpoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `multi_provider_networks`
--

DROP TABLE IF EXISTS `multi_provider_networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `multi_provider_networks` (
  `network_id` varchar(36) NOT NULL,
  PRIMARY KEY (`network_id`),
  CONSTRAINT `multi_provider_networks_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `multi_provider_networks`
--

LOCK TABLES `multi_provider_networks` WRITE;
/*!40000 ALTER TABLE `multi_provider_networks` DISABLE KEYS */;
/*!40000 ALTER TABLE `multi_provider_networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkconnections`
--

DROP TABLE IF EXISTS `networkconnections`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkconnections` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `network_gateway_id` varchar(36) DEFAULT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  `segmentation_type` enum('flat','vlan') DEFAULT NULL,
  `segmentation_id` int(11) DEFAULT NULL,
  `port_id` varchar(36) NOT NULL,
  PRIMARY KEY (`port_id`),
  UNIQUE KEY `network_gateway_id` (`network_gateway_id`,`segmentation_type`,`segmentation_id`),
  KEY `network_id` (`network_id`),
  KEY `ix_networkconnections_tenant_id` (`tenant_id`),
  CONSTRAINT `networkconnections_ibfk_1` FOREIGN KEY (`network_gateway_id`) REFERENCES `networkgateways` (`id`) ON DELETE CASCADE,
  CONSTRAINT `networkconnections_ibfk_2` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `networkconnections_ibfk_3` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkconnections`
--

LOCK TABLES `networkconnections` WRITE;
/*!40000 ALTER TABLE `networkconnections` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkconnections` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkdhcpagentbindings`
--

DROP TABLE IF EXISTS `networkdhcpagentbindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkdhcpagentbindings` (
  `network_id` varchar(36) NOT NULL,
  `dhcp_agent_id` varchar(36) NOT NULL,
  PRIMARY KEY (`network_id`,`dhcp_agent_id`),
  KEY `dhcp_agent_id` (`dhcp_agent_id`),
  CONSTRAINT `networkdhcpagentbindings_ibfk_1` FOREIGN KEY (`dhcp_agent_id`) REFERENCES `agents` (`id`) ON DELETE CASCADE,
  CONSTRAINT `networkdhcpagentbindings_ibfk_2` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkdhcpagentbindings`
--

LOCK TABLES `networkdhcpagentbindings` WRITE;
/*!40000 ALTER TABLE `networkdhcpagentbindings` DISABLE KEYS */;
INSERT INTO `networkdhcpagentbindings` VALUES ('0267647d-80ff-4e2a-afbd-76ade85fa731','e0e6ed38-5306-4f68-8696-51f68c419465'),('a19a8724-c0e4-4944-a636-f4a166537786','e0e6ed38-5306-4f68-8696-51f68c419465'),('c308c5a6-3682-46e2-b633-6685d7b34b0e','e0e6ed38-5306-4f68-8696-51f68c419465');
/*!40000 ALTER TABLE `networkdhcpagentbindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkdnsdomains`
--

DROP TABLE IF EXISTS `networkdnsdomains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkdnsdomains` (
  `network_id` varchar(36) NOT NULL,
  `dns_domain` varchar(255) NOT NULL,
  PRIMARY KEY (`network_id`),
  KEY `ix_networkdnsdomains_network_id` (`network_id`),
  CONSTRAINT `networkdnsdomains_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkdnsdomains`
--

LOCK TABLES `networkdnsdomains` WRITE;
/*!40000 ALTER TABLE `networkdnsdomains` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkdnsdomains` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkgatewaydevicereferences`
--

DROP TABLE IF EXISTS `networkgatewaydevicereferences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkgatewaydevicereferences` (
  `id` varchar(36) NOT NULL,
  `network_gateway_id` varchar(36) NOT NULL,
  `interface_name` varchar(64) NOT NULL,
  PRIMARY KEY (`id`,`network_gateway_id`,`interface_name`),
  KEY `network_gateway_id` (`network_gateway_id`),
  CONSTRAINT `networkgatewaydevicereferences_ibfk_1` FOREIGN KEY (`network_gateway_id`) REFERENCES `networkgateways` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkgatewaydevicereferences`
--

LOCK TABLES `networkgatewaydevicereferences` WRITE;
/*!40000 ALTER TABLE `networkgatewaydevicereferences` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkgatewaydevicereferences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkgatewaydevices`
--

DROP TABLE IF EXISTS `networkgatewaydevices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkgatewaydevices` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `nsx_id` varchar(36) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `connector_type` varchar(10) DEFAULT NULL,
  `connector_ip` varchar(64) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_networkgatewaydevices_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkgatewaydevices`
--

LOCK TABLES `networkgatewaydevices` WRITE;
/*!40000 ALTER TABLE `networkgatewaydevices` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkgatewaydevices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkgateways`
--

DROP TABLE IF EXISTS `networkgateways`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkgateways` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `tenant_id` varchar(36) DEFAULT NULL,
  `default` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkgateways`
--

LOCK TABLES `networkgateways` WRITE;
/*!40000 ALTER TABLE `networkgateways` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkgateways` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkqueuemappings`
--

DROP TABLE IF EXISTS `networkqueuemappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkqueuemappings` (
  `network_id` varchar(36) NOT NULL,
  `queue_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`network_id`),
  KEY `queue_id` (`queue_id`),
  CONSTRAINT `networkqueuemappings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `networkqueuemappings_ibfk_2` FOREIGN KEY (`queue_id`) REFERENCES `qosqueues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkqueuemappings`
--

LOCK TABLES `networkqueuemappings` WRITE;
/*!40000 ALTER TABLE `networkqueuemappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `networkqueuemappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networkrbacs`
--

DROP TABLE IF EXISTS `networkrbacs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networkrbacs` (
  `id` varchar(36) NOT NULL,
  `object_id` varchar(36) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `target_tenant` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_networkrbacs0tenant_target0object_id0action` (`action`,`object_id`,`target_tenant`),
  KEY `object_id` (`object_id`),
  KEY `ix_networkrbacs_tenant_id` (`tenant_id`),
  CONSTRAINT `networkrbacs_ibfk_1` FOREIGN KEY (`object_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networkrbacs`
--

LOCK TABLES `networkrbacs` WRITE;
/*!40000 ALTER TABLE `networkrbacs` DISABLE KEYS */;
INSERT INTO `networkrbacs` VALUES ('69325bfb-3543-43c9-a162-5788875afc76','0267647d-80ff-4e2a-afbd-76ade85fa731','b66fea8bb53a4b76bcda394c67284c53','*','access_as_shared'),('bab65aa6-509d-4f6b-820c-1f7720e0abfa','0267647d-80ff-4e2a-afbd-76ade85fa731','b66fea8bb53a4b76bcda394c67284c53','*','access_as_external');
/*!40000 ALTER TABLE `networkrbacs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networks`
--

DROP TABLE IF EXISTS `networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networks` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `admin_state_up` tinyint(1) DEFAULT NULL,
  `mtu` int(11) DEFAULT NULL,
  `vlan_transparent` tinyint(1) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  `availability_zone_hints` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_networks0standard_attr_id` (`standard_attr_id`),
  KEY `ix_networks_tenant_id` (`tenant_id`),
  CONSTRAINT `networks_ibfk_1` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networks`
--

LOCK TABLES `networks` WRITE;
/*!40000 ALTER TABLE `networks` DISABLE KEYS */;
INSERT INTO `networks` VALUES ('b66fea8bb53a4b76bcda394c67284c53','0267647d-80ff-4e2a-afbd-76ade85fa731','provider','ACTIVE',1,1500,NULL,20,'[]'),('68bed08176254214900d5d6112eb3284','a19a8724-c0e4-4944-a636-f4a166537786','user1net','ACTIVE',1,1450,NULL,22,'[]'),('52d692239a014c9a9960ce6b64d25dc8','c308c5a6-3682-46e2-b633-6685d7b34b0e','user2net','ACTIVE',1,1450,NULL,24,'[]');
/*!40000 ALTER TABLE `networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `networksecuritybindings`
--

DROP TABLE IF EXISTS `networksecuritybindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `networksecuritybindings` (
  `network_id` varchar(36) NOT NULL,
  `port_security_enabled` tinyint(1) NOT NULL,
  PRIMARY KEY (`network_id`),
  CONSTRAINT `networksecuritybindings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `networksecuritybindings`
--

LOCK TABLES `networksecuritybindings` WRITE;
/*!40000 ALTER TABLE `networksecuritybindings` DISABLE KEYS */;
INSERT INTO `networksecuritybindings` VALUES ('0267647d-80ff-4e2a-afbd-76ade85fa731',1),('a19a8724-c0e4-4944-a636-f4a166537786',1),('c308c5a6-3682-46e2-b633-6685d7b34b0e',1);
/*!40000 ALTER TABLE `networksecuritybindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `neutron_nsx_network_mappings`
--

DROP TABLE IF EXISTS `neutron_nsx_network_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `neutron_nsx_network_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_id` varchar(36) NOT NULL,
  PRIMARY KEY (`neutron_id`,`nsx_id`),
  CONSTRAINT `neutron_nsx_network_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `neutron_nsx_network_mappings`
--

LOCK TABLES `neutron_nsx_network_mappings` WRITE;
/*!40000 ALTER TABLE `neutron_nsx_network_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `neutron_nsx_network_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `neutron_nsx_port_mappings`
--

DROP TABLE IF EXISTS `neutron_nsx_port_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `neutron_nsx_port_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_port_id` varchar(36) NOT NULL,
  `nsx_switch_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`neutron_id`),
  CONSTRAINT `neutron_nsx_port_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `neutron_nsx_port_mappings`
--

LOCK TABLES `neutron_nsx_port_mappings` WRITE;
/*!40000 ALTER TABLE `neutron_nsx_port_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `neutron_nsx_port_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `neutron_nsx_router_mappings`
--

DROP TABLE IF EXISTS `neutron_nsx_router_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `neutron_nsx_router_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`neutron_id`),
  CONSTRAINT `neutron_nsx_router_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `neutron_nsx_router_mappings`
--

LOCK TABLES `neutron_nsx_router_mappings` WRITE;
/*!40000 ALTER TABLE `neutron_nsx_router_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `neutron_nsx_router_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `neutron_nsx_security_group_mappings`
--

DROP TABLE IF EXISTS `neutron_nsx_security_group_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `neutron_nsx_security_group_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_id` varchar(36) NOT NULL,
  PRIMARY KEY (`neutron_id`,`nsx_id`),
  CONSTRAINT `neutron_nsx_security_group_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `securitygroups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `neutron_nsx_security_group_mappings`
--

LOCK TABLES `neutron_nsx_security_group_mappings` WRITE;
/*!40000 ALTER TABLE `neutron_nsx_security_group_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `neutron_nsx_security_group_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nexthops`
--

DROP TABLE IF EXISTS `nexthops`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nexthops` (
  `rule_id` int(11) NOT NULL,
  `nexthop` varchar(64) NOT NULL,
  PRIMARY KEY (`rule_id`,`nexthop`),
  CONSTRAINT `nexthops_ibfk_1` FOREIGN KEY (`rule_id`) REFERENCES `routerrules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nexthops`
--

LOCK TABLES `nexthops` WRITE;
/*!40000 ALTER TABLE `nexthops` DISABLE KEYS */;
/*!40000 ALTER TABLE `nexthops` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_edge_dhcp_static_bindings`
--

DROP TABLE IF EXISTS `nsxv_edge_dhcp_static_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_edge_dhcp_static_bindings` (
  `edge_id` varchar(36) NOT NULL,
  `mac_address` varchar(32) NOT NULL,
  `binding_id` varchar(36) NOT NULL,
  PRIMARY KEY (`edge_id`,`mac_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_edge_dhcp_static_bindings`
--

LOCK TABLES `nsxv_edge_dhcp_static_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_edge_dhcp_static_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_edge_dhcp_static_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_edge_vnic_bindings`
--

DROP TABLE IF EXISTS `nsxv_edge_vnic_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_edge_vnic_bindings` (
  `edge_id` varchar(36) NOT NULL,
  `vnic_index` int(11) NOT NULL AUTO_INCREMENT,
  `tunnel_index` int(11) NOT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`edge_id`,`vnic_index`,`tunnel_index`),
  KEY `idx_autoinc_vnic_index` (`vnic_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_edge_vnic_bindings`
--

LOCK TABLES `nsxv_edge_vnic_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_edge_vnic_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_edge_vnic_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_firewall_rule_bindings`
--

DROP TABLE IF EXISTS `nsxv_firewall_rule_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_firewall_rule_bindings` (
  `rule_id` varchar(36) NOT NULL,
  `edge_id` varchar(36) NOT NULL,
  `rule_vse_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`rule_id`,`edge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_firewall_rule_bindings`
--

LOCK TABLES `nsxv_firewall_rule_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_firewall_rule_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_firewall_rule_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_internal_edges`
--

DROP TABLE IF EXISTS `nsxv_internal_edges`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_internal_edges` (
  `ext_ip_address` varchar(64) NOT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  `purpose` enum('inter_edge_net') DEFAULT NULL,
  PRIMARY KEY (`ext_ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_internal_edges`
--

LOCK TABLES `nsxv_internal_edges` WRITE;
/*!40000 ALTER TABLE `nsxv_internal_edges` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_internal_edges` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_internal_networks`
--

DROP TABLE IF EXISTS `nsxv_internal_networks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_internal_networks` (
  `network_purpose` enum('inter_edge_net') NOT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`network_purpose`),
  KEY `network_id` (`network_id`),
  CONSTRAINT `nsxv_internal_networks_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_internal_networks`
--

LOCK TABLES `nsxv_internal_networks` WRITE;
/*!40000 ALTER TABLE `nsxv_internal_networks` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_internal_networks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_port_index_mappings`
--

DROP TABLE IF EXISTS `nsxv_port_index_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_port_index_mappings` (
  `port_id` varchar(36) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `index` int(11) NOT NULL,
  PRIMARY KEY (`port_id`),
  UNIQUE KEY `device_id` (`device_id`,`index`),
  CONSTRAINT `nsxv_port_index_mappings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_port_index_mappings`
--

LOCK TABLES `nsxv_port_index_mappings` WRITE;
/*!40000 ALTER TABLE `nsxv_port_index_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_port_index_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_port_vnic_mappings`
--

DROP TABLE IF EXISTS `nsxv_port_vnic_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_port_vnic_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_id` varchar(42) NOT NULL,
  PRIMARY KEY (`neutron_id`,`nsx_id`),
  CONSTRAINT `nsxv_port_vnic_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_port_vnic_mappings`
--

LOCK TABLES `nsxv_port_vnic_mappings` WRITE;
/*!40000 ALTER TABLE `nsxv_port_vnic_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_port_vnic_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_router_bindings`
--

DROP TABLE IF EXISTS `nsxv_router_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_router_bindings` (
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `router_id` varchar(36) NOT NULL,
  `edge_id` varchar(36) DEFAULT NULL,
  `lswitch_id` varchar(36) DEFAULT NULL,
  `appliance_size` enum('compact','large','xlarge','quadlarge') DEFAULT NULL,
  `edge_type` enum('service','vdr') DEFAULT NULL,
  PRIMARY KEY (`router_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_router_bindings`
--

LOCK TABLES `nsxv_router_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_router_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_router_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_router_ext_attributes`
--

DROP TABLE IF EXISTS `nsxv_router_ext_attributes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_router_ext_attributes` (
  `router_id` varchar(36) NOT NULL,
  `distributed` tinyint(1) NOT NULL,
  `router_type` enum('shared','exclusive') NOT NULL,
  `service_router` tinyint(1) NOT NULL,
  PRIMARY KEY (`router_id`),
  CONSTRAINT `nsxv_router_ext_attributes_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_router_ext_attributes`
--

LOCK TABLES `nsxv_router_ext_attributes` WRITE;
/*!40000 ALTER TABLE `nsxv_router_ext_attributes` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_router_ext_attributes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_rule_mappings`
--

DROP TABLE IF EXISTS `nsxv_rule_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_rule_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `nsx_rule_id` varchar(36) NOT NULL,
  PRIMARY KEY (`neutron_id`,`nsx_rule_id`),
  CONSTRAINT `nsxv_rule_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `securitygrouprules` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_rule_mappings`
--

LOCK TABLES `nsxv_rule_mappings` WRITE;
/*!40000 ALTER TABLE `nsxv_rule_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_rule_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_security_group_section_mappings`
--

DROP TABLE IF EXISTS `nsxv_security_group_section_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_security_group_section_mappings` (
  `neutron_id` varchar(36) NOT NULL,
  `ip_section_id` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`neutron_id`),
  CONSTRAINT `nsxv_security_group_section_mappings_ibfk_1` FOREIGN KEY (`neutron_id`) REFERENCES `securitygroups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_security_group_section_mappings`
--

LOCK TABLES `nsxv_security_group_section_mappings` WRITE;
/*!40000 ALTER TABLE `nsxv_security_group_section_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_security_group_section_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_spoofguard_policy_network_mappings`
--

DROP TABLE IF EXISTS `nsxv_spoofguard_policy_network_mappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_spoofguard_policy_network_mappings` (
  `network_id` varchar(36) NOT NULL,
  `policy_id` varchar(36) NOT NULL,
  PRIMARY KEY (`network_id`),
  CONSTRAINT `nsxv_spoofguard_policy_network_mappings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_spoofguard_policy_network_mappings`
--

LOCK TABLES `nsxv_spoofguard_policy_network_mappings` WRITE;
/*!40000 ALTER TABLE `nsxv_spoofguard_policy_network_mappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_spoofguard_policy_network_mappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_tz_network_bindings`
--

DROP TABLE IF EXISTS `nsxv_tz_network_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_tz_network_bindings` (
  `network_id` varchar(36) NOT NULL,
  `binding_type` enum('flat','vlan','portgroup') NOT NULL,
  `phy_uuid` varchar(36) NOT NULL,
  `vlan_id` int(11) NOT NULL,
  PRIMARY KEY (`network_id`,`binding_type`,`phy_uuid`,`vlan_id`),
  CONSTRAINT `nsxv_tz_network_bindings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_tz_network_bindings`
--

LOCK TABLES `nsxv_tz_network_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_tz_network_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_tz_network_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nsxv_vdr_dhcp_bindings`
--

DROP TABLE IF EXISTS `nsxv_vdr_dhcp_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nsxv_vdr_dhcp_bindings` (
  `vdr_router_id` varchar(36) NOT NULL,
  `dhcp_router_id` varchar(36) NOT NULL,
  `dhcp_edge_id` varchar(36) NOT NULL,
  PRIMARY KEY (`vdr_router_id`),
  UNIQUE KEY `unique_nsxv_vdr_dhcp_bindings0dhcp_router_id` (`dhcp_router_id`),
  UNIQUE KEY `unique_nsxv_vdr_dhcp_bindings0dhcp_edge_id` (`dhcp_edge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nsxv_vdr_dhcp_bindings`
--

LOCK TABLES `nsxv_vdr_dhcp_bindings` WRITE;
/*!40000 ALTER TABLE `nsxv_vdr_dhcp_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nsxv_vdr_dhcp_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nuage_net_partition_router_mapping`
--

DROP TABLE IF EXISTS `nuage_net_partition_router_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nuage_net_partition_router_mapping` (
  `net_partition_id` varchar(36) NOT NULL,
  `router_id` varchar(36) NOT NULL,
  `nuage_router_id` varchar(36) DEFAULT NULL,
  `nuage_rtr_rd` varchar(36) DEFAULT NULL,
  `nuage_rtr_rt` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`net_partition_id`,`router_id`),
  UNIQUE KEY `nuage_router_id` (`nuage_router_id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `nuage_net_partition_router_mapping_ibfk_1` FOREIGN KEY (`net_partition_id`) REFERENCES `nuage_net_partitions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `nuage_net_partition_router_mapping_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nuage_net_partition_router_mapping`
--

LOCK TABLES `nuage_net_partition_router_mapping` WRITE;
/*!40000 ALTER TABLE `nuage_net_partition_router_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `nuage_net_partition_router_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nuage_net_partitions`
--

DROP TABLE IF EXISTS `nuage_net_partitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nuage_net_partitions` (
  `id` varchar(36) NOT NULL,
  `name` varchar(64) DEFAULT NULL,
  `l3dom_tmplt_id` varchar(36) DEFAULT NULL,
  `l2dom_tmplt_id` varchar(36) DEFAULT NULL,
  `isolated_zone` varchar(64) DEFAULT NULL,
  `shared_zone` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nuage_net_partitions`
--

LOCK TABLES `nuage_net_partitions` WRITE;
/*!40000 ALTER TABLE `nuage_net_partitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `nuage_net_partitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nuage_provider_net_bindings`
--

DROP TABLE IF EXISTS `nuage_provider_net_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nuage_provider_net_bindings` (
  `network_id` varchar(36) NOT NULL,
  `network_type` varchar(32) NOT NULL,
  `physical_network` varchar(64) NOT NULL,
  `vlan_id` int(11) NOT NULL,
  PRIMARY KEY (`network_id`),
  CONSTRAINT `nuage_provider_net_bindings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nuage_provider_net_bindings`
--

LOCK TABLES `nuage_provider_net_bindings` WRITE;
/*!40000 ALTER TABLE `nuage_provider_net_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `nuage_provider_net_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `nuage_subnet_l2dom_mapping`
--

DROP TABLE IF EXISTS `nuage_subnet_l2dom_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `nuage_subnet_l2dom_mapping` (
  `subnet_id` varchar(36) NOT NULL,
  `net_partition_id` varchar(36) DEFAULT NULL,
  `nuage_subnet_id` varchar(36) DEFAULT NULL,
  `nuage_l2dom_tmplt_id` varchar(36) DEFAULT NULL,
  `nuage_user_id` varchar(36) DEFAULT NULL,
  `nuage_group_id` varchar(36) DEFAULT NULL,
  `nuage_managed_subnet` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`subnet_id`),
  UNIQUE KEY `nuage_subnet_id` (`nuage_subnet_id`),
  KEY `net_partition_id` (`net_partition_id`),
  CONSTRAINT `nuage_subnet_l2dom_mapping_ibfk_1` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`) ON DELETE CASCADE,
  CONSTRAINT `nuage_subnet_l2dom_mapping_ibfk_2` FOREIGN KEY (`net_partition_id`) REFERENCES `nuage_net_partitions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `nuage_subnet_l2dom_mapping`
--

LOCK TABLES `nuage_subnet_l2dom_mapping` WRITE;
/*!40000 ALTER TABLE `nuage_subnet_l2dom_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `nuage_subnet_l2dom_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `poolloadbalanceragentbindings`
--

DROP TABLE IF EXISTS `poolloadbalanceragentbindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `poolloadbalanceragentbindings` (
  `pool_id` varchar(36) NOT NULL,
  `agent_id` varchar(36) NOT NULL,
  PRIMARY KEY (`pool_id`),
  KEY `agent_id` (`agent_id`),
  CONSTRAINT `poolloadbalanceragentbindings_ibfk_1` FOREIGN KEY (`pool_id`) REFERENCES `pools` (`id`) ON DELETE CASCADE,
  CONSTRAINT `poolloadbalanceragentbindings_ibfk_2` FOREIGN KEY (`agent_id`) REFERENCES `agents` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `poolloadbalanceragentbindings`
--

LOCK TABLES `poolloadbalanceragentbindings` WRITE;
/*!40000 ALTER TABLE `poolloadbalanceragentbindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `poolloadbalanceragentbindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `poolmonitorassociations`
--

DROP TABLE IF EXISTS `poolmonitorassociations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `poolmonitorassociations` (
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `pool_id` varchar(36) NOT NULL,
  `monitor_id` varchar(36) NOT NULL,
  PRIMARY KEY (`pool_id`,`monitor_id`),
  KEY `monitor_id` (`monitor_id`),
  CONSTRAINT `poolmonitorassociations_ibfk_1` FOREIGN KEY (`pool_id`) REFERENCES `pools` (`id`),
  CONSTRAINT `poolmonitorassociations_ibfk_2` FOREIGN KEY (`monitor_id`) REFERENCES `healthmonitors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `poolmonitorassociations`
--

LOCK TABLES `poolmonitorassociations` WRITE;
/*!40000 ALTER TABLE `poolmonitorassociations` DISABLE KEYS */;
/*!40000 ALTER TABLE `poolmonitorassociations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pools`
--

DROP TABLE IF EXISTS `pools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `pools` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `vip_id` varchar(36) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `subnet_id` varchar(36) NOT NULL,
  `protocol` enum('HTTP','HTTPS','TCP') NOT NULL,
  `lb_method` enum('ROUND_ROBIN','LEAST_CONNECTIONS','SOURCE_IP') NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `vip_id` (`vip_id`),
  CONSTRAINT `pools_ibfk_1` FOREIGN KEY (`vip_id`) REFERENCES `vips` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pools`
--

LOCK TABLES `pools` WRITE;
/*!40000 ALTER TABLE `pools` DISABLE KEYS */;
/*!40000 ALTER TABLE `pools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `poolstatisticss`
--

DROP TABLE IF EXISTS `poolstatisticss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `poolstatisticss` (
  `pool_id` varchar(36) NOT NULL,
  `bytes_in` bigint(20) NOT NULL,
  `bytes_out` bigint(20) NOT NULL,
  `active_connections` bigint(20) NOT NULL,
  `total_connections` bigint(20) NOT NULL,
  PRIMARY KEY (`pool_id`),
  CONSTRAINT `poolstatisticss_ibfk_1` FOREIGN KEY (`pool_id`) REFERENCES `pools` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `poolstatisticss`
--

LOCK TABLES `poolstatisticss` WRITE;
/*!40000 ALTER TABLE `poolstatisticss` DISABLE KEYS */;
/*!40000 ALTER TABLE `poolstatisticss` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portbindingports`
--

DROP TABLE IF EXISTS `portbindingports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `portbindingports` (
  `port_id` varchar(36) NOT NULL,
  `host` varchar(255) NOT NULL,
  PRIMARY KEY (`port_id`),
  CONSTRAINT `portbindingports_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portbindingports`
--

LOCK TABLES `portbindingports` WRITE;
/*!40000 ALTER TABLE `portbindingports` DISABLE KEYS */;
/*!40000 ALTER TABLE `portbindingports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portdnses`
--

DROP TABLE IF EXISTS `portdnses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `portdnses` (
  `port_id` varchar(36) NOT NULL,
  `current_dns_name` varchar(255) NOT NULL,
  `current_dns_domain` varchar(255) NOT NULL,
  `previous_dns_name` varchar(255) NOT NULL,
  `previous_dns_domain` varchar(255) NOT NULL,
  PRIMARY KEY (`port_id`),
  KEY `ix_portdnses_port_id` (`port_id`),
  CONSTRAINT `portdnses_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portdnses`
--

LOCK TABLES `portdnses` WRITE;
/*!40000 ALTER TABLE `portdnses` DISABLE KEYS */;
/*!40000 ALTER TABLE `portdnses` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portqueuemappings`
--

DROP TABLE IF EXISTS `portqueuemappings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `portqueuemappings` (
  `port_id` varchar(36) NOT NULL,
  `queue_id` varchar(36) NOT NULL,
  PRIMARY KEY (`port_id`,`queue_id`),
  KEY `queue_id` (`queue_id`),
  CONSTRAINT `portqueuemappings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `portqueuemappings_ibfk_2` FOREIGN KEY (`queue_id`) REFERENCES `qosqueues` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portqueuemappings`
--

LOCK TABLES `portqueuemappings` WRITE;
/*!40000 ALTER TABLE `portqueuemappings` DISABLE KEYS */;
/*!40000 ALTER TABLE `portqueuemappings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ports`
--

DROP TABLE IF EXISTS `ports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ports` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `network_id` varchar(36) NOT NULL,
  `mac_address` varchar(32) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `status` varchar(16) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `device_owner` varchar(255) NOT NULL,
  `dns_name` varchar(255) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_ports0network_id0mac_address` (`network_id`,`mac_address`),
  UNIQUE KEY `uniq_ports0standard_attr_id` (`standard_attr_id`),
  KEY `ix_ports_tenant_id` (`tenant_id`),
  KEY `ix_ports_network_id_device_owner` (`network_id`,`device_owner`),
  KEY `ix_ports_network_id_mac_address` (`network_id`,`mac_address`),
  CONSTRAINT `ports_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`),
  CONSTRAINT `ports_ibfk_2` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ports`
--

LOCK TABLES `ports` WRITE;
/*!40000 ALTER TABLE `ports` DISABLE KEYS */;
INSERT INTO `ports` VALUES ('','09d66f0a-4686-412b-9cf8-7874a1d0ef30','','0267647d-80ff-4e2a-afbd-76ade85fa731','fa:16:3e:9c:99:77',1,'ACTIVE','b77acd74-91f9-49d5-b478-123726ec4cd8','network:router_gateway',NULL,28),('52d692239a014c9a9960ce6b64d25dc8','1bffddfb-0afe-4d78-b421-b88d9d8e921b','','c308c5a6-3682-46e2-b633-6685d7b34b0e','fa:16:3e:e0:71:bd',1,'ACTIVE','dhcpd3377d3c-a0d1-5d71-9947-f17125c357bb-c308c5a6-3682-46e2-b633-6685d7b34b0e','network:dhcp',NULL,34),('','465ef707-f517-43ab-b2b8-eeb68f9d3eeb','','0267647d-80ff-4e2a-afbd-76ade85fa731','fa:16:3e:27:6d:64',1,'N/A','c3658951-4d15-4335-ba60-4efc45524867','network:floatingip',NULL,41),('b66fea8bb53a4b76bcda394c67284c53','5589876b-8c66-4bfb-a465-c6509a277972','','0267647d-80ff-4e2a-afbd-76ade85fa731','fa:16:3e:0b:3a:1f',1,'ACTIVE','dhcpd3377d3c-a0d1-5d71-9947-f17125c357bb-0267647d-80ff-4e2a-afbd-76ade85fa731','network:dhcp',NULL,32),('','5a347f2e-bcec-4dd8-b09d-318601894fc2','','0267647d-80ff-4e2a-afbd-76ade85fa731','fa:16:3e:e3:21:21',1,'ACTIVE','9a23817d-2d25-4edc-b328-ba1057e308f1','network:router_gateway',NULL,31),('68bed08176254214900d5d6112eb3284','6a98a347-8752-4f71-b485-257f6f1a2d04','','a19a8724-c0e4-4944-a636-f4a166537786','fa:16:3e:26:f6:b8',1,'ACTIVE','b77acd74-91f9-49d5-b478-123726ec4cd8','network:router_interface',NULL,27),('68bed08176254214900d5d6112eb3284','737565c0-2c6d-4bab-9985-8f3a1c069881','','a19a8724-c0e4-4944-a636-f4a166537786','fa:16:3e:42:5d:ed',1,'ACTIVE','7b7d261d-2eb6-4dc2-8353-958e4f10ea59','compute:None',NULL,35),('68bed08176254214900d5d6112eb3284','a61e24db-b68a-4df2-8af5-17a3295a0b8c','','a19a8724-c0e4-4944-a636-f4a166537786','fa:16:3e:56:c6:4e',1,'ACTIVE','dhcpd3377d3c-a0d1-5d71-9947-f17125c357bb-a19a8724-c0e4-4944-a636-f4a166537786','network:dhcp',NULL,33),('52d692239a014c9a9960ce6b64d25dc8','fb358de8-c4c7-4f86-822a-bd374ecb792a','','c308c5a6-3682-46e2-b633-6685d7b34b0e','fa:16:3e:51:f5:ee',1,'ACTIVE','9a23817d-2d25-4edc-b328-ba1057e308f1','network:router_interface',NULL,30);
/*!40000 ALTER TABLE `ports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `portsecuritybindings`
--

DROP TABLE IF EXISTS `portsecuritybindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `portsecuritybindings` (
  `port_id` varchar(36) NOT NULL,
  `port_security_enabled` tinyint(1) NOT NULL,
  PRIMARY KEY (`port_id`),
  CONSTRAINT `portsecuritybindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `portsecuritybindings`
--

LOCK TABLES `portsecuritybindings` WRITE;
/*!40000 ALTER TABLE `portsecuritybindings` DISABLE KEYS */;
INSERT INTO `portsecuritybindings` VALUES ('09d66f0a-4686-412b-9cf8-7874a1d0ef30',0),('1bffddfb-0afe-4d78-b421-b88d9d8e921b',0),('465ef707-f517-43ab-b2b8-eeb68f9d3eeb',0),('5589876b-8c66-4bfb-a465-c6509a277972',0),('5a347f2e-bcec-4dd8-b09d-318601894fc2',0),('6a98a347-8752-4f71-b485-257f6f1a2d04',0),('737565c0-2c6d-4bab-9985-8f3a1c069881',1),('a61e24db-b68a-4df2-8af5-17a3295a0b8c',0),('fb358de8-c4c7-4f86-822a-bd374ecb792a',0);
/*!40000 ALTER TABLE `portsecuritybindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `providerresourceassociations`
--

DROP TABLE IF EXISTS `providerresourceassociations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `providerresourceassociations` (
  `provider_name` varchar(255) NOT NULL,
  `resource_id` varchar(36) NOT NULL,
  PRIMARY KEY (`provider_name`,`resource_id`),
  UNIQUE KEY `resource_id` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `providerresourceassociations`
--

LOCK TABLES `providerresourceassociations` WRITE;
/*!40000 ALTER TABLE `providerresourceassociations` DISABLE KEYS */;
/*!40000 ALTER TABLE `providerresourceassociations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qos_bandwidth_limit_rules`
--

DROP TABLE IF EXISTS `qos_bandwidth_limit_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qos_bandwidth_limit_rules` (
  `id` varchar(36) NOT NULL,
  `qos_policy_id` varchar(36) NOT NULL,
  `max_kbps` int(11) DEFAULT NULL,
  `max_burst_kbps` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `qos_policy_id` (`qos_policy_id`),
  CONSTRAINT `qos_bandwidth_limit_rules_ibfk_1` FOREIGN KEY (`qos_policy_id`) REFERENCES `qos_policies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qos_bandwidth_limit_rules`
--

LOCK TABLES `qos_bandwidth_limit_rules` WRITE;
/*!40000 ALTER TABLE `qos_bandwidth_limit_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `qos_bandwidth_limit_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qos_network_policy_bindings`
--

DROP TABLE IF EXISTS `qos_network_policy_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qos_network_policy_bindings` (
  `policy_id` varchar(36) NOT NULL,
  `network_id` varchar(36) NOT NULL,
  UNIQUE KEY `network_id` (`network_id`),
  KEY `policy_id` (`policy_id`),
  CONSTRAINT `qos_network_policy_bindings_ibfk_1` FOREIGN KEY (`policy_id`) REFERENCES `qos_policies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `qos_network_policy_bindings_ibfk_2` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qos_network_policy_bindings`
--

LOCK TABLES `qos_network_policy_bindings` WRITE;
/*!40000 ALTER TABLE `qos_network_policy_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `qos_network_policy_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qos_policies`
--

DROP TABLE IF EXISTS `qos_policies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qos_policies` (
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_qos_policies_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qos_policies`
--

LOCK TABLES `qos_policies` WRITE;
/*!40000 ALTER TABLE `qos_policies` DISABLE KEYS */;
/*!40000 ALTER TABLE `qos_policies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qos_port_policy_bindings`
--

DROP TABLE IF EXISTS `qos_port_policy_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qos_port_policy_bindings` (
  `policy_id` varchar(36) NOT NULL,
  `port_id` varchar(36) NOT NULL,
  UNIQUE KEY `port_id` (`port_id`),
  KEY `policy_id` (`policy_id`),
  CONSTRAINT `qos_port_policy_bindings_ibfk_1` FOREIGN KEY (`policy_id`) REFERENCES `qos_policies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `qos_port_policy_bindings_ibfk_2` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qos_port_policy_bindings`
--

LOCK TABLES `qos_port_policy_bindings` WRITE;
/*!40000 ALTER TABLE `qos_port_policy_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `qos_port_policy_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qospolicyrbacs`
--

DROP TABLE IF EXISTS `qospolicyrbacs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qospolicyrbacs` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `target_tenant` varchar(255) NOT NULL,
  `action` varchar(255) NOT NULL,
  `object_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `target_tenant` (`target_tenant`,`object_id`,`action`),
  KEY `object_id` (`object_id`),
  KEY `ix_qospolicyrbacs_tenant_id` (`tenant_id`),
  CONSTRAINT `qospolicyrbacs_ibfk_1` FOREIGN KEY (`object_id`) REFERENCES `qos_policies` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qospolicyrbacs`
--

LOCK TABLES `qospolicyrbacs` WRITE;
/*!40000 ALTER TABLE `qospolicyrbacs` DISABLE KEYS */;
/*!40000 ALTER TABLE `qospolicyrbacs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qosqueues`
--

DROP TABLE IF EXISTS `qosqueues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qosqueues` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `default` tinyint(1) DEFAULT '0',
  `min` int(11) NOT NULL,
  `max` int(11) DEFAULT NULL,
  `qos_marking` enum('untrusted','trusted') DEFAULT NULL,
  `dscp` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_qosqueues_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qosqueues`
--

LOCK TABLES `qosqueues` WRITE;
/*!40000 ALTER TABLE `qosqueues` DISABLE KEYS */;
/*!40000 ALTER TABLE `qosqueues` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotas`
--

DROP TABLE IF EXISTS `quotas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quotas` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `limit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_quotas_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotas`
--

LOCK TABLES `quotas` WRITE;
/*!40000 ALTER TABLE `quotas` DISABLE KEYS */;
/*!40000 ALTER TABLE `quotas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `quotausages`
--

DROP TABLE IF EXISTS `quotausages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quotausages` (
  `tenant_id` varchar(255) NOT NULL,
  `resource` varchar(255) NOT NULL,
  `dirty` tinyint(1) NOT NULL DEFAULT '0',
  `in_use` int(11) NOT NULL DEFAULT '0',
  `reserved` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`tenant_id`,`resource`),
  KEY `ix_quotausages_tenant_id` (`tenant_id`),
  KEY `ix_quotausages_resource` (`resource`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `quotausages`
--

LOCK TABLES `quotausages` WRITE;
/*!40000 ALTER TABLE `quotausages` DISABLE KEYS */;
INSERT INTO `quotausages` VALUES ('52d692239a014c9a9960ce6b64d25dc8','network',0,1,0),('52d692239a014c9a9960ce6b64d25dc8','router',0,1,0),('52d692239a014c9a9960ce6b64d25dc8','security_group',0,1,0),('52d692239a014c9a9960ce6b64d25dc8','subnet',0,1,0),('68bed08176254214900d5d6112eb3284','floatingip',0,1,0),('68bed08176254214900d5d6112eb3284','network',0,1,0),('68bed08176254214900d5d6112eb3284','port',0,3,0),('68bed08176254214900d5d6112eb3284','router',0,1,0),('68bed08176254214900d5d6112eb3284','security_group',0,1,0),('b66fea8bb53a4b76bcda394c67284c53','network',0,1,0),('b66fea8bb53a4b76bcda394c67284c53','subnet',0,1,0);
/*!40000 ALTER TABLE `quotausages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reservations`
--

DROP TABLE IF EXISTS `reservations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reservations` (
  `id` varchar(36) NOT NULL,
  `tenant_id` varchar(255) DEFAULT NULL,
  `expiration` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reservations`
--

LOCK TABLES `reservations` WRITE;
/*!40000 ALTER TABLE `reservations` DISABLE KEYS */;
/*!40000 ALTER TABLE `reservations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resourcedeltas`
--

DROP TABLE IF EXISTS `resourcedeltas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `resourcedeltas` (
  `resource` varchar(255) NOT NULL,
  `reservation_id` varchar(36) NOT NULL,
  `amount` int(11) DEFAULT NULL,
  PRIMARY KEY (`resource`,`reservation_id`),
  KEY `reservation_id` (`reservation_id`),
  CONSTRAINT `resourcedeltas_ibfk_1` FOREIGN KEY (`reservation_id`) REFERENCES `reservations` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resourcedeltas`
--

LOCK TABLES `resourcedeltas` WRITE;
/*!40000 ALTER TABLE `resourcedeltas` DISABLE KEYS */;
/*!40000 ALTER TABLE `resourcedeltas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `router_extra_attributes`
--

DROP TABLE IF EXISTS `router_extra_attributes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `router_extra_attributes` (
  `router_id` varchar(36) NOT NULL,
  `distributed` tinyint(1) NOT NULL DEFAULT '0',
  `service_router` tinyint(1) NOT NULL DEFAULT '0',
  `ha` tinyint(1) NOT NULL DEFAULT '0',
  `ha_vr_id` int(11) DEFAULT NULL,
  `availability_zone_hints` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`router_id`),
  CONSTRAINT `router_extra_attributes_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `router_extra_attributes`
--

LOCK TABLES `router_extra_attributes` WRITE;
/*!40000 ALTER TABLE `router_extra_attributes` DISABLE KEYS */;
INSERT INTO `router_extra_attributes` VALUES ('9a23817d-2d25-4edc-b328-ba1057e308f1',0,0,0,0,'[]'),('b77acd74-91f9-49d5-b478-123726ec4cd8',0,0,0,0,'[]');
/*!40000 ALTER TABLE `router_extra_attributes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `routerl3agentbindings`
--

DROP TABLE IF EXISTS `routerl3agentbindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `routerl3agentbindings` (
  `router_id` varchar(36) NOT NULL,
  `l3_agent_id` varchar(36) NOT NULL,
  PRIMARY KEY (`router_id`,`l3_agent_id`),
  KEY `l3_agent_id` (`l3_agent_id`),
  CONSTRAINT `routerl3agentbindings_ibfk_1` FOREIGN KEY (`l3_agent_id`) REFERENCES `agents` (`id`) ON DELETE CASCADE,
  CONSTRAINT `routerl3agentbindings_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `routerl3agentbindings`
--

LOCK TABLES `routerl3agentbindings` WRITE;
/*!40000 ALTER TABLE `routerl3agentbindings` DISABLE KEYS */;
INSERT INTO `routerl3agentbindings` VALUES ('9a23817d-2d25-4edc-b328-ba1057e308f1','a01c0dc3-8be6-4722-87f5-e2d77edaf33f'),('b77acd74-91f9-49d5-b478-123726ec4cd8','a01c0dc3-8be6-4722-87f5-e2d77edaf33f');
/*!40000 ALTER TABLE `routerl3agentbindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `routerports`
--

DROP TABLE IF EXISTS `routerports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `routerports` (
  `router_id` varchar(36) NOT NULL,
  `port_id` varchar(36) NOT NULL,
  `port_type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`router_id`,`port_id`),
  KEY `port_id` (`port_id`),
  CONSTRAINT `routerports_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `routerports_ibfk_2` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `routerports`
--

LOCK TABLES `routerports` WRITE;
/*!40000 ALTER TABLE `routerports` DISABLE KEYS */;
INSERT INTO `routerports` VALUES ('9a23817d-2d25-4edc-b328-ba1057e308f1','5a347f2e-bcec-4dd8-b09d-318601894fc2','network:router_gateway'),('9a23817d-2d25-4edc-b328-ba1057e308f1','fb358de8-c4c7-4f86-822a-bd374ecb792a','network:router_interface'),('b77acd74-91f9-49d5-b478-123726ec4cd8','09d66f0a-4686-412b-9cf8-7874a1d0ef30','network:router_gateway'),('b77acd74-91f9-49d5-b478-123726ec4cd8','6a98a347-8752-4f71-b485-257f6f1a2d04','network:router_interface');
/*!40000 ALTER TABLE `routerports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `routerroutes`
--

DROP TABLE IF EXISTS `routerroutes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `routerroutes` (
  `destination` varchar(64) NOT NULL,
  `nexthop` varchar(64) NOT NULL,
  `router_id` varchar(36) NOT NULL,
  PRIMARY KEY (`destination`,`nexthop`,`router_id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `routerroutes_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `routerroutes`
--

LOCK TABLES `routerroutes` WRITE;
/*!40000 ALTER TABLE `routerroutes` DISABLE KEYS */;
/*!40000 ALTER TABLE `routerroutes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `routerrules`
--

DROP TABLE IF EXISTS `routerrules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `routerrules` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `source` varchar(64) NOT NULL,
  `destination` varchar(64) NOT NULL,
  `action` varchar(10) NOT NULL,
  `router_id` varchar(36) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `routerrules_ibfk_1` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `routerrules`
--

LOCK TABLES `routerrules` WRITE;
/*!40000 ALTER TABLE `routerrules` DISABLE KEYS */;
/*!40000 ALTER TABLE `routerrules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `routers`
--

DROP TABLE IF EXISTS `routers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `routers` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `admin_state_up` tinyint(1) DEFAULT NULL,
  `gw_port_id` varchar(36) DEFAULT NULL,
  `enable_snat` tinyint(1) NOT NULL DEFAULT '1',
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_routers0standard_attr_id` (`standard_attr_id`),
  KEY `gw_port_id` (`gw_port_id`),
  KEY `ix_routers_tenant_id` (`tenant_id`),
  CONSTRAINT `routers_ibfk_1` FOREIGN KEY (`gw_port_id`) REFERENCES `ports` (`id`),
  CONSTRAINT `routers_ibfk_2` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `routers`
--

LOCK TABLES `routers` WRITE;
/*!40000 ALTER TABLE `routers` DISABLE KEYS */;
INSERT INTO `routers` VALUES ('52d692239a014c9a9960ce6b64d25dc8','9a23817d-2d25-4edc-b328-ba1057e308f1','router','ACTIVE',1,'5a347f2e-bcec-4dd8-b09d-318601894fc2',1,29),('68bed08176254214900d5d6112eb3284','b77acd74-91f9-49d5-b478-123726ec4cd8','router','ACTIVE',1,'09d66f0a-4686-412b-9cf8-7874a1d0ef30',1,26);
/*!40000 ALTER TABLE `routers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `securitygroupportbindings`
--

DROP TABLE IF EXISTS `securitygroupportbindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `securitygroupportbindings` (
  `port_id` varchar(36) NOT NULL,
  `security_group_id` varchar(36) NOT NULL,
  PRIMARY KEY (`port_id`,`security_group_id`),
  KEY `security_group_id` (`security_group_id`),
  CONSTRAINT `securitygroupportbindings_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`) ON DELETE CASCADE,
  CONSTRAINT `securitygroupportbindings_ibfk_2` FOREIGN KEY (`security_group_id`) REFERENCES `securitygroups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `securitygroupportbindings`
--

LOCK TABLES `securitygroupportbindings` WRITE;
/*!40000 ALTER TABLE `securitygroupportbindings` DISABLE KEYS */;
INSERT INTO `securitygroupportbindings` VALUES ('737565c0-2c6d-4bab-9985-8f3a1c069881','1fd2dfa5-6205-4894-abcb-44850f8c9600');
/*!40000 ALTER TABLE `securitygroupportbindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `securitygrouprules`
--

DROP TABLE IF EXISTS `securitygrouprules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `securitygrouprules` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `security_group_id` varchar(36) NOT NULL,
  `remote_group_id` varchar(36) DEFAULT NULL,
  `direction` enum('ingress','egress') DEFAULT NULL,
  `ethertype` varchar(40) DEFAULT NULL,
  `protocol` varchar(40) DEFAULT NULL,
  `port_range_min` int(11) DEFAULT NULL,
  `port_range_max` int(11) DEFAULT NULL,
  `remote_ip_prefix` varchar(255) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_securitygrouprules0standard_attr_id` (`standard_attr_id`),
  KEY `security_group_id` (`security_group_id`),
  KEY `remote_group_id` (`remote_group_id`),
  KEY `ix_securitygrouprules_tenant_id` (`tenant_id`),
  CONSTRAINT `securitygrouprules_ibfk_1` FOREIGN KEY (`security_group_id`) REFERENCES `securitygroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `securitygrouprules_ibfk_2` FOREIGN KEY (`remote_group_id`) REFERENCES `securitygroups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `securitygrouprules_ibfk_3` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `securitygrouprules`
--

LOCK TABLES `securitygrouprules` WRITE;
/*!40000 ALTER TABLE `securitygrouprules` DISABLE KEYS */;
INSERT INTO `securitygrouprules` VALUES ('b66fea8bb53a4b76bcda394c67284c53','0e904e37-b6cb-48c2-a2cc-c12336d9b56c','08e1ed01-9c9a-448d-b465-87c99ca43d20',NULL,'egress','IPv6',NULL,NULL,NULL,NULL,19),('68bed08176254214900d5d6112eb3284','1a28a338-b04d-42d1-bcfd-9f80df0c44dd','1fd2dfa5-6205-4894-abcb-44850f8c9600','1fd2dfa5-6205-4894-abcb-44850f8c9600','ingress','IPv6',NULL,NULL,NULL,NULL,11),('68bed08176254214900d5d6112eb3284','307397e5-6152-4386-8ac9-7136537e13dd','1fd2dfa5-6205-4894-abcb-44850f8c9600',NULL,'ingress','IPv4','icmp',NULL,NULL,'0.0.0.0/0',14),('52d692239a014c9a9960ce6b64d25dc8','46458bc3-9bd2-41d2-ad57-cf28896d0e17','eee365de-cd10-4680-b1b9-29c4d35f1829','eee365de-cd10-4680-b1b9-29c4d35f1829','ingress','IPv6',NULL,NULL,NULL,NULL,4),('b66fea8bb53a4b76bcda394c67284c53','51c9a3be-c2bf-437e-866a-efc9d76b50cd','08e1ed01-9c9a-448d-b465-87c99ca43d20',NULL,'egress','IPv4',NULL,NULL,NULL,NULL,17),('52d692239a014c9a9960ce6b64d25dc8','5cf80705-5cad-4af0-881c-ce62e3a8aee1','eee365de-cd10-4680-b1b9-29c4d35f1829',NULL,'ingress','IPv4','icmp',NULL,NULL,'0.0.0.0/0',6),('52d692239a014c9a9960ce6b64d25dc8','5ddde4e2-1a61-4073-aa90-c7ab51c44882','eee365de-cd10-4680-b1b9-29c4d35f1829',NULL,'ingress','IPv4','tcp',22,22,'0.0.0.0/0',7),('68bed08176254214900d5d6112eb3284','5dedfaea-5f94-4fe2-be4a-cf74ad270a3f','1fd2dfa5-6205-4894-abcb-44850f8c9600','1fd2dfa5-6205-4894-abcb-44850f8c9600','ingress','IPv4',NULL,NULL,NULL,NULL,9),('b66fea8bb53a4b76bcda394c67284c53','605f0bb5-3c11-46da-a45f-95f09d34c598','08e1ed01-9c9a-448d-b465-87c99ca43d20','08e1ed01-9c9a-448d-b465-87c99ca43d20','ingress','IPv4',NULL,NULL,NULL,NULL,16),('391750905a7f491d85932ad58e809362','6e9a0829-bd87-44fb-954c-6692979cdb4b','869cf42b-1d82-40e2-a975-adde0ce3e689','869cf42b-1d82-40e2-a975-adde0ce3e689','ingress','IPv6',NULL,NULL,NULL,NULL,39),('391750905a7f491d85932ad58e809362','6f55705a-c468-4088-884d-cc597a94d5be','869cf42b-1d82-40e2-a975-adde0ce3e689',NULL,'egress','IPv4',NULL,NULL,NULL,NULL,38),('52d692239a014c9a9960ce6b64d25dc8','74a236af-f83f-4db4-a304-623924104a6e','eee365de-cd10-4680-b1b9-29c4d35f1829',NULL,'egress','IPv6',NULL,NULL,NULL,NULL,5),('68bed08176254214900d5d6112eb3284','7948a11e-315a-4487-a51f-e87539d00ee4','1fd2dfa5-6205-4894-abcb-44850f8c9600',NULL,'ingress','IPv4','tcp',22,22,'0.0.0.0/0',13),('391750905a7f491d85932ad58e809362','798e615b-68a1-47cb-a7b0-307769d61880','869cf42b-1d82-40e2-a975-adde0ce3e689','869cf42b-1d82-40e2-a975-adde0ce3e689','ingress','IPv4',NULL,NULL,NULL,NULL,37),('b66fea8bb53a4b76bcda394c67284c53','82a58d30-92e2-4b49-bd27-46527904d548','08e1ed01-9c9a-448d-b465-87c99ca43d20','08e1ed01-9c9a-448d-b465-87c99ca43d20','ingress','IPv6',NULL,NULL,NULL,NULL,18),('52d692239a014c9a9960ce6b64d25dc8','94a56fea-1bba-483f-97dc-ca697532d076','eee365de-cd10-4680-b1b9-29c4d35f1829',NULL,'egress','IPv4',NULL,NULL,NULL,NULL,3),('391750905a7f491d85932ad58e809362','caca9126-6348-4090-b19d-7d39b5e1ed46','869cf42b-1d82-40e2-a975-adde0ce3e689',NULL,'egress','IPv6',NULL,NULL,NULL,NULL,40),('52d692239a014c9a9960ce6b64d25dc8','e2c33fc1-1854-48cc-9949-6d3ec652c867','eee365de-cd10-4680-b1b9-29c4d35f1829','eee365de-cd10-4680-b1b9-29c4d35f1829','ingress','IPv4',NULL,NULL,NULL,NULL,2),('68bed08176254214900d5d6112eb3284','e653467e-2c42-4a1e-801b-87ff0e199872','1fd2dfa5-6205-4894-abcb-44850f8c9600',NULL,'egress','IPv6',NULL,NULL,NULL,NULL,12),('68bed08176254214900d5d6112eb3284','f69d9d5d-6848-4c4c-b0dc-8c8b37a6fd67','1fd2dfa5-6205-4894-abcb-44850f8c9600',NULL,'egress','IPv4',NULL,NULL,NULL,NULL,10);
/*!40000 ALTER TABLE `securitygrouprules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `securitygroups`
--

DROP TABLE IF EXISTS `securitygroups`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `securitygroups` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_securitygroups0standard_attr_id` (`standard_attr_id`),
  KEY `ix_securitygroups_tenant_id` (`tenant_id`),
  CONSTRAINT `securitygroups_ibfk_1` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `securitygroups`
--

LOCK TABLES `securitygroups` WRITE;
/*!40000 ALTER TABLE `securitygroups` DISABLE KEYS */;
INSERT INTO `securitygroups` VALUES ('b66fea8bb53a4b76bcda394c67284c53','08e1ed01-9c9a-448d-b465-87c99ca43d20','default',15),('68bed08176254214900d5d6112eb3284','1fd2dfa5-6205-4894-abcb-44850f8c9600','default',8),('391750905a7f491d85932ad58e809362','869cf42b-1d82-40e2-a975-adde0ce3e689','default',36),('52d692239a014c9a9960ce6b64d25dc8','eee365de-cd10-4680-b1b9-29c4d35f1829','default',1);
/*!40000 ALTER TABLE `securitygroups` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `serviceprofiles`
--

DROP TABLE IF EXISTS `serviceprofiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `serviceprofiles` (
  `id` varchar(36) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `driver` varchar(1024) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `metainfo` varchar(4096) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `serviceprofiles`
--

LOCK TABLES `serviceprofiles` WRITE;
/*!40000 ALTER TABLE `serviceprofiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `serviceprofiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sessionpersistences`
--

DROP TABLE IF EXISTS `sessionpersistences`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sessionpersistences` (
  `vip_id` varchar(36) NOT NULL,
  `type` enum('SOURCE_IP','HTTP_COOKIE','APP_COOKIE') NOT NULL,
  `cookie_name` varchar(1024) DEFAULT NULL,
  PRIMARY KEY (`vip_id`),
  CONSTRAINT `sessionpersistences_ibfk_1` FOREIGN KEY (`vip_id`) REFERENCES `vips` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sessionpersistences`
--

LOCK TABLES `sessionpersistences` WRITE;
/*!40000 ALTER TABLE `sessionpersistences` DISABLE KEYS */;
/*!40000 ALTER TABLE `sessionpersistences` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `standardattributes`
--

DROP TABLE IF EXISTS `standardattributes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `standardattributes` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(255) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `standardattributes`
--

LOCK TABLES `standardattributes` WRITE;
/*!40000 ALTER TABLE `standardattributes` DISABLE KEYS */;
INSERT INTO `standardattributes` VALUES (1,'securitygroups','2017-03-05 23:01:48','2017-03-05 23:01:48','Default security group'),(2,'securitygrouprules','2017-03-05 23:01:48','2017-03-05 23:01:48',''),(3,'securitygrouprules','2017-03-05 23:01:48','2017-03-05 23:01:48',''),(4,'securitygrouprules','2017-03-05 23:01:48','2017-03-05 23:01:48',''),(5,'securitygrouprules','2017-03-05 23:01:48','2017-03-05 23:01:48',''),(6,'securitygrouprules','2017-03-05 23:01:50','2017-03-05 23:01:50',''),(7,'securitygrouprules','2017-03-05 23:02:01','2017-03-05 23:02:01',''),(8,'securitygroups','2017-03-05 23:02:09','2017-03-05 23:02:09','Default security group'),(9,'securitygrouprules','2017-03-05 23:02:09','2017-03-05 23:02:09',''),(10,'securitygrouprules','2017-03-05 23:02:09','2017-03-05 23:02:09',''),(11,'securitygrouprules','2017-03-05 23:02:09','2017-03-05 23:02:09',''),(12,'securitygrouprules','2017-03-05 23:02:09','2017-03-05 23:02:09',''),(13,'securitygrouprules','2017-03-05 23:02:10','2017-03-05 23:02:10',''),(14,'securitygrouprules','2017-03-05 23:02:42','2017-03-05 23:02:42',''),(15,'securitygroups','2017-03-05 23:03:58','2017-03-05 23:03:58','Default security group'),(16,'securitygrouprules','2017-03-05 23:03:58','2017-03-05 23:03:58',''),(17,'securitygrouprules','2017-03-05 23:03:58','2017-03-05 23:03:58',''),(18,'securitygrouprules','2017-03-05 23:03:58','2017-03-05 23:03:58',''),(19,'securitygrouprules','2017-03-05 23:03:58','2017-03-05 23:03:58',''),(20,'networks','2017-03-05 23:03:58','2017-03-05 23:03:58',''),(21,'subnets','2017-03-05 23:37:10','2017-03-05 23:37:10',''),(22,'networks','2017-03-05 23:39:44','2017-03-05 23:39:44',''),(23,'subnets','2017-03-05 23:41:44','2017-03-05 23:41:44',''),(24,'networks','2017-03-05 23:42:11','2017-03-05 23:42:11',''),(25,'subnets','2017-03-05 23:42:36','2017-03-05 23:42:36',''),(26,'routers','2017-03-05 23:43:27','2017-03-05 23:43:58',''),(27,'ports','2017-03-05 23:43:44','2017-04-18 14:15:06',''),(28,'ports','2017-03-05 23:43:58','2017-04-18 14:15:05',''),(29,'routers','2017-03-05 23:44:24','2017-03-05 23:44:39',''),(30,'ports','2017-03-05 23:44:33','2017-04-18 14:15:05',''),(31,'ports','2017-03-05 23:44:39','2017-04-18 14:15:05',''),(32,'ports','2017-03-05 23:49:14','2017-04-18 14:15:06',''),(33,'ports','2017-03-05 23:49:14','2017-04-18 14:15:05',''),(34,'ports','2017-03-05 23:49:14','2017-04-18 14:15:06',''),(35,'ports','2017-03-07 14:47:51','2017-04-02 08:00:48',''),(36,'securitygroups','2017-03-07 14:48:13','2017-03-07 14:48:13','Default security group'),(37,'securitygrouprules','2017-03-07 14:48:13','2017-03-07 14:48:13',''),(38,'securitygrouprules','2017-03-07 14:48:13','2017-03-07 14:48:13',''),(39,'securitygrouprules','2017-03-07 14:48:13','2017-03-07 14:48:13',''),(40,'securitygrouprules','2017-03-07 14:48:13','2017-03-07 14:48:13',''),(41,'ports','2017-03-08 15:48:01','2017-03-08 15:48:01',''),(42,'floatingips','2017-03-08 15:48:01','2017-03-08 15:48:23','');
/*!40000 ALTER TABLE `standardattributes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subnetpoolprefixes`
--

DROP TABLE IF EXISTS `subnetpoolprefixes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subnetpoolprefixes` (
  `cidr` varchar(64) NOT NULL,
  `subnetpool_id` varchar(36) NOT NULL,
  PRIMARY KEY (`cidr`,`subnetpool_id`),
  KEY `subnetpool_id` (`subnetpool_id`),
  CONSTRAINT `subnetpoolprefixes_ibfk_1` FOREIGN KEY (`subnetpool_id`) REFERENCES `subnetpools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subnetpoolprefixes`
--

LOCK TABLES `subnetpoolprefixes` WRITE;
/*!40000 ALTER TABLE `subnetpoolprefixes` DISABLE KEYS */;
/*!40000 ALTER TABLE `subnetpoolprefixes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subnetpools`
--

DROP TABLE IF EXISTS `subnetpools`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subnetpools` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `ip_version` int(11) NOT NULL,
  `default_prefixlen` int(11) NOT NULL,
  `min_prefixlen` int(11) NOT NULL,
  `max_prefixlen` int(11) NOT NULL,
  `shared` tinyint(1) NOT NULL,
  `default_quota` int(11) DEFAULT NULL,
  `hash` varchar(36) NOT NULL DEFAULT '',
  `address_scope_id` varchar(36) DEFAULT NULL,
  `is_default` tinyint(1) NOT NULL DEFAULT '0',
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_subnetpools0standard_attr_id` (`standard_attr_id`),
  KEY `ix_subnetpools_tenant_id` (`tenant_id`),
  CONSTRAINT `subnetpools_ibfk_1` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subnetpools`
--

LOCK TABLES `subnetpools` WRITE;
/*!40000 ALTER TABLE `subnetpools` DISABLE KEYS */;
/*!40000 ALTER TABLE `subnetpools` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subnetroutes`
--

DROP TABLE IF EXISTS `subnetroutes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subnetroutes` (
  `destination` varchar(64) NOT NULL,
  `nexthop` varchar(64) NOT NULL,
  `subnet_id` varchar(36) NOT NULL,
  PRIMARY KEY (`destination`,`nexthop`,`subnet_id`),
  KEY `subnet_id` (`subnet_id`),
  CONSTRAINT `subnetroutes_ibfk_1` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subnetroutes`
--

LOCK TABLES `subnetroutes` WRITE;
/*!40000 ALTER TABLE `subnetroutes` DISABLE KEYS */;
/*!40000 ALTER TABLE `subnetroutes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `subnets`
--

DROP TABLE IF EXISTS `subnets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `subnets` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `network_id` varchar(36) DEFAULT NULL,
  `ip_version` int(11) NOT NULL,
  `cidr` varchar(64) NOT NULL,
  `gateway_ip` varchar(64) DEFAULT NULL,
  `enable_dhcp` tinyint(1) DEFAULT NULL,
  `ipv6_ra_mode` enum('slaac','dhcpv6-stateful','dhcpv6-stateless') DEFAULT NULL,
  `ipv6_address_mode` enum('slaac','dhcpv6-stateful','dhcpv6-stateless') DEFAULT NULL,
  `subnetpool_id` varchar(36) DEFAULT NULL,
  `standard_attr_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_subnets0standard_attr_id` (`standard_attr_id`),
  KEY `network_id` (`network_id`),
  KEY `ix_subnets_tenant_id` (`tenant_id`),
  KEY `ix_subnets_subnetpool_id` (`subnetpool_id`),
  CONSTRAINT `subnets_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`),
  CONSTRAINT `subnets_ibfk_2` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `subnets`
--

LOCK TABLES `subnets` WRITE;
/*!40000 ALTER TABLE `subnets` DISABLE KEYS */;
INSERT INTO `subnets` VALUES ('68bed08176254214900d5d6112eb3284','029772dd-bb4f-41f8-8dd6-831f7bbc540a','user1net','a19a8724-c0e4-4944-a636-f4a166537786',4,'192.168.13.0/24','192.168.13.1',1,NULL,NULL,NULL,23),('52d692239a014c9a9960ce6b64d25dc8','4160ef6e-0301-440b-a602-93a2c9080025','user2net','c308c5a6-3682-46e2-b633-6685d7b34b0e',4,'192.168.13.0/24','192.168.13.1',1,NULL,NULL,NULL,25),('b66fea8bb53a4b76bcda394c67284c53','d9b48087-72ed-4c99-95cd-466f46b0c361','provider','0267647d-80ff-4e2a-afbd-76ade85fa731',4,'203.0.113.0/24','203.0.113.254',1,NULL,NULL,NULL,21);
/*!40000 ALTER TABLE `subnets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tags`
--

DROP TABLE IF EXISTS `tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tags` (
  `standard_attr_id` bigint(20) NOT NULL,
  `tag` varchar(60) NOT NULL,
  PRIMARY KEY (`standard_attr_id`,`tag`),
  CONSTRAINT `tags_ibfk_1` FOREIGN KEY (`standard_attr_id`) REFERENCES `standardattributes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tags`
--

LOCK TABLES `tags` WRITE;
/*!40000 ALTER TABLE `tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `tags` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tz_network_bindings`
--

DROP TABLE IF EXISTS `tz_network_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tz_network_bindings` (
  `network_id` varchar(36) NOT NULL,
  `binding_type` enum('flat','vlan','stt','gre','l3_ext') NOT NULL,
  `phy_uuid` varchar(36) NOT NULL,
  `vlan_id` int(11) NOT NULL,
  PRIMARY KEY (`network_id`,`binding_type`,`phy_uuid`,`vlan_id`),
  CONSTRAINT `tz_network_bindings_ibfk_1` FOREIGN KEY (`network_id`) REFERENCES `networks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tz_network_bindings`
--

LOCK TABLES `tz_network_bindings` WRITE;
/*!40000 ALTER TABLE `tz_network_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `tz_network_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vcns_router_bindings`
--

DROP TABLE IF EXISTS `vcns_router_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vcns_router_bindings` (
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `router_id` varchar(36) NOT NULL,
  `edge_id` varchar(16) DEFAULT NULL,
  `lswitch_id` varchar(36) NOT NULL,
  PRIMARY KEY (`router_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vcns_router_bindings`
--

LOCK TABLES `vcns_router_bindings` WRITE;
/*!40000 ALTER TABLE `vcns_router_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `vcns_router_bindings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vips`
--

DROP TABLE IF EXISTS `vips`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vips` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `status` varchar(16) NOT NULL,
  `status_description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `port_id` varchar(36) DEFAULT NULL,
  `protocol_port` int(11) NOT NULL,
  `protocol` enum('HTTP','HTTPS','TCP') NOT NULL,
  `pool_id` varchar(36) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `connection_limit` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pool_id` (`pool_id`),
  KEY `port_id` (`port_id`),
  CONSTRAINT `vips_ibfk_1` FOREIGN KEY (`port_id`) REFERENCES `ports` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vips`
--

LOCK TABLES `vips` WRITE;
/*!40000 ALTER TABLE `vips` DISABLE KEYS */;
/*!40000 ALTER TABLE `vips` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vpnservices`
--

DROP TABLE IF EXISTS `vpnservices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vpnservices` (
  `tenant_id` varchar(255) DEFAULT NULL,
  `id` varchar(36) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(16) NOT NULL,
  `admin_state_up` tinyint(1) NOT NULL,
  `subnet_id` varchar(36) NOT NULL,
  `router_id` varchar(36) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `subnet_id` (`subnet_id`),
  KEY `router_id` (`router_id`),
  CONSTRAINT `vpnservices_ibfk_1` FOREIGN KEY (`subnet_id`) REFERENCES `subnets` (`id`),
  CONSTRAINT `vpnservices_ibfk_2` FOREIGN KEY (`router_id`) REFERENCES `routers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vpnservices`
--

LOCK TABLES `vpnservices` WRITE;
/*!40000 ALTER TABLE `vpnservices` DISABLE KEYS */;
/*!40000 ALTER TABLE `vpnservices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Current Database: `keystone`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `keystone` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `keystone`;

--
-- Table structure for table `access_token`
--

DROP TABLE IF EXISTS `access_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `access_token` (
  `id` varchar(64) NOT NULL,
  `access_secret` varchar(64) NOT NULL,
  `authorizing_user_id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  `role_ids` text NOT NULL,
  `consumer_id` varchar(64) NOT NULL,
  `expires_at` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_access_token_consumer_id` (`consumer_id`),
  KEY `ix_access_token_authorizing_user_id` (`authorizing_user_id`),
  CONSTRAINT `access_token_ibfk_1` FOREIGN KEY (`consumer_id`) REFERENCES `consumer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `access_token`
--

LOCK TABLES `access_token` WRITE;
/*!40000 ALTER TABLE `access_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `access_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `assignment`
--

DROP TABLE IF EXISTS `assignment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `assignment` (
  `type` enum('UserProject','GroupProject','UserDomain','GroupDomain') NOT NULL,
  `actor_id` varchar(64) NOT NULL,
  `target_id` varchar(64) NOT NULL,
  `role_id` varchar(64) NOT NULL,
  `inherited` tinyint(1) NOT NULL,
  PRIMARY KEY (`type`,`actor_id`,`target_id`,`role_id`,`inherited`),
  KEY `ix_actor_id` (`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `assignment`
--

LOCK TABLES `assignment` WRITE;
/*!40000 ALTER TABLE `assignment` DISABLE KEYS */;
INSERT INTO `assignment` VALUES ('UserProject','13f2f948f54546cf886a8ac1987193ad','391750905a7f491d85932ad58e809362','2f6bf3cfc64c4ad694f6bec7fd5a8221',0),('UserProject','14c5ccef7b6444ec9fff22173506fac0','1c498b93fb7e4e36afbfe778f4049cda','1aa170f1b0c841fd969e63aa5bbcae88',0),('UserProject','2676658a28e74ec3bd99c45854019610','52d692239a014c9a9960ce6b64d25dc8','c67d8ab32cdf40d2bc8eefb3a3d80b3f',0),('UserProject','8849b1309bd44da086619322734d51de','391750905a7f491d85932ad58e809362','2f6bf3cfc64c4ad694f6bec7fd5a8221',0),('UserProject','8d498828b2c24dcfac5e0f981b24ed7e','391750905a7f491d85932ad58e809362','2f6bf3cfc64c4ad694f6bec7fd5a8221',0),('UserProject','944628e7820f4c76bc8145d06eb9846b','b66fea8bb53a4b76bcda394c67284c53','2f6bf3cfc64c4ad694f6bec7fd5a8221',0),('UserProject','ac1e3b460bb14b059efb42332387b3b1','68bed08176254214900d5d6112eb3284','c67d8ab32cdf40d2bc8eefb3a3d80b3f',0);
/*!40000 ALTER TABLE `assignment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `config_register`
--

DROP TABLE IF EXISTS `config_register`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `config_register` (
  `type` varchar(64) NOT NULL,
  `domain_id` varchar(64) NOT NULL,
  PRIMARY KEY (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `config_register`
--

LOCK TABLES `config_register` WRITE;
/*!40000 ALTER TABLE `config_register` DISABLE KEYS */;
/*!40000 ALTER TABLE `config_register` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `consumer`
--

DROP TABLE IF EXISTS `consumer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `consumer` (
  `id` varchar(64) NOT NULL,
  `description` varchar(64) DEFAULT NULL,
  `secret` varchar(64) NOT NULL,
  `extra` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `consumer`
--

LOCK TABLES `consumer` WRITE;
/*!40000 ALTER TABLE `consumer` DISABLE KEYS */;
/*!40000 ALTER TABLE `consumer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `credential`
--

DROP TABLE IF EXISTS `credential`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `credential` (
  `id` varchar(64) NOT NULL,
  `user_id` varchar(64) NOT NULL,
  `project_id` varchar(64) DEFAULT NULL,
  `blob` text NOT NULL,
  `type` varchar(255) NOT NULL,
  `extra` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `credential`
--

LOCK TABLES `credential` WRITE;
/*!40000 ALTER TABLE `credential` DISABLE KEYS */;
/*!40000 ALTER TABLE `credential` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `domain`
--

DROP TABLE IF EXISTS `domain`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `domain` (
  `id` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `extra` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ixu_domain_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `domain`
--

LOCK TABLES `domain` WRITE;
/*!40000 ALTER TABLE `domain` DISABLE KEYS */;
INSERT INTO `domain` VALUES ('<<keystone.domain.root>>','<<keystone.domain.root>>',0,'{}');
/*!40000 ALTER TABLE `domain` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `endpoint`
--

DROP TABLE IF EXISTS `endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `endpoint` (
  `id` varchar(64) NOT NULL,
  `legacy_endpoint_id` varchar(64) DEFAULT NULL,
  `interface` varchar(8) NOT NULL,
  `service_id` varchar(64) NOT NULL,
  `url` text NOT NULL,
  `extra` text,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `region_id` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `service_id` (`service_id`),
  KEY `fk_endpoint_region_id` (`region_id`),
  CONSTRAINT `endpoint_service_id_fkey` FOREIGN KEY (`service_id`) REFERENCES `service` (`id`),
  CONSTRAINT `fk_endpoint_region_id` FOREIGN KEY (`region_id`) REFERENCES `region` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `endpoint`
--

LOCK TABLES `endpoint` WRITE;
/*!40000 ALTER TABLE `endpoint` DISABLE KEYS */;
INSERT INTO `endpoint` VALUES ('0dc7441b7e07415f88093054d2b59e14',NULL,'public','b7fc7945d7a64578813463d070c59deb','http://controller:9696','{}',1,'RegionOne'),('2a07a2e096b746a3a5f969c8e7f6280c',NULL,'admin','667dfd0dfe5f4145b723c787b3852f83','http://controller:9292','{}',1,'RegionOne'),('6242e9afc843449d812fe8ee71e5d2ca',NULL,'internal','667dfd0dfe5f4145b723c787b3852f83','http://controller:9292','{}',1,'RegionOne'),('7dc9b10afe0445369935ed5fb9428b0c',NULL,'internal','32ff38b0a90a48cd85c79ed9bbc9ef7f','http://controller:8774/v2.1/%(tenant_id)s','{}',1,'RegionOne'),('868c1ddc79454e9584e831e729caca5c',NULL,'internal','b7fc7945d7a64578813463d070c59deb','http://controller:9696','{}',1,'RegionOne'),('a6023652a9214248894ef380178078b8',NULL,'admin','b7fc7945d7a64578813463d070c59deb','http://controller:9696','{}',1,'RegionOne'),('a97ffa31113042bca6573176b98c7ee9',NULL,'public','667dfd0dfe5f4145b723c787b3852f83','http://controller:9292','{}',1,'RegionOne'),('b6c075de7626460b99a723fda1922d0e',NULL,'public','9ad176dc4a6d4fd8920a51c0cfd3ccac','http://controller:5000/v3','{}',1,'RegionOne'),('c6b83c2a630141f782c9ff8a6ef3c18c',NULL,'internal','9ad176dc4a6d4fd8920a51c0cfd3ccac','http://controller:5000/v3','{}',1,'RegionOne'),('c6d856aef8824e4b9b8b0b9e17718e76',NULL,'admin','9ad176dc4a6d4fd8920a51c0cfd3ccac','http://controller:35357/v3','{}',1,'RegionOne'),('e863832841224f8ebbe9076f368a8d43',NULL,'public','32ff38b0a90a48cd85c79ed9bbc9ef7f','http://controller:8774/v2.1/%(tenant_id)s','{}',1,'RegionOne'),('ed1798295ba64aabb706e0268948c61a',NULL,'admin','32ff38b0a90a48cd85c79ed9bbc9ef7f','http://controller:8774/v2.1/%(tenant_id)s','{}',1,'RegionOne');
/*!40000 ALTER TABLE `endpoint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `endpoint_group`
--

DROP TABLE IF EXISTS `endpoint_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `endpoint_group` (
  `id` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` text,
  `filters` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `endpoint_group`
--

LOCK TABLES `endpoint_group` WRITE;
/*!40000 ALTER TABLE `endpoint_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `endpoint_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `federated_user`
--

DROP TABLE IF EXISTS `federated_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `federated_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) NOT NULL,
  `idp_id` varchar(64) NOT NULL,
  `protocol_id` varchar(64) NOT NULL,
  `unique_id` varchar(255) NOT NULL,
  `display_name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idp_id` (`idp_id`,`protocol_id`,`unique_id`),
  KEY `user_id` (`user_id`),
  KEY `federated_user_protocol_id_fkey` (`protocol_id`,`idp_id`),
  CONSTRAINT `federated_user_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `federated_user_ibfk_2` FOREIGN KEY (`idp_id`) REFERENCES `identity_provider` (`id`) ON DELETE CASCADE,
  CONSTRAINT `federated_user_protocol_id_fkey` FOREIGN KEY (`protocol_id`, `idp_id`) REFERENCES `federation_protocol` (`id`, `idp_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `federated_user`
--

LOCK TABLES `federated_user` WRITE;
/*!40000 ALTER TABLE `federated_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `federated_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `federation_protocol`
--

DROP TABLE IF EXISTS `federation_protocol`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `federation_protocol` (
  `id` varchar(64) NOT NULL,
  `idp_id` varchar(64) NOT NULL,
  `mapping_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`,`idp_id`),
  KEY `idp_id` (`idp_id`),
  CONSTRAINT `federation_protocol_ibfk_1` FOREIGN KEY (`idp_id`) REFERENCES `identity_provider` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `federation_protocol`
--

LOCK TABLES `federation_protocol` WRITE;
/*!40000 ALTER TABLE `federation_protocol` DISABLE KEYS */;
/*!40000 ALTER TABLE `federation_protocol` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `group`
--

DROP TABLE IF EXISTS `group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `group` (
  `id` varchar(64) NOT NULL,
  `domain_id` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `description` text,
  `extra` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ixu_group_name_domain_id` (`domain_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `group`
--

LOCK TABLES `group` WRITE;
/*!40000 ALTER TABLE `group` DISABLE KEYS */;
/*!40000 ALTER TABLE `group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `id_mapping`
--

DROP TABLE IF EXISTS `id_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `id_mapping` (
  `public_id` varchar(64) NOT NULL,
  `domain_id` varchar(64) NOT NULL,
  `local_id` varchar(64) NOT NULL,
  `entity_type` enum('user','group') NOT NULL,
  PRIMARY KEY (`public_id`),
  UNIQUE KEY `domain_id` (`domain_id`,`local_id`,`entity_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `id_mapping`
--

LOCK TABLES `id_mapping` WRITE;
/*!40000 ALTER TABLE `id_mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `id_mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `identity_provider`
--

DROP TABLE IF EXISTS `identity_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `identity_provider` (
  `id` varchar(64) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `identity_provider`
--

LOCK TABLES `identity_provider` WRITE;
/*!40000 ALTER TABLE `identity_provider` DISABLE KEYS */;
/*!40000 ALTER TABLE `identity_provider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `idp_remote_ids`
--

DROP TABLE IF EXISTS `idp_remote_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `idp_remote_ids` (
  `idp_id` varchar(64) DEFAULT NULL,
  `remote_id` varchar(255) NOT NULL,
  PRIMARY KEY (`remote_id`),
  KEY `idp_id` (`idp_id`),
  CONSTRAINT `idp_remote_ids_ibfk_1` FOREIGN KEY (`idp_id`) REFERENCES `identity_provider` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `idp_remote_ids`
--

LOCK TABLES `idp_remote_ids` WRITE;
/*!40000 ALTER TABLE `idp_remote_ids` DISABLE KEYS */;
/*!40000 ALTER TABLE `idp_remote_ids` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `implied_role`
--

DROP TABLE IF EXISTS `implied_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `implied_role` (
  `prior_role_id` varchar(64) NOT NULL,
  `implied_role_id` varchar(64) NOT NULL,
  PRIMARY KEY (`prior_role_id`,`implied_role_id`),
  KEY `implied_role_implied_role_id_fkey` (`implied_role_id`),
  CONSTRAINT `implied_role_implied_role_id_fkey` FOREIGN KEY (`implied_role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE,
  CONSTRAINT `implied_role_prior_role_id_fkey` FOREIGN KEY (`prior_role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `implied_role`
--

LOCK TABLES `implied_role` WRITE;
/*!40000 ALTER TABLE `implied_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `implied_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `local_user`
--

DROP TABLE IF EXISTS `local_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `local_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(64) NOT NULL,
  `domain_id` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `domain_id` (`domain_id`,`name`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `local_user_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `local_user`
--

LOCK TABLES `local_user` WRITE;
/*!40000 ALTER TABLE `local_user` DISABLE KEYS */;
INSERT INTO `local_user` VALUES (1,'944628e7820f4c76bc8145d06eb9846b','5314e5eeed944bc6908597f0ed72e3f1','admin'),(2,'14c5ccef7b6444ec9fff22173506fac0','5314e5eeed944bc6908597f0ed72e3f1','demo'),(3,'13f2f948f54546cf886a8ac1987193ad','5314e5eeed944bc6908597f0ed72e3f1','glance'),(4,'8849b1309bd44da086619322734d51de','5314e5eeed944bc6908597f0ed72e3f1','nova'),(5,'8d498828b2c24dcfac5e0f981b24ed7e','5314e5eeed944bc6908597f0ed72e3f1','neutron'),(6,'ac1e3b460bb14b059efb42332387b3b1','5314e5eeed944bc6908597f0ed72e3f1','user1'),(7,'2676658a28e74ec3bd99c45854019610','5314e5eeed944bc6908597f0ed72e3f1','user2');
/*!40000 ALTER TABLE `local_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mapping`
--

DROP TABLE IF EXISTS `mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mapping` (
  `id` varchar(64) NOT NULL,
  `rules` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mapping`
--

LOCK TABLES `mapping` WRITE;
/*!40000 ALTER TABLE `mapping` DISABLE KEYS */;
/*!40000 ALTER TABLE `mapping` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `migrate_version`
--

DROP TABLE IF EXISTS `migrate_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `migrate_version` (
  `repository_id` varchar(250) NOT NULL,
  `repository_path` text,
  `version` int(11) DEFAULT NULL,
  PRIMARY KEY (`repository_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `migrate_version`
--

LOCK TABLES `migrate_version` WRITE;
/*!40000 ALTER TABLE `migrate_version` DISABLE KEYS */;
INSERT INTO `migrate_version` VALUES ('keystone','/usr/lib/python2.7/dist-packages/keystone/common/sql/migrate_repo',97);
/*!40000 ALTER TABLE `migrate_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password`
--

DROP TABLE IF EXISTS `password`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `password` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `local_user_id` int(11) NOT NULL,
  `password` varchar(128) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `local_user_id` (`local_user_id`),
  CONSTRAINT `password_ibfk_1` FOREIGN KEY (`local_user_id`) REFERENCES `local_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password`
--

LOCK TABLES `password` WRITE;
/*!40000 ALTER TABLE `password` DISABLE KEYS */;
INSERT INTO `password` VALUES (1,1,'$6$rounds=10000$8aEYBNTIixmYu3uZ$YGCGCWBBAIHnreh/W/nR8rikNiQnNh1Ex9athFnWOOJ125nQqJWHdHrx9wr3dVFNgxuEq4gbBSRdjv6b0gBS1/'),(2,2,'$6$rounds=10000$WSlU.tJqw7GWCqbg$pvdIL/pSkP2Zt9Fy21e/uy/TVTbeXuj6J9dlIBGAB6heS.K0LQH24yhS6wXc2N0KQwOUDjeJyetqiM3otucbn/'),(3,3,'$6$rounds=10000$rf288Hcj9wWpBopq$DK.EmsmQREF7Aq3Xp5rNFoiM03oAar4DIVBnUKERIMMs6tp/7nCFnpV.zulS7XsF4iTxG4xUIav5M1oFh8xCE1'),(4,4,'$6$rounds=10000$XksPps.DkkT1oMdm$ubAb5HORpKGxdb0HmX9ar8EcAqlxRumxY/Xwu9J704lGNM3ErTNSagMz.qCzAX4k99I6iCeXihOYrb4fRVszY/'),(5,5,'$6$rounds=10000$1gwB5pl.pbwl/FwD$BOgZbcqJFsQbUWQefr4Y9dS1LFDyUzJBokVy0XUGtB8p1JrRXw70o8w3kSGqAo19efpg.U1cH2/mncA3W3o8C1'),(6,6,'$6$rounds=10000$QBN831pMX87KRjff$8xz5GXNasgGfh1/V5lwOHGnxWpVS/BBKJ8IYeJpPT1aTzT8tO0XygceZR5rq7aEgP1b7fcWz20iSfZQyQKScl.'),(7,7,'$6$rounds=10000$uZEzZREqwfD9fwMY$emDrU0CGGxcqw50S82C.CfUHwwiJQeZwsF65p8norTD3HDRyn9.jLJPQsildW91NvZ.GPbNlT8ucyhMJckdrK/');
/*!40000 ALTER TABLE `password` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `policy`
--

DROP TABLE IF EXISTS `policy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `policy` (
  `id` varchar(64) NOT NULL,
  `type` varchar(255) NOT NULL,
  `blob` text NOT NULL,
  `extra` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `policy`
--

LOCK TABLES `policy` WRITE;
/*!40000 ALTER TABLE `policy` DISABLE KEYS */;
/*!40000 ALTER TABLE `policy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `policy_association`
--

DROP TABLE IF EXISTS `policy_association`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `policy_association` (
  `id` varchar(64) NOT NULL,
  `policy_id` varchar(64) NOT NULL,
  `endpoint_id` varchar(64) DEFAULT NULL,
  `service_id` varchar(64) DEFAULT NULL,
  `region_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `endpoint_id` (`endpoint_id`,`service_id`,`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `policy_association`
--

LOCK TABLES `policy_association` WRITE;
/*!40000 ALTER TABLE `policy_association` DISABLE KEYS */;
/*!40000 ALTER TABLE `policy_association` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project` (
  `id` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `extra` text,
  `description` text,
  `enabled` tinyint(1) DEFAULT NULL,
  `domain_id` varchar(64) NOT NULL,
  `parent_id` varchar(64) DEFAULT NULL,
  `is_domain` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ixu_project_name_domain_id` (`domain_id`,`name`),
  KEY `project_parent_id_fkey` (`parent_id`),
  CONSTRAINT `project_domain_id_fkey` FOREIGN KEY (`domain_id`) REFERENCES `project` (`id`),
  CONSTRAINT `project_parent_id_fkey` FOREIGN KEY (`parent_id`) REFERENCES `project` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES ('1c498b93fb7e4e36afbfe778f4049cda','demo','{}','Demo Project',1,'5314e5eeed944bc6908597f0ed72e3f1','5314e5eeed944bc6908597f0ed72e3f1',0),('391750905a7f491d85932ad58e809362','service','{}','Service Project',1,'5314e5eeed944bc6908597f0ed72e3f1','5314e5eeed944bc6908597f0ed72e3f1',0),('52d692239a014c9a9960ce6b64d25dc8','user2_prj','{}','User2 project',1,'5314e5eeed944bc6908597f0ed72e3f1','5314e5eeed944bc6908597f0ed72e3f1',0),('5314e5eeed944bc6908597f0ed72e3f1','default','{}','Default Domain',1,'<<keystone.domain.root>>',NULL,1),('68bed08176254214900d5d6112eb3284','user1_prj','{}','User1 project',1,'5314e5eeed944bc6908597f0ed72e3f1','5314e5eeed944bc6908597f0ed72e3f1',0),('<<keystone.domain.root>>','<<keystone.domain.root>>','{}','',0,'<<keystone.domain.root>>',NULL,1),('b66fea8bb53a4b76bcda394c67284c53','admin','{}','Admin Project',1,'5314e5eeed944bc6908597f0ed72e3f1','5314e5eeed944bc6908597f0ed72e3f1',0);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_endpoint`
--

DROP TABLE IF EXISTS `project_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_endpoint` (
  `endpoint_id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  PRIMARY KEY (`endpoint_id`,`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_endpoint`
--

LOCK TABLES `project_endpoint` WRITE;
/*!40000 ALTER TABLE `project_endpoint` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_endpoint` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_endpoint_group`
--

DROP TABLE IF EXISTS `project_endpoint_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_endpoint_group` (
  `endpoint_group_id` varchar(64) NOT NULL,
  `project_id` varchar(64) NOT NULL,
  PRIMARY KEY (`endpoint_group_id`,`project_id`),
  CONSTRAINT `project_endpoint_group_ibfk_1` FOREIGN KEY (`endpoint_group_id`) REFERENCES `endpoint_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_endpoint_group`
--

LOCK TABLES `project_endpoint_group` WRITE;
/*!40000 ALTER TABLE `project_endpoint_group` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_endpoint_group` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `region` (
  `id` varchar(255) NOT NULL,
  `description` varchar(255) NOT NULL,
  `parent_region_id` varchar(255) DEFAULT NULL,
  `extra` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `region`
--

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
INSERT INTO `region` VALUES ('RegionOne','',NULL,'{}');
/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `request_token`
--

DROP TABLE IF EXISTS `request_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `request_token` (
  `id` varchar(64) NOT NULL,
  `request_secret` varchar(64) NOT NULL,
  `verifier` varchar(64) DEFAULT NULL,
  `authorizing_user_id` varchar(64) DEFAULT NULL,
  `requested_project_id` varchar(64) NOT NULL,
  `role_ids` text,
  `consumer_id` varchar(64) NOT NULL,
  `expires_at` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_request_token_consumer_id` (`consumer_id`),
  CONSTRAINT `request_token_ibfk_1` FOREIGN KEY (`consumer_id`) REFERENCES `consumer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `request_token`
--

LOCK TABLES `request_token` WRITE;
/*!40000 ALTER TABLE `request_token` DISABLE KEYS */;
/*!40000 ALTER TABLE `request_token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `revocation_event`
--

DROP TABLE IF EXISTS `revocation_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `revocation_event` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `domain_id` varchar(64) DEFAULT NULL,
  `project_id` varchar(64) DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `role_id` varchar(64) DEFAULT NULL,
  `trust_id` varchar(64) DEFAULT NULL,
  `consumer_id` varchar(64) DEFAULT NULL,
  `access_token_id` varchar(64) DEFAULT NULL,
  `issued_before` datetime NOT NULL,
  `expires_at` datetime DEFAULT NULL,
  `revoked_at` datetime NOT NULL,
  `audit_id` varchar(32) DEFAULT NULL,
  `audit_chain_id` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_revocation_event_new_revoked_at` (`revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `revocation_event`
--

LOCK TABLES `revocation_event` WRITE;
/*!40000 ALTER TABLE `revocation_event` DISABLE KEYS */;
/*!40000 ALTER TABLE `revocation_event` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role` (
  `id` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `extra` text,
  `domain_id` varchar(64) NOT NULL DEFAULT '<<null>>',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ixu_role_name_domain_id` (`name`,`domain_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES ('1aa170f1b0c841fd969e63aa5bbcae88','user','{}','<<null>>'),('2f6bf3cfc64c4ad694f6bec7fd5a8221','admin','{}','<<null>>'),('c67d8ab32cdf40d2bc8eefb3a3d80b3f','owner','{}','<<null>>');
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sensitive_config`
--

DROP TABLE IF EXISTS `sensitive_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sensitive_config` (
  `domain_id` varchar(64) NOT NULL,
  `group` varchar(255) NOT NULL,
  `option` varchar(255) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`domain_id`,`group`,`option`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sensitive_config`
--

LOCK TABLES `sensitive_config` WRITE;
/*!40000 ALTER TABLE `sensitive_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `sensitive_config` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service` (
  `id` varchar(64) NOT NULL,
  `type` varchar(255) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `extra` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service`
--

LOCK TABLES `service` WRITE;
/*!40000 ALTER TABLE `service` DISABLE KEYS */;
INSERT INTO `service` VALUES ('32ff38b0a90a48cd85c79ed9bbc9ef7f','compute',1,'{\"description\": \"OpenStack Compute\", \"name\": \"nova\"}'),('667dfd0dfe5f4145b723c787b3852f83','image',1,'{\"description\": \"OpenStack Image\", \"name\": \"glance\"}'),('9ad176dc4a6d4fd8920a51c0cfd3ccac','identity',1,'{\"description\": \"OpenStack Identity\", \"name\": \"keystone\"}'),('b7fc7945d7a64578813463d070c59deb','network',1,'{\"description\": \"OpenStack Networking\", \"name\": \"neutron\"}');
/*!40000 ALTER TABLE `service` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `service_provider`
--

DROP TABLE IF EXISTS `service_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `service_provider` (
  `auth_url` varchar(256) NOT NULL,
  `id` varchar(64) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `description` text,
  `sp_url` varchar(256) NOT NULL,
  `relay_state_prefix` varchar(256) NOT NULL DEFAULT 'ss:mem:',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `service_provider`
--

LOCK TABLES `service_provider` WRITE;
/*!40000 ALTER TABLE `service_provider` DISABLE KEYS */;
/*!40000 ALTER TABLE `service_provider` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `token`
--

DROP TABLE IF EXISTS `token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `token` (
  `id` varchar(64) NOT NULL,
  `expires` datetime DEFAULT NULL,
  `extra` text,
  `valid` tinyint(1) NOT NULL,
  `trust_id` varchar(64) DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ix_token_expires` (`expires`),
  KEY `ix_token_expires_valid` (`expires`,`valid`),
  KEY `ix_token_user_id` (`user_id`),
  KEY `ix_token_trust_id` (`trust_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `token`
--

LOCK TABLES `token` WRITE;
/*!40000 ALTER TABLE `token` DISABLE KEYS */;
/*!40000 ALTER TABLE `token` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trust`
--

DROP TABLE IF EXISTS `trust`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trust` (
  `id` varchar(64) NOT NULL,
  `trustor_user_id` varchar(64) NOT NULL,
  `trustee_user_id` varchar(64) NOT NULL,
  `project_id` varchar(64) DEFAULT NULL,
  `impersonation` tinyint(1) NOT NULL,
  `deleted_at` datetime DEFAULT NULL,
  `expires_at` datetime DEFAULT NULL,
  `remaining_uses` int(11) DEFAULT NULL,
  `extra` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `duplicate_trust_constraint` (`trustor_user_id`,`trustee_user_id`,`project_id`,`impersonation`,`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trust`
--

LOCK TABLES `trust` WRITE;
/*!40000 ALTER TABLE `trust` DISABLE KEYS */;
/*!40000 ALTER TABLE `trust` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `trust_role`
--

DROP TABLE IF EXISTS `trust_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `trust_role` (
  `trust_id` varchar(64) NOT NULL,
  `role_id` varchar(64) NOT NULL,
  PRIMARY KEY (`trust_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trust_role`
--

LOCK TABLES `trust_role` WRITE;
/*!40000 ALTER TABLE `trust_role` DISABLE KEYS */;
/*!40000 ALTER TABLE `trust_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user` (
  `id` varchar(64) NOT NULL,
  `extra` text,
  `enabled` tinyint(1) DEFAULT NULL,
  `default_project_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES ('13f2f948f54546cf886a8ac1987193ad','{}',1,NULL),('14c5ccef7b6444ec9fff22173506fac0','{}',1,NULL),('2676658a28e74ec3bd99c45854019610','{}',1,NULL),('8849b1309bd44da086619322734d51de','{}',1,NULL),('8d498828b2c24dcfac5e0f981b24ed7e','{}',1,NULL),('944628e7820f4c76bc8145d06eb9846b','{}',1,NULL),('ac1e3b460bb14b059efb42332387b3b1','{}',1,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_group_membership`
--

DROP TABLE IF EXISTS `user_group_membership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_group_membership` (
  `user_id` varchar(64) NOT NULL,
  `group_id` varchar(64) NOT NULL,
  PRIMARY KEY (`user_id`,`group_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `fk_user_group_membership_group_id` FOREIGN KEY (`group_id`) REFERENCES `group` (`id`),
  CONSTRAINT `fk_user_group_membership_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_group_membership`
--

LOCK TABLES `user_group_membership` WRITE;
/*!40000 ALTER TABLE `user_group_membership` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_group_membership` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `whitelisted_config`
--

DROP TABLE IF EXISTS `whitelisted_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `whitelisted_config` (
  `domain_id` varchar(64) NOT NULL,
  `group` varchar(255) NOT NULL,
  `option` varchar(255) NOT NULL,
  `value` text NOT NULL,
  PRIMARY KEY (`domain_id`,`group`,`option`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `whitelisted_config`
--

LOCK TABLES `whitelisted_config` WRITE;
/*!40000 ALTER TABLE `whitelisted_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `whitelisted_config` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-04-18 14:17:00
