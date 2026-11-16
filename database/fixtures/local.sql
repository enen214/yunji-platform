-- Synthetic data only. Loaded by disposable local runner, never production migrations.
UPDATE sys_user SET nick_name='本地管理员',email='',phonenumber='' WHERE user_id=1;
INSERT INTO app_user(id,nickname) VALUES(91000000000000001,'本地学生甲'),(91000000000000002,'本地学生乙'),(91000000000000003,'注销演练用户');
INSERT INTO organization(id,parent_id,type,name,code) VALUES(901,0,'SCHOOL','合成大学','SYNTHETIC'),(902,901,'COLLEGE','合成学院','SYNTHETIC_COLLEGE');
INSERT INTO keyword(id,display_name,normalized_name) VALUES(911,'合成考试','合成考试'),(912,'合成活动','合成活动');
