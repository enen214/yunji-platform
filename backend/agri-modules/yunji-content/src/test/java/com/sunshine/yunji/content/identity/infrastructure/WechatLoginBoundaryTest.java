package com.sunshine.yunji.content.identity.infrastructure;

import com.sunshine.yunji.content.identity.api.IdentityController;
import com.sunshine.yunji.content.identity.application.IdentityService;
import com.sunshine.yunji.content.identity.application.Actors;
import com.sunshine.yunji.content.notification.application.ReminderTrackingService;
import com.sunshine.yunji.content.shared.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WechatLoginBoundaryTest {
    @Test void nativeEndpointNeverRoutesToSyntheticLoginEvenInLocalMode() {
        var service = mock(IdentityService.class); var gateway = mock(WechatGateway.class);
        var controller = new IdentityController(service, mock(Actors.class), gateway);
        when(service.isLocal()).thenReturn(true);
        when(gateway.exchange("code", "app", "phone")).thenThrow(new BusinessException(401, "invalid"));
        assertThrows(BusinessException.class, () -> controller.nativeWechat(new IdentityController.NativeWechatLoginRequest("code", "app", "phone")));
        verifyNoInteractions(service);
    }

    @Test void providerLoginUsesVerifiedMaskAndRealAuditWithoutReturningPhoneOrOpenid() {
        var db = mock(Db.class); var crypto = mock(IdentityCrypto.class);
        when(crypto.hash("WECHAT", "app", "openid")).thenReturn("subject-hash");
        when(db.rows(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("userId", "71")));
        when(db.one(anyString(), any(Object[].class))).thenReturn(Map.of("id", "71"));
        when(db.token()).thenReturn("synthetic-session");
        var service = new IdentityService(db, mock(ReminderTrackingService.class), crypto, true, "local-secret");
        var session = service.providerLogin(new WechatGateway.Identity("app", "openid", "138****1234"));
        assertEquals("71", session.get("userId"));
        assertEquals(Set.of("accessToken", "expiresIn", "userId", "realm"), session.keySet());
        verify(db).update("UPDATE app_user SET phone_masked=? WHERE id=?", "138****1234", 71L);
        verify(db).audit(71L, "CUSTOMER", "CUSTOMER_LOGIN", "USER", 71L, Map.of("local", false, "provider", "WECHAT", "phoneAuthorized", true));
        clearInvocations(db);
        service.providerLogin(new WechatGateway.Identity("app", "openid"));
        verify(db, never()).update(eq("UPDATE app_user SET phone_masked=? WHERE id=?"), any(Object[].class));
    }
}
