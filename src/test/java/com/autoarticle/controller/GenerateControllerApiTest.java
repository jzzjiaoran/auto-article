package com.autoarticle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 JSON 接口错误语义（JZZ-13）：
 * - 业务/校验失败映射到正确 HTTP 状态码与架构错误码（400/1001、404/1004、500/9000）
 * - 不再一律 HTTP 200 + code 500
 * - 参数校验生效（@Valid），不把 SQL 约束异常回显给客户端
 */
@SpringBootTest
@AutoConfigureMockMvc
class GenerateControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_404_with_code_1004_when_task_not_found() throws Exception {
        mockMvc.perform(get("/tasks/doesnotexist").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value(containsString("不存在")))
                .andExpect(jsonPath("$.message").value(not(containsString("ID: null"))));
    }

    @Test
    void should_return_400_with_code_1001_when_title_missing() throws Exception {
        mockMvc.perform(post("/articles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"style\":\"story\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.message").value(containsString("文章标题不能为空")))
                .andExpect(jsonPath("$.message").value(not(containsString("NULL not allowed"))))
                .andExpect(jsonPath("$.message").value(not(containsString("insert into articles"))));
    }

    @Test
    void should_return_404_with_code_1004_when_topic_not_found() throws Exception {
        mockMvc.perform(post("/articles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"topicId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1004))
                .andExpect(jsonPath("$.message").value(containsString("热点 不存在")));
    }

    @Test
    void should_return_400_with_code_1001_when_body_malformed() throws Exception {
        mockMvc.perform(post("/articles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    void should_return_200_with_code_0_when_create_valid() throws Exception {
        mockMvc.perform(post("/articles")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"测试文章\",\"style\":\"story\",\"length\":\"medium\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }
}
