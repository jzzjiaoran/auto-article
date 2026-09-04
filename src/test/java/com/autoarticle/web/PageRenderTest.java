package com.autoarticle.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allMainPagesRender() throws Exception {
        String[] paths = {"/", "/hot-topics", "/articles", "/generate", "/publish-records", "/platform-accounts", "/platform-accounts/new"};
        for (String path : paths) {
            mockMvc.perform(get(path).accept("text/html"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("auto-article")));
        }
    }

    @Test
    void errorPagesRenderWithoutServerError() throws Exception {
        mockMvc.perform(get("/articles/999999").accept("text/html"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auto-article")));
        mockMvc.perform(get("/platform-accounts/999999/edit").accept("text/html"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("auto-article")));
    }
}
