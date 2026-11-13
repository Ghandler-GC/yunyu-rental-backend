# 1、初始化数据库：创建nacos外接数据库bitehouse_nacos_dev和脚手架业务数据库bitehouse_dev
# 2、创建用户，用户名：bitedev 密码：YOUR_PASSWORD
# 3、授予bitedev用户特定权限

CREATE database if NOT EXISTS `bitehouse_nacos_prd` default character set utf8mb4 collate utf8mb4_general_ci;
CREATE database if NOT EXISTS `bitehouse_prd` default character set utf8mb4 collate utf8mb4_general_ci;

CREATE USER 'bitedev'@'%' IDENTIFIED BY 'YOUR_PASSWORD';
grant replication slave, replication client on *.* to 'bitedev'@'%';

GRANT ALL PRIVILEGES ON bitehouse_nacos_prd.* TO  'bitedev'@'%';
GRANT ALL PRIVILEGES ON bitehouse_prd.* TO  'bitedev'@'%';

FLUSH PRIVILEGES;
