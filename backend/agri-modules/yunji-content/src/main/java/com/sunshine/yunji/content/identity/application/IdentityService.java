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
    private final com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders;
    private final boolean local;
    private final String secret;
    private final IdentityCrypto crypto;
    public IdentityService(Db db,com.sunshine.yunji.content.notification.application.ReminderTrackingService reminders,IdentityCrypto crypto,@Value("${yunji.local-enabled:false}") boolean local,@Value("${yunji.local-secret:}") String secret){this.db=db;this.reminders=reminders;this.crypto=crypto;this.local=local;this.secret=secret;}
    public boolean isLocal(){return local;}
    @Transactional public Map<String,Object> providerLogin(WechatGateway.Identity identity){
        String hash=crypto.hash("WECHAT",identity.appId(),identity.openid());var found=db.rows("SELECT user_id FROM app_user_identity WHERE tenant_id='000000' AND provider='WECHAT' AND provider_app_id=? AND subject_hash=?",identity.appId(),hash);long user;
        if(found.isEmpty()){user=db.id();db.update("INSERT INTO app_user(id,nickname) VALUES(?,'新用户')",user);db.update("INSERT INTO app_user_identity(id,user_id,provider,provider_app_id,subject_hash,encrypted_subject,key_version) VALUES(?,?,'WECHAT',?,?,?,1)",db.id(),user,identity.appId(),hash,crypto.encrypt(identity.openid()));}
        else user=Db.number(found.get(0).get("userId"));
        db.one("SELECT id FROM app_user WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL",user);
        if(identity.phoneMasked()!=null)db.update("UPDATE app_user SET phone_masked=? WHERE id=?",identity.phoneMasked(),user);
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
    public Map<String,Object> profile(long user){return db.one("SELECT id,nickname,avatar_url,phone_masked,organization_id,status FROM app_user WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL",user);}
    @Transactional public Map<String,Object> profile(long user,String nickname,Long organization){
        if(organization!=null)db.one("SELECT id FROM organization WHERE id=? AND tenant_id='000000' AND deleted_at IS NULL AND status='ENABLED'",organization);
        db.update("UPDATE app_user SET nickname=?,organization_id=? WHERE id=?",Db.text(nickname,100),organization,user);
        db.audit(user,"CUSTOMER","PROFILE_EDIT","USER",user,Map.of("changed",true));return profile(user);
    }
    @Transactional public void logout(String token){var session=db.one("SELECT user_id FROM customer_session WHERE token_hash=? FOR UPDATE",Db.hash(token));long user=Db.number(session.get("userId"));db.update("DELETE FROM customer_session WHERE token_hash=?",Db.hash(token));db.audit(user,"CUSTOMER","CUSTOMER_LOGOUT","USER",user,Map.of());}
    @Transactional public void consent(long user,long version,String type){
        db.one("SELECT v.id FROM content_page_version v JOIN content_page p ON p.id=v.page_id WHERE v.id=? AND p.tenant_id='000000'",version);
        BusinessException.require(Set.of("TERMS","PRIVACY").contains(type),422,"协议类型错误");
        db.update("INSERT IGNORE INTO user_consent(user_id,page_version_id,consent_type) VALUES(?,?,?)",user,version,type);
        db.audit(user,"CUSTOMER","CONSENT_ACCEPT","PAGE_VERSION",version,Map.of("type",type));
    }
    @Transactional public void close(long user,String confirmation){
        reminders.lock();
        BusinessException.require("注销账号".equals(confirmation),422,"需要明确确认注销");
        db.update("UPDATE app_user SET status='CLOSED',nickname=NULL,avatar_url=NULL,phone_masked=NULL,deleted_at=UTC_TIMESTAMP() WHERE id=?",user);
        db.update("DELETE FROM customer_session WHERE user_id=?",user);
        db.update("DELETE FROM customer_search_history WHERE user_id=?",user);
        db.update("DELETE FROM activity_favorite WHERE user_id=?",user);
        db.update("UPDATE user_follow SET deleted_at=UTC_TIMESTAMP() WHERE user_id=?",user);
        db.update("UPDATE notification_rule SET enabled=0 WHERE user_id=?",user);
        reminders.close(user);
        db.update("UPDATE notification_binding SET binding_state='CLOSED' WHERE user_id=?",user);
        db.audit(user,"CUSTOMER","ACCOUNT_CLOSE","USER",user,Map.of("closed",true));db.event("USER",user,"ACCOUNT_CLOSED",Map.of("userId",Long.toString(user)));
    }
}
