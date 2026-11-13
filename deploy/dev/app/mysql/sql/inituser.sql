CREATE database if NOT EXISTS `bitehouse_nacos_dev` default character set utf8mb4 collate utf8mb4_general_ci;
CREATE database if NOT EXISTS `bitehouse_dev` default character set utf8mb4 collate utf8mb4_general_ci;

CREATE USER 'bitedev'@'%' IDENTIFIED BY 'bite@123';
grant replication slave, replication client on *.* to 'bitedev'@'%';

GRANT ALL PRIVILEGES ON bitehouse_nacos_dev.* TO  'bitedev'@'%';
GRANT ALL PRIVILEGES ON bitehouse_dev.* TO  'bitedev'@'%';

FLUSH PRIVILEGES;
