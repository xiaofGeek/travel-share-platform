CREATE DATABASE IF NOT EXISTS `travel_share` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `travel_share`;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS travel_announcement,travel_audit_record,travel_report,travel_message,travel_browse_history,travel_hot_keyword,travel_search_history,travel_recommendation,travel_banner,travel_route_item,travel_route_day,travel_route_destination,travel_route,travel_comment_like,travel_comment,travel_favorite,travel_favorite_folder,travel_guide_like,travel_guide_tag,travel_guide_image,travel_guide,travel_tag,travel_topic_guide,travel_topic_destination,travel_topic,travel_scenic_spot,travel_destination_image,travel_destination,travel_user_follow,travel_user_profile,sys_file,sys_config,sys_login_log,sys_operation_log,sys_role_menu,sys_menu,sys_user_role,sys_user,sys_role;

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  code VARCHAR(30) NOT NULL UNIQUE,
  description VARCHAR(255),
  sort_order INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(32) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50) NOT NULL,
  email VARCHAR(100) UNIQUE,
  phone VARCHAR(30),
  avatar VARCHAR(255),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  status TINYINT NOT NULL DEFAULT 1,
  city VARCHAR(50),
  bio VARCHAR(300),
  preferences VARCHAR(255),
  cover_image VARCHAR(255),
  visited_cities INT NOT NULL DEFAULT 0,
  guide_count INT NOT NULL DEFAULT 0,
  route_count INT NOT NULL DEFAULT 0,
  follower_count INT NOT NULL DEFAULT 0,
  following_count INT NOT NULL DEFAULT 0,
  received_likes INT NOT NULL DEFAULT 0,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_role_status(role,status), INDEX idx_user_created(create_time)
) ENGINE=InnoDB;

CREATE TABLE sys_user_role (user_id BIGINT NOT NULL,role_id BIGINT NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(user_id,role_id)) ENGINE=InnoDB;
CREATE TABLE sys_menu (id BIGINT PRIMARY KEY AUTO_INCREMENT,parent_id BIGINT DEFAULT 0,name VARCHAR(50) NOT NULL,path VARCHAR(120),component VARCHAR(120),permission VARCHAR(100),icon VARCHAR(50),type VARCHAR(20) DEFAULT 'MENU',sort_order INT DEFAULT 0,visible TINYINT DEFAULT 1,status TINYINT DEFAULT 1,create_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE sys_role_menu (role_id BIGINT NOT NULL,menu_id BIGINT NOT NULL,PRIMARY KEY(role_id,menu_id)) ENGINE=InnoDB;
CREATE TABLE sys_operation_log (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT,username VARCHAR(32),module VARCHAR(50),operation VARCHAR(100),method VARCHAR(200),request_uri VARCHAR(255),ip VARCHAR(64),status TINYINT DEFAULT 1,duration_ms BIGINT,detail VARCHAR(500),create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_operation_created(create_time)) ENGINE=InnoDB;
CREATE TABLE sys_login_log (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT,username VARCHAR(32),ip VARCHAR(64),browser VARCHAR(100),os VARCHAR(100),status TINYINT DEFAULT 1,message VARCHAR(255),create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_login_created(create_time)) ENGINE=InnoDB;
CREATE TABLE sys_config (id BIGINT PRIMARY KEY AUTO_INCREMENT,config_key VARCHAR(100) NOT NULL UNIQUE,config_value TEXT,config_name VARCHAR(100),config_type VARCHAR(20) DEFAULT 'STRING',remark VARCHAR(255),update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE sys_file (id BIGINT PRIMARY KEY AUTO_INCREMENT,original_name VARCHAR(255),stored_name VARCHAR(255) NOT NULL UNIQUE,file_path VARCHAR(500) NOT NULL,file_type VARCHAR(50),file_size BIGINT,uploader_id BIGINT,business_type VARCHAR(30),referenced TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;

CREATE TABLE travel_user_profile (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL UNIQUE,gender_display VARCHAR(20),home_city VARCHAR(50),travel_preferences VARCHAR(255),background_image VARCHAR(255),website VARCHAR(255),create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE travel_user_follow (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,target_user_id BIGINT NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_follow(user_id,target_user_id),INDEX idx_follow_target(target_user_id)) ENGINE=InnoDB;

CREATE TABLE travel_destination (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,code VARCHAR(50) NOT NULL UNIQUE,name VARCHAR(80) NOT NULL,name_en VARCHAR(100),type VARCHAR(30) NOT NULL,parent_id BIGINT,cover_image VARCHAR(255),summary VARCHAR(500),description TEXT,season VARCHAR(100),suggested_days INT,average_budget DECIMAL(10,2),tags VARCHAR(255),location_text VARCHAR(255),longitude DECIMAL(10,6),latitude DECIMAL(10,6),guide_count INT DEFAULT 0,favorite_count INT DEFAULT 0,view_count INT DEFAULT 0,recommended TINYINT DEFAULT 0,enabled TINYINT DEFAULT 1,sort_order INT DEFAULT 0,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_destination_parent(parent_id),INDEX idx_destination_recommend(recommended,enabled,sort_order)
) ENGINE=InnoDB;
CREATE TABLE travel_destination_image (id BIGINT PRIMARY KEY AUTO_INCREMENT,destination_id BIGINT NOT NULL,image_url VARCHAR(255) NOT NULL,caption VARCHAR(255),sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_destination_image(destination_id)) ENGINE=InnoDB;
CREATE TABLE travel_scenic_spot (id BIGINT PRIMARY KEY AUTO_INCREMENT,destination_id BIGINT NOT NULL,name VARCHAR(100) NOT NULL,cover_image VARCHAR(255),summary VARCHAR(500),address VARCHAR(255),recommended_season VARCHAR(100),suggested_hours DECIMAL(4,1),tips VARCHAR(500),enabled TINYINT DEFAULT 1,sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_spot_destination(destination_id)) ENGINE=InnoDB;

CREATE TABLE travel_topic (id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(100) NOT NULL,subtitle VARCHAR(180),cover_image VARCHAR(255),summary VARCHAR(500),content TEXT,recommended TINYINT DEFAULT 0,enabled TINYINT DEFAULT 1,sort_order INT DEFAULT 0,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE travel_topic_destination (topic_id BIGINT NOT NULL,destination_id BIGINT NOT NULL,sort_order INT DEFAULT 0,PRIMARY KEY(topic_id,destination_id)) ENGINE=InnoDB;
CREATE TABLE travel_topic_guide (topic_id BIGINT NOT NULL,guide_id BIGINT NOT NULL,sort_order INT DEFAULT 0,PRIMARY KEY(topic_id,guide_id)) ENGINE=InnoDB;
CREATE TABLE travel_tag (id BIGINT PRIMARY KEY AUTO_INCREMENT,name VARCHAR(50) NOT NULL,code VARCHAR(60) NOT NULL UNIQUE,type VARCHAR(30) DEFAULT 'GUIDE',use_count INT DEFAULT 0,enabled TINYINT DEFAULT 1,sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;

CREATE TABLE travel_guide (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,guide_no VARCHAR(40) NOT NULL UNIQUE,title VARCHAR(120) NOT NULL,subtitle VARCHAR(180),cover_image VARCHAR(255) NOT NULL,summary VARCHAR(600) NOT NULL,author_id BIGINT NOT NULL,destination_id BIGINT NOT NULL,topic_id BIGINT,days INT,budget DECIMAL(10,2),months VARCHAR(100),travel_mode VARCHAR(50),audience VARCHAR(100),content LONGTEXT NOT NULL,expenses TEXT,tips TEXT,status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',audit_status VARCHAR(30) NOT NULL DEFAULT 'NOT_SUBMITTED',audit_opinion VARCHAR(500),featured TINYINT DEFAULT 0,pinned TINYINT DEFAULT 0,view_count INT DEFAULT 0,like_count INT DEFAULT 0,favorite_count INT DEFAULT 0,comment_count INT DEFAULT 0,published_at DATETIME,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_guide_author(author_id),INDEX idx_guide_destination(destination_id),INDEX idx_guide_status(status,audit_status),INDEX idx_guide_publish(published_at),FULLTEXT KEY ft_guide_text(title,summary)
) ENGINE=InnoDB;
CREATE TABLE travel_guide_image (id BIGINT PRIMARY KEY AUTO_INCREMENT,guide_id BIGINT NOT NULL,image_url VARCHAR(255) NOT NULL,caption VARCHAR(255),image_type VARCHAR(20) DEFAULT 'CONTENT',sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_guide_image(guide_id)) ENGINE=InnoDB;
CREATE TABLE travel_guide_tag (guide_id BIGINT NOT NULL,tag_id BIGINT NOT NULL,PRIMARY KEY(guide_id,tag_id)) ENGINE=InnoDB;
CREATE TABLE travel_guide_like (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,guide_id BIGINT NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_guide_like(user_id,guide_id),INDEX idx_like_guide(guide_id)) ENGINE=InnoDB;
CREATE TABLE travel_favorite_folder (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,name VARCHAR(60) NOT NULL,is_default TINYINT DEFAULT 0,sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_folder(user_id,name)) ENGINE=InnoDB;
CREATE TABLE travel_favorite (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,folder_id BIGINT,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_favorite(user_id,folder_id,target_type,target_id),INDEX idx_favorite_target(target_type,target_id)) ENGINE=InnoDB;

CREATE TABLE travel_comment (id BIGINT PRIMARY KEY AUTO_INCREMENT,guide_id BIGINT NOT NULL,user_id BIGINT NOT NULL,parent_id BIGINT,reply_user_id BIGINT,content VARCHAR(500) NOT NULL,like_count INT DEFAULT 0,status VARCHAR(20) DEFAULT 'NORMAL',deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_comment_guide(guide_id,status),INDEX idx_comment_parent(parent_id)) ENGINE=InnoDB;
CREATE TABLE travel_comment_like (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,comment_id BIGINT NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_comment_like(user_id,comment_id)) ENGINE=InnoDB;

CREATE TABLE travel_route (id BIGINT PRIMARY KEY AUTO_INCREMENT,route_no VARCHAR(40) NOT NULL UNIQUE,name VARCHAR(120) NOT NULL,cover_image VARCHAR(255),user_id BIGINT NOT NULL,destination_id BIGINT,total_days INT NOT NULL,budget DECIMAL(10,2),start_point VARCHAR(100),end_point VARCHAR(100),season VARCHAR(100),audience VARCHAR(100),summary VARCHAR(1000),status VARCHAR(20) DEFAULT 'PUBLISHED',is_public TINYINT DEFAULT 1,favorite_count INT DEFAULT 0,view_count INT DEFAULT 0,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_route_user(user_id),INDEX idx_route_public(is_public,status)) ENGINE=InnoDB;
CREATE TABLE travel_route_destination (route_id BIGINT NOT NULL,destination_id BIGINT NOT NULL,sort_order INT DEFAULT 0,PRIMARY KEY(route_id,destination_id)) ENGINE=InnoDB;
CREATE TABLE travel_route_day (id BIGINT PRIMARY KEY AUTO_INCREMENT,route_id BIGINT NOT NULL,day_number INT NOT NULL,title VARCHAR(120),summary VARCHAR(500),daily_cost DECIMAL(10,2) DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_route_day(route_id,day_number)) ENGINE=InnoDB;
CREATE TABLE travel_route_item (id BIGINT PRIMARY KEY AUTO_INCREMENT,route_day_id BIGINT NOT NULL,start_time VARCHAR(10),end_time VARCHAR(10),name VARCHAR(120) NOT NULL,type VARCHAR(30),destination_id BIGINT,address VARCHAR(255),transport VARCHAR(100),cost DECIMAL(10,2) DEFAULT 0,duration_minutes INT,description VARCHAR(800),image_url VARCHAR(255),sort_order INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_route_item_day(route_day_id,sort_order)) ENGINE=InnoDB;

CREATE TABLE travel_banner (id BIGINT PRIMARY KEY AUTO_INCREMENT,title VARCHAR(100),subtitle VARCHAR(255),image_url VARCHAR(255) NOT NULL,link_url VARCHAR(255),sort_order INT DEFAULT 0,enabled TINYINT DEFAULT 1,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE travel_recommendation (id BIGINT PRIMARY KEY AUTO_INCREMENT,position_code VARCHAR(50) NOT NULL,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,title VARCHAR(100),sort_order INT DEFAULT 0,start_time DATETIME,end_time DATETIME,enabled TINYINT DEFAULT 1,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,UNIQUE KEY uk_recommend(position_code,target_type,target_id)) ENGINE=InnoDB;
CREATE TABLE travel_search_history (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,keyword VARCHAR(100) NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_search_user(user_id,create_time),INDEX idx_search_keyword(keyword)) ENGINE=InnoDB;
CREATE TABLE travel_hot_keyword (id BIGINT PRIMARY KEY AUTO_INCREMENT,keyword VARCHAR(100) NOT NULL UNIQUE,search_count INT DEFAULT 0,sort_order INT DEFAULT 0,enabled TINYINT DEFAULT 1,create_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE travel_browse_history (id BIGINT PRIMARY KEY AUTO_INCREMENT,user_id BIGINT NOT NULL,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,duration_seconds INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_history_user(user_id,create_time)) ENGINE=InnoDB;
CREATE TABLE travel_message (id BIGINT PRIMARY KEY AUTO_INCREMENT,receiver_id BIGINT NOT NULL,sender_id BIGINT,title VARCHAR(120) NOT NULL,content VARCHAR(1000) NOT NULL,message_type VARCHAR(30),business_type VARCHAR(30),business_id BIGINT,is_read TINYINT DEFAULT 0,deleted TINYINT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_message_receiver(receiver_id,is_read,create_time)) ENGINE=InnoDB;
CREATE TABLE travel_report (id BIGINT PRIMARY KEY AUTO_INCREMENT,reporter_id BIGINT NOT NULL,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,reason VARCHAR(50) NOT NULL,description VARCHAR(500),status VARCHAR(20) DEFAULT 'PENDING',handler_id BIGINT,handle_note VARCHAR(500),handle_time DATETIME,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_report_status(status,create_time),INDEX idx_report_reporter(reporter_id,create_time),INDEX idx_report_target(target_type,target_id,status),INDEX idx_report_handler(handler_id,handle_time)) ENGINE=InnoDB;
CREATE TABLE travel_audit_record (id BIGINT PRIMARY KEY AUTO_INCREMENT,target_type VARCHAR(20) NOT NULL,target_id BIGINT NOT NULL,auditor_id BIGINT NOT NULL,decision VARCHAR(20) NOT NULL,opinion VARCHAR(500) NOT NULL,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,INDEX idx_audit_target(target_type,target_id)) ENGINE=InnoDB;
CREATE TABLE travel_announcement (id BIGINT PRIMARY KEY AUTO_INCREMENT,title VARCHAR(120) NOT NULL,summary VARCHAR(500),content TEXT,category VARCHAR(30),pinned TINYINT DEFAULT 0,enabled TINYINT DEFAULT 1,publish_time DATETIME,create_time DATETIME DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS=1;
