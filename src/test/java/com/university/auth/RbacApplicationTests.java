package com.university.auth;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
    @Disabled
class RbacApplicationTests {
    @Autowired
    private MockMvc mockMvc;

     @Test
    @WithMockUser(roles = "USER")
    void userCanAccessUserEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/user-endpoint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("USER_ENDPOINT_OK"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/admin-endpoint"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/admin-endpoint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ADMIN_ENDPOINT_OK"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminBrowserEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/admin-browser-endpoint"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin point"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCanAccessUserBrowserEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/user-browser-endpoint"))
                .andExpect(status().isOk())
                .andExpect(content().string("user point"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotAccessUserBrowserEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/user-browser-endpoint"))
                .andExpect(status().isForbidden());
    }

    @WithMockUser(roles = {"USER", "ADMIN"})
    void userAdminCombinedRoleCannotAccessUserBrowserEndpoint() throws Exception {
        mockMvc.perform(get("/api/protected/user-browser-endpoint"))
                .andExpect(status().isForbidden());
    }
    
}
