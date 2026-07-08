package com.april.studentmanagementproject.config;

import com.april.studentmanagementproject.controller.StudentController;
import com.april.studentmanagementproject.dto.StudentDto;
import com.april.studentmanagementproject.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudentController.class)
@Import({SecurityConfig.class, GoogleOAuth2AuthoritiesMapper.class})
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-client-secret",
        "app.security.admin-emails=jackiewangjiayi513@gmail.com"
})
class StudentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void studentApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadStudents() throws Exception {
        when(studentService.getAllStudents()).thenReturn(List.of());

        mockMvc.perform(get("/api/students")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void regularUserCannotCreateStudent() throws Exception {
        mockMvc.perform(post("/api/students")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "April",
                                  "lastName": "Wang",
                                  "email": "april@example.com"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanCreateStudent() throws Exception {
        when(studentService.addStudent(any(StudentDto.class)))
                .thenReturn(new StudentDto(1L, "April", "Wang", "april@example.com"));

        mockMvc.perform(post("/api/students")
                        .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "April",
                                  "lastName": "Wang",
                                  "email": "april@example.com"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
