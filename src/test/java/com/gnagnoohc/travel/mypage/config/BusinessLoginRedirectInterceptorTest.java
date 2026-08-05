// package com.gnagnoohc.travel.mypage.config;

// import static org.assertj.core.api.Assertions.assertThat;

// import org.junit.jupiter.api.Test;
// import org.springframework.mock.web.MockHttpServletRequest;
// import org.springframework.mock.web.MockHttpServletResponse;
// import org.springframework.web.servlet.ModelAndView;

// class BusinessLoginRedirectInterceptorTest {

//     private final BusinessLoginRedirectInterceptor interceptor =
//             new BusinessLoginRedirectInterceptor();

//     @Test
//     void businessLoginRedirectsToMainPage() {
//         ModelAndView modelAndView =
//                 new ModelAndView("redirect:/business/dashboard");

//         interceptor.postHandle(
//                 new MockHttpServletRequest("POST", "/auth/login"),
//                 new MockHttpServletResponse(),
//                 new Object(),
//                 modelAndView);

//         assertThat(modelAndView.getViewName()).isEqualTo("redirect:/");
//     }

//     @Test
//     void otherLoginResultsRemainUnchanged() {
//         ModelAndView adminRedirect = new ModelAndView("redirect:/admin");
//         ModelAndView loginFailure = new ModelAndView("auth/login");

//         interceptor.postHandle(
//                 new MockHttpServletRequest("POST", "/auth/login"),
//                 new MockHttpServletResponse(),
//                 new Object(),
//                 adminRedirect);
//         interceptor.postHandle(
//                 new MockHttpServletRequest("POST", "/auth/login"),
//                 new MockHttpServletResponse(),
//                 new Object(),
//                 loginFailure);

//         assertThat(adminRedirect.getViewName()).isEqualTo("redirect:/admin");
//         assertThat(loginFailure.getViewName()).isEqualTo("auth/login");
//     }
// }
