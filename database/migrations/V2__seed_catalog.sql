INSERT INTO content_category (id, code, name, sort_order) VALUES
(1001, 'COMPETITION', '竞赛双创', 10),
(1002, 'ACADEMIC', '学业修读', 20),
(1003, 'LIFE', '生活服务', 30),
(1004, 'NOTICE', '通知公告', 40),
(1005, 'FURTHER_EDU', '升学发展', 50),
(1006, 'CAREER', '就业求职', 60),
(1007, 'CAMPUS', '校园事务', 70)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order);

INSERT INTO content_page (id, page_key, title, content) VALUES
(2001, 'user_agreement', '用户协议', '<p>请在上线前替换为经确认的协议正文。</p>'),
(2002, 'privacy_policy', '隐私政策', '<p>请在上线前替换为经确认的隐私政策。</p>'),
(2003, 'about_us', '关于云迹', '<p>请在上线前替换为正式介绍。</p>')
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content);

