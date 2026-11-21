-- Framework users 3 and 4 are repurposed as synthetic reviewer / creator.
UPDATE sys_user SET nick_name='合成审核员',email='',phonenumber='' WHERE user_id=3;
UPDATE sys_user SET nick_name='合成创作者',email='',phonenumber='' WHERE user_id=4;
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,menu_type,visible,status,perms) VALUES
(9001,'内容读取',0,1,'yunji-read','F','0','0','yunji:article:read'),
(9002,'内容编辑',0,2,'yunji-write','F','0','0','yunji:article:write'),
(9003,'内容审核',0,3,'yunji-review','F','0','0','yunji:article:review'),
(9004,'内容发布',0,4,'yunji-publish','F','0','0','yunji:article:publish');
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(4,9001),(4,9002),(3,9001),(3,9003),(3,9004),(3,9507);
INSERT INTO staff_scope(user_id,department_id) VALUES(3,102);
