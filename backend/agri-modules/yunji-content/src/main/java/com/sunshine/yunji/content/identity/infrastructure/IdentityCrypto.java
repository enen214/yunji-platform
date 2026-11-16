package com.sunshine.yunji.content.identity.infrastructure;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.sunshine.yunji.content.shared.BusinessException;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
@Component
public class IdentityCrypto {
 private final String hmac,encryption;
 public IdentityCrypto(@Value("${yunji.identity.hmac-key:}") String hmac,@Value("${yunji.identity.encryption-key:}") String encryption){this.hmac=hmac;this.encryption=encryption;}
 public String hash(String provider,String app,String subject){BusinessException.require(hmac.length()>=32,503,"身份密钥尚未配置");try{var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(hmac.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal((provider+"\0"+app+"\0"+subject).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new BusinessException(503,"身份密钥配置错误");}}
 public String encrypt(String subject){try{byte[] key=Base64.getDecoder().decode(encryption);BusinessException.require(key.length==32,503,"身份加密密钥需要256位");byte[] iv=new byte[12];new SecureRandom().nextBytes(iv);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));byte[] data=cipher.doFinal(subject.getBytes(StandardCharsets.UTF_8));byte[] out=new byte[iv.length+data.length];System.arraycopy(iv,0,out,0,iv.length);System.arraycopy(data,0,out,iv.length,data.length);return "v1:"+Base64.getEncoder().encodeToString(out);}catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException(503,"身份加密密钥尚未配置");}}
 /** 仅用于服务端发送（订阅消息需要原始 openid）。明文不落库、不出进程。 */
 public String decrypt(String payload){
  try {
   BusinessException.require(payload!=null&&payload.startsWith("v1:")&&payload.length()<=2048,503,"身份密文格式不正确");
   byte[] key=Base64.getDecoder().decode(encryption);
   BusinessException.require(key.length==32,503,"身份加密密钥需要256位");
   byte[] all=Base64.getDecoder().decode(payload.substring(3));
   BusinessException.require(all.length>12,503,"身份密文长度不正确");
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
   cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,all,0,12));
   byte[] plain=cipher.doFinal(all,12,all.length-12);
   return new String(plain,java.nio.charset.StandardCharsets.UTF_8);
  } catch (BusinessException e) { throw e; }
  catch (Exception e) { throw new BusinessException(503,"身份密文无法解密"); }
 }
}
