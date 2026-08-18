USE `travel_share`;
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO sys_role(id,name,code,description,sort_order) VALUES
(1,'系统管理员','ADMIN','平台全部管理权限',1),(2,'内容审核员','AUDITOR','内容审核与举报处理',2),(3,'旅游创作者','CREATOR','发布攻略与路线',3),(4,'普通用户','USER','浏览、互动与创建个人行程',4);

SET @pwd='$2a$10$AlJjnSqaShBRUPsos.X.JuHrqbst6hgoUbjvpyB23mjzPCWKUWAFe';
INSERT INTO sys_user(id,username,password,nickname,email,avatar,role,status,city,bio,preferences,cover_image,visited_cities,guide_count,route_count,follower_count,following_count,received_likes,deleted) VALUES
(1,'admin',@pwd,'山海迹管理员','admin@example.com','/uploads/demo/avatars/avatar-001.png','ADMIN',1,'杭州','负责平台运营与系统管理','城市漫步,人文历史','/uploads/demo/banners/banner-001.png',28,0,0,0,0,0,0),
(2,'auditor01',@pwd,'内容守望者','auditor01@example.com','/uploads/demo/avatars/avatar-002.png','AUDITOR',1,'成都','认真对待每一份旅行分享','美食,古城','/uploads/demo/banners/banner-002.png',19,0,0,0,0,0,0),
(3,'creator01',@pwd,'北纬三十度','creator01@example.com','/uploads/demo/avatars/avatar-003.png','CREATOR',1,'重庆','用脚步记录山城、古镇与远方。','摄影,徒步,城市漫步','/uploads/demo/banners/banner-003.png',42,0,0,12800,188,0,0),
(4,'user01',@pwd,'周末出发','user01@example.com','/uploads/demo/avatars/avatar-004.png','USER',1,'上海','把每个周末过成一段小旅行。','周末游,美食,低预算','/uploads/demo/banners/banner-004.png',16,0,0,36,52,0,0);
INSERT INTO sys_user_role(user_id,role_id) VALUES(1,1),(2,2),(3,3),(4,4);

INSERT INTO sys_menu(id,parent_id,name,path,permission,icon,sort_order) VALUES
(1,0,'数据看板','/dashboard','dashboard:view','DataAnalysis',1),(2,0,'内容管理','/content',NULL,'Document',2),(3,2,'目的地管理','/destinations','destination:list','Location',1),(4,2,'攻略管理','/guides','guide:list','Reading',2),(5,2,'攻略审核','/audits','audit:list','Checked',3),(6,2,'路线管理','/routes','route:list','Guide',4),(7,2,'专题与标签','/topics','topic:list','CollectionTag',5),(8,2,'评论管理','/comments','comment:list','ChatDotRound',6),(9,0,'用户管理','/users','user:list','User',3),(10,0,'举报处理','/reports','report:list','Warning',4),(11,0,'运营管理','/operations','operation:list','Promotion',5),(12,0,'系统管理','/system','system:list','Setting',6),(13,0,'日志中心','/logs','log:list','Tickets',7);
INSERT INTO sys_role_menu SELECT 1,id FROM sys_menu;
INSERT INTO sys_role_menu SELECT 2,id FROM sys_menu WHERE id IN(1,2,4,5,8,10);

INSERT INTO sys_config(config_key,config_value,config_name,config_type,remark) VALUES
('site.name','山海迹','平台名称','STRING','C 端展示名称'),('site.slogan','把远方写成可以出发的计划','平台标语','STRING','首页标语'),('guide.audit.enabled','true','攻略审核开关','BOOLEAN','创作者内容需审核'),('upload.maxMb','10','图片上传上限','NUMBER','单位 MB');

INSERT INTO travel_tag(id,name,code,type,use_count,sort_order) VALUES
(1,'小众旅行','niche','GUIDE',380,1),(2,'城市漫步','city-walk','GUIDE',920,2),(3,'美食','food','GUIDE',1180,3),(4,'摄影','photo','GUIDE',760,4),(5,'亲子','family','GUIDE',420,5),(6,'情侣','couple','GUIDE',510,6),(7,'自驾','road-trip','ROUTE',680,7),(8,'学生党','student','GUIDE',840,8),(9,'低预算','budget','GUIDE',960,9),(10,'周末游','weekend','GUIDE',1240,10),(11,'海岛','island','DESTINATION',580,11),(12,'古镇','ancient-town','DESTINATION',630,12),(13,'徒步','hiking','ROUTE',710,13),(14,'自然风光','nature','DESTINATION',1020,14),(15,'人文历史','culture','GUIDE',880,15),(16,'夜景','night','GUIDE',540,16),(17,'春季旅行','spring','GUIDE',450,17),(18,'夏季避暑','summer','GUIDE',660,18),(19,'秋季赏景','autumn','GUIDE',620,19),(20,'冬季旅行','winter','GUIDE',590,20);

INSERT INTO travel_topic(id,name,subtitle,cover_image,summary,content,recommended,enabled,sort_order) VALUES
(1,'毕业旅行季','趁青春，和同学一起走向山海','/uploads/demo/topics/topic-001.png','适合学生团队的高性价比毕业旅行目的地与路线。','从预算、交通与同行节奏出发，整理适合毕业旅行的真实方案。',1,1,1),
(2,'周末两天一夜','不用请假也能抵达的小远方','/uploads/demo/topics/topic-002.png','城市周边短途灵感，周五下班也能出发。','轻装出发，把有限时间留给最值得的体验。',1,1,2),
(3,'夏日去看海','沿着海风寻找松弛感','/uploads/demo/topics/topic-003.png','海岛、海湾与滨海城市的清爽路线。','注意防晒与天气变化，行程安排留出弹性。',1,1,3),
(4,'古城慢游','在青石板路上读懂时间','/uploads/demo/topics/topic-004.png','古城、古镇与历史街区的深度漫游。','避开匆忙打卡，跟随街巷肌理慢慢行走。',1,1,4),
(5,'一路向西','高原、戈壁与辽阔公路','/uploads/demo/topics/topic-005.png','适合自驾与摄影爱好者的西部路线。','尊重自然、量力而行，并关注高原适应。',1,1,5),
(6,'为美食出发','从早市吃到夜宵','/uploads/demo/topics/topic-006.png','用味觉认识一座城市。','小店与市场往往最能呈现当地生活。',1,1,6),
(7,'秋日摄影地图','把金色与层林尽染装进镜头','/uploads/demo/topics/topic-007.png','适合秋季出发的风光摄影路线。','清晨和傍晚光线柔和，也要注意保暖。',1,1,7),
(8,'亲子轻旅行','让大人和孩子都不累','/uploads/demo/topics/topic-008.png','低强度、体验丰富的家庭路线。','每天只安排少量重点，给孩子留出休息时间。',1,1,8),
(9,'学生党低预算','把钱花在真正喜欢的体验上','/uploads/demo/topics/topic-009.png','交通、住宿和餐饮的实用省钱方法。','低预算不等于降低安全与卫生标准。',1,1,9),
(10,'城市漫步计划','用双脚认识街区','/uploads/demo/topics/topic-010.png','适合步行和公共交通的城市路线。','从一条街、一座市场和一片老社区开始。',1,1,10),
(11,'山水之间','湖泊、峡谷与森林','/uploads/demo/topics/topic-011.png','亲近自然的轻徒步与观景路线。','遵守景区规则，不进入未开放区域。',0,1,11),(12,'冬日限定','雪国、温泉与暖食','/uploads/demo/topics/topic-012.png','属于冬天的旅行仪式感。','关注路况与保暖，预留交通延误时间。',0,1,12),(13,'情侣纪念旅行','一起收藏沿途的小事','/uploads/demo/topics/topic-013.png','节奏舒缓、适合双人体验的路线。','不追求景点数量，重视共同体验。',0,1,13),(14,'自驾看中国','把风景串成一条路','/uploads/demo/topics/topic-014.png','适合新手与进阶驾驶者的自驾攻略。','不疲劳驾驶，提前确认车况和补给。',0,1,14),(15,'第一次自由行','从订票到返程的完整清单','/uploads/demo/topics/topic-015.png','给自由行新手的基础方法。','把重要信息离线保存，并准备备选方案。',0,1,15),(16,'岭南风物','骑楼、早茶与海风','/uploads/demo/topics/topic-016.png','华南城市的人文与味觉体验。','从街区日常进入地方文化。',0,1,16),(17,'江南寻梦','园林、水巷与烟雨','/uploads/demo/topics/topic-017.png','适合慢慢走的江南小城。','错峰出发，清晨是古镇最安静的时刻。',0,1,17),(18,'高铁直达','不自驾也能轻松抵达','/uploads/demo/topics/topic-018.png','公共交通友好的目的地合集。','住宿尽量靠近公共交通节点。',0,1,18),(19,'一人旅行','独处也自在的出发方式','/uploads/demo/topics/topic-019.png','安全、轻松的一人旅行建议。','保持联系，避免在陌生区域深夜独行。',0,1,19),(20,'春日花事','在最好的花期去见一座城','/uploads/demo/topics/topic-020.png','春季赏花与踏青路线。','花期受天气影响，请以当地最新信息为准。',0,1,20);

INSERT INTO travel_banner(id,title,subtitle,image_url,link_url,sort_order,enabled) VALUES
(1,'去大理，等一场洱海日落','五天慢旅行，把时间留给风和云','/uploads/demo/banners/banner-001.png','/destination/18',1,1),(2,'成都，不止是美食','从老街茶馆到山野秘境','/uploads/demo/banners/banner-002.png','/destination/5',2,1),(3,'秋天的北京适合步行','胡同、银杏与城市中轴线','/uploads/demo/banners/banner-003.png','/destination/1',3,1),(4,'沿着海岸线出发','厦门、泉州与闽南旧时光','/uploads/demo/banners/banner-004.png','/topic/3',4,1),(5,'第一次去新疆','把辽阔写进十天的公路计划','/uploads/demo/banners/banner-005.png','/topic/5',5,1),(6,'雪落哈尔滨','冰雪、暖食与冬日夜景','/uploads/demo/banners/banner-006.png','/destination/29',6,1),(7,'周末去苏州','园林外，还有可以慢慢走的街巷','/uploads/demo/banners/banner-007.png','/destination/9',7,1),(8,'毕业旅行去哪里','给青春一张通往山海的车票','/uploads/demo/banners/banner-008.png','/topic/1',8,1);

INSERT INTO travel_hot_keyword(keyword,search_count,sort_order) VALUES('成都三天两夜',9820,1),('大理环洱海',8760,2),('北京秋季路线',7920,3),('学生党厦门',7450,4),('重庆美食',7100,5),('苏州周末游',6820,6),('新疆自驾',6400,7),('哈尔滨冬季',5980,8),('青岛看海',5550,9),('长沙美食',5320,10),('杭州城市漫步',4980,11),('西安人文',4720,12),('三亚亲子',4510,13),('敦煌摄影',4200,14),('桂林山水',3980,15),('泉州古城',3760,16),('上海Citywalk',3550,17),('云南毕业旅行',3320,18),('低预算旅行',3100,19),('周末两天一夜',2980,20);

INSERT INTO travel_announcement(title,summary,content,category,pinned,enabled,publish_time) VALUES
('山海迹内容公约','真实分享，友善交流。','请尊重原创，不发布广告、虚假信息或侵犯他人权益的内容。','RULE',1,1,NOW()),('攻略投稿指南','一篇好攻略应当有清晰路线和真实体验。','建议说明时间、预算、交通与注意事项，避免绝对化的实时票价信息。','GUIDE',1,1,NOW()),('暑期出行安全提醒','关注天气与客流变化。','高温、强降雨等天气下请及时调整计划。','SAFETY',0,1,NOW()),('本地图片上传说明','支持常见图片格式。','图片应为本人原创或拥有合法使用权。','HELP',0,1,NOW()),('文明旅行倡议','把美景留给后来的人。','遵守当地规定，不进入未开放区域。','NOTICE',0,1,NOW()),('账户安全提示','请妥善保管登录信息。','不要向他人透露密码。','SECURITY',0,1,NOW()),('路线复制功能上线','公开路线可以保存为私人副本。','复制后可按照自己的时间与预算重新编辑。','FEATURE',0,1,NOW()),('评论区友善交流提醒','不同旅行方式都值得尊重。','请围绕内容讨论，避免人身攻击。','RULE',0,1,NOW()),('春季花期提示','花期会随天气变化。','出发前请以当地最新公告为准。','TRAVEL',0,1,NOW()),('高原旅行提示','合理安排行程，量力而行。','初到高原时避免剧烈运动。','SAFETY',0,1,NOW()),('海边旅行提示','注意防晒和潮汐变化。','不要进入缺少安全保障的水域。','SAFETY',0,1,NOW()),('冬季自驾提示','提前确认路况与车辆状态。','冰雪路段谨慎驾驶，预留更多时间。','SAFETY',0,1,NOW()),('投稿审核时效说明','审核结果通过站内消息通知。','内容被驳回后可根据意见修改并重新提交。','HELP',0,1,NOW()),('举报处理说明','每一条举报都会进入后台流程。','请准确选择原因并补充必要说明。','HELP',0,1,NOW()),('欢迎来到山海迹','把远方写成可以出发的计划。','愿每一份认真分享，都能照亮另一个人的旅程。','NOTICE',0,1,NOW());

COMMIT;
