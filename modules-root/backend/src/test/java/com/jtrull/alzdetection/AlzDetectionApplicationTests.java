package com.jtrull.alzdetection;

import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
class AlzDetectionApplicationTests {
    @Autowired private MockMvc mvc;

    public static final int TEST_INVOCATIONS = 10;
    public static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testFakeEndpoint() throws Exception {
        mvc.perform(get("/api/v19/")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is4xxClientError());
	}
}
