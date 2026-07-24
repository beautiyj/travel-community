package com.gnagnoohc.travel.config;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.gnagnoohc.travel.reservation.scheduler.ReservationExpireScheduler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SpringBootTest(properties = {
        "kakao.oauth.client-id=test-client-id",
        "kakao.oauth.client-secret=test-client-secret",
        "file.upload-dir=build/test-upload",
        "file.upload-community=build/test-upload",
        "toss.secret-key=test",
        "toss.client-key=test",
        "kakaopay.secret-key=test",
        "kakaopay.cid=test"
})
@AutoConfigureMockMvc
class SecurityConfigOAuth2EndpointTest {

    @MockitoBean
    private ReservationExpireScheduler reservationExpireScheduler;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    void 표준_인가_시작경로는_카카오로_리다이렉트한다() throws Exception {
        mockMvc.perform(get("http://localhost:9999/oauth2/authorization/kakao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString(
                        "https://kauth.kakao.com/oauth/authorize")))
                .andExpect(header().string("Location", containsString(
                        "redirect_uri=http://localhost:9999/auth/callback/kakao")));
    }

    @Test
    void 새_callback은_Spring_OAuth2_필터가_처리한다() throws Exception {
        mockMvc.perform(get("/auth/callback/kakao")
                        .param("code", "invalid-code")
                        .param("state", "invalid-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?socialError=true"));
    }

    @Test
    void 이전_수동_callback은_더_이상_매핑되지_않는다() throws Exception {
        mockMvc.perform(get("/auth/kakao/callback"))
                .andExpect(status().isNotFound());
    }

    @Test
    void Google과_Naver는_등록되지_않는다() {
        assertNull(clientRegistrationRepository.findByRegistrationId("google"));
        assertNull(clientRegistrationRepository.findByRegistrationId("naver"));
    }
}
