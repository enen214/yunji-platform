package com.sunshine.yunji.content.identity.application;

import com.sunshine.yunji.content.shared.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import com.sunshine.yunji.content.identity.infrastructure.*;

@Service
public class IdentityService {
    private final Db db;
    private final boolean local;
    private final String secret;
    private final IdentityCrypto crypto;
    private final com.sunshine.yunji.content.resource.application.FileService files;
    public IdentityService(Db db,IdentityCrypto crypto,com.sunshine.yunji.content.resource.application.FileService files,@Value("${yunji.local-enabled:false}") boolean local,@Value("${yunji.local-secret:}") String secret){this.db=db;this.crypto=crypto;this.files=files;this.local=local;this.secret=secret;}
    public boolean isLocal(){return local;}
    @Transactional public Map<String,Object> providerLogin(WechatGateway.Identity identity){
        String hash=crypto.hash("WECHAT",identity.appId(),identity.openid());
        var bound=db.rows("SELECT user_id FROM app_user_identity WHERE tenant_id='000000' AND provider='WECHAT' AND provider_app_id=? AND subject_hash=?",identity.appId(),hash);
        long user;
        if(!bound.isEmpty())user=Db.number(bound.get(0).get("userId"));
        else{
            // 手机号是账号唯一标识：新 openid 带着已登记的手机号登录时，复用原账号而不是再建一个。
            var byPhone=identity.phoneHash()==null?List.<Map<String,Object>>of():db.rows("SELECT id FROM app_user WHERE tenant_id='000000' AND phone_hash=? AND deleted_at IS NULL",identity.phoneHash());
            if(byPhone.isEmpty()){user=db.id();db.update("INSERT INTO app_user(id,nickname) VALUES(?,'新用户')",user);}
            else user=Db.number(byPhone.get(0).get("id"));
            db.update("INSERT INTO app_user_identity(id,user_id,provider,provider_app_id,subject_hash,encrypted_subject,key_version) VALUES(?,?,'WECHAT',?,?,?,1)",db.id(),user,identity.appId(),hash,crypto.encrypt(identity.openid()));
        }
        db.one("SELECT id FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user);
        if(identity.phoneMasked()!=null){
            db.update("UPDATE app_user SET phone_masked=? WHERE id=?",identity.phoneMasked(),user);
            // 手机号已被别的在用账号占用时不抢占，避免把两个账号的数据合到一起。
            if(identity.phoneHash()!=null&&db.count("SELECT COUNT(*) FROM app_user WHERE tenant_id='000000' AND phone_hash=? AND id<>? AND deleted_at IS NULL",identity.phoneHash(),user)==0)
                db.update("UPDATE app_user SET phone_hash=? WHERE id=?",identity.phoneHash(),user);
        }
        String token=db.token();db.update("INSERT INTO customer_session(token_hash,user_id,expires_at) VALUES(?,?,UTC_TIMESTAMP()+INTERVAL 2 HOUR)",Db.hash(token),user);db.update("UPDATE app_user SET last_login_at=UTC_TIMESTAMP() WHERE id=?",user);db.audit(user,"CUSTOMER","CUSTOMER_LOGIN","USER",user,Map.of("local",false,"provider","WECHAT","phoneAuthorized",identity.phoneMasked()!=null));return Map.of("accessToken",token,"expiresIn",7200,"userId",Long.toString(user),"realm","CUSTOMER");
    }
    @Transactional public Map<String,Object> localWechat(String code,String app){
        var result=localLogin(code,app);long user=Db.number(result.get("userId"));String hash=crypto.hash("LOCAL_WECHAT",app,Long.toString(user));
        db.update("INSERT IGNORE INTO app_user_identity(id,user_id,provider,provider_app_id,subject_hash,encrypted_subject,key_version) VALUES(?,?,'LOCAL_WECHAT',?,?,?,1)",db.id(),user,app,hash,crypto.encrypt("synthetic:"+user));return result;
    }
    @Transactional public Map<String,Object> issueCode(String supplied,long user,String app){
        BusinessException.require(local&&!secret.isBlank()&&java.security.MessageDigest.isEqual(secret.getBytes(),Objects.toString(supplied,"").getBytes()),404,"接口不存在");
        db.one("SELECT id FROM app_user WHERE id=? AND tenant_id='000000' AND status='ACTIVE' AND deleted_at IS NULL",user);
        String code=db.token();db.update("INSERT INTO local_login_code(code_hash,user_id,app_id,expires_at) VALUES(?,?,?,UTC_TIMESTAMP()+INTERVAL 2 MINUTE)",Db.hash(code),user,Db.text(app,100));
        return Map.of("code",code,"expiresIn",120);
    }
    @Transactional public Map<String,Object> localLogin(String code,String app){
        BusinessException.require(local,404,"本地登录未启用");
        var c=db.one("SELECT * FROM local_login_code WHERE code_hash=? AND app_id=? AND expires_at>UTC_TIMESTAMP() AND used_at IS NULL FOR UPDATE",Db.hash(Db.text(code,100)),app);
        long user=Db.number(c.get("userId"));db.one("SELECT id FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user);
        db.update("UPDATE local_login_code SET used_at=UTC_TIMESTAMP() WHERE code_hash=?",Db.hash(code));
        String token=db.token();db.update("INSERT INTO customer_session(token_hash,user_id,expires_at) VALUES(?,?,UTC_TIMESTAMP()+INTERVAL 2 HOUR)",Db.hash(token),user);
        db.update("UPDATE app_user SET last_login_at=UTC_TIMESTAMP() WHERE id=?",user);
        db.audit(user,"CUSTOMER","CUSTOMER_LOGIN","USER",user,Map.of("local",local));return Map.of("accessToken",token,"expiresIn",7200,"userId",Long.toString(user),"realm","CUSTOMER");
    }
    public Map<String,Object> profile(long user){return db.one("SELECT id,nickname,avatar_url,avatar_object_id,phone_masked,status FROM app_user WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL",user);}
    /** 头像上传：内容进本地对象库，只回写引用和地址，不存 base64。 */
    @Transactional public Map<String,Object> avatar(long user,org.springframework.web.multipart.MultipartFile file){
        profile(user);
        var object=files.upload(new com.sunshine.yunji.content.resource.application.FileService.Principal("CUSTOMER",user),file,"PRIVATE");
        long objectId=Db.number(object.get("id"));
        db.update("UPDATE app_user SET avatar_url=?,avatar_object_id=? WHERE id=?","/api/v1/me/avatar?v="+objectId,objectId,user);
        db.audit(user,"CUSTOMER","PROFILE_AVATAR","USER",user,Map.of("objectId",Long.toString(objectId)));
        return profile(user);
    }
    /** 头像读取：只能取自己名下的那张图。 */
    public com.sunshine.yunji.content.resource.application.FileService.Download avatar(long user){
        var row=profile(user);
        String objectId=Objects.toString(row.get("avatarObjectId"),"");
        BusinessException.require(!objectId.isBlank(),404,"尚未设置头像");
        return files.avatar(user,Long.parseLong(objectId));
    }
    /** 个人资料编辑：只改昵称。组织维度（2-09）已停用，不再接受 organizationId。 */
    @Transactional public Map<String,Object> profile(long user,String nickname){
        db.update("UPDATE app_user SET nickname=? WHERE id=?",Db.text(nickname,100),user);
        db.audit(user,"CUSTOMER","PROFILE_EDIT","USER",user,Map.of("changed",true));return profile(user);
    }
    @Transactional public void logout(String token){var session=db.one("SELECT user_id FROM customer_session WHERE token_hash=? FOR UPDATE",Db.hash(token));long user=Db.number(session.get("userId"));db.update("DELETE FROM customer_session WHERE token_hash=?",Db.hash(token));db.audit(user,"CUSTOMER","CUSTOMER_LOGOUT","USER",user,Map.of());}
}
