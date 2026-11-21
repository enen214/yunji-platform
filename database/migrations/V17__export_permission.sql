-- Export is a separate grant; no ordinary staff role receives it implicitly.
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,visible,status,perms,icon,create_dept,create_by,create_time)
VALUES(9518,'云迹数据导出',9500,18,'export',NULL,'F','0','0','yunji:export','#',103,1,UTC_TIMESTAMP());
