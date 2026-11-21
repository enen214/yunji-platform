package com.sunshine.yunji.content.identity.application;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.sunshine.yunji.content.shared.Db;
import cn.dev33.satoken.secure.BCrypt;
import java.util.Set;
@Component
public class ProductionGuard implements ApplicationRunner {
 private final Environment env;private final Db db;
 public ProductionGuard(Environment env,Db db){this.env=env;this.db=db;}
 public void run(ApplicationArguments args){
  if(env.matchesProfiles("local")){
   if(env.matchesProfiles("prod"))throw new IllegalStateException("Production and local profiles must not be combined");
   String address=env.getProperty("server.address","");
   if(!Set.of("127.0.0.1","::1","localhost").contains(address))throw new IllegalStateException("Local synthetic runtime must bind loopback");
   return;
  }
  if(env.getProperty("yunji.local-enabled",Boolean.class,false))throw new IllegalStateException("Production must disable local gateways");
  String secret=env.getProperty("sa-token.jwt-secret-key","");if(secret.length()<48)throw new IllegalStateException("Production token secret too short");
  var admin=db.one("SELECT password FROM sys_user WHERE user_id=1");String initial=admin.get("password").toString();if(initial.equals("!UNINITIALIZED!")||BCrypt.checkpw("admin123",initial))throw new IllegalStateException("Replace the official bootstrap admin password before production startup");
  if(db.count("SELECT COUNT(*) FROM sys_user WHERE user_id IN(3,4) AND status='0'")>0)throw new IllegalStateException("Disable official synthetic framework accounts before production startup");
 }
}
