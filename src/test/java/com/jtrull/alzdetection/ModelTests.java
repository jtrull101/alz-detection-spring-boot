package com.jtrull.alzdetection;


import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;


@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class ModelTests {
    @Autowired
	private MockMvc mvc;

	@Autowired
	private ModelRepository modelRepository;

	@Autowired
	private ModelService modelService;

    private static final String BASE_URL = "/api/v1/model";

    /**
     * Load model from file passed into REST reqeust
     * @throws Exception
     */
    @Test
	@Order(1)
	public void testAddLoadedModel() throws Exception {
		Optional<File> savedModel = modelService.getSavedModelInResourcesDir("model/");
		if (savedModel.isEmpty()) {
			throw new Exception("Unable to find saved model");
		}

		// MockMultipartFile mmf = new MockMultipartFile("file", "saved_model.pb", "multipart/mixed",
		// 		new FileInputStream(savedModel.get()));
		byte[] model = IOUtils.toByteArray(new FileInputStream(savedModel.get()));

		MockMultipartFile mmf = new MockMultipartFile("file", model);
		MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders
				.multipart(BASE_URL +"/load/").file("file", mmf.getBytes());

		mvc.perform(multipartRequest)
				.andExpect(status().isOk())
				.andReturn();
	}

    /**
     * Load default pre-installed model
     * @throws Exception
     */
	@Test
	@Order(1)
	public void testAddDefaultModel() throws Exception {
		mvc.perform(post(BASE_URL +"/load/default")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isOk());
	}

	/**
     * Get model by specified modelID
     * @throws Exception
     */
	@Test 
	@Order(2)
	public void testGetModel() throws Exception {
		// Grab the first model and assert we can get it
		Model model = modelRepository.findAll().get(0);
		if (model == null) {
			throw new Exception("unable to run delete if no model exists yet");
		}
		mvc.perform(get(BASE_URL + model.getId().toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

    /**
     * Get all models regardles of ID
     * @throws Exception
     */
    @Test
    @Order(2)
    public void testGetAllModels() throws Exception {
		mvc.perform(get(BASE_URL + "/all")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

    /**
     * Delete model of specified ID
     * @throws Exception
     */
	@Test
	@Order(3)
	public void testDeleteModel() throws Exception {
		// Grab the first model and delete it
		Model model = modelRepository.findAll().get(0);
		if (model == null) {
			throw new Exception("unable to run delete if no model exists yet");
		}

		MvcResult result = mvc.perform(delete(BASE_URL + "/delete/" + model.getId().toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = result.getResponse().getContentAsString();
		assert Boolean.valueOf(content);
	}

    /** 
     * Delete all models present for the API
     */
    @Test
    @Order(4)
    public void testDeleteAllModels() throws Exception {
        MvcResult result = mvc.perform(delete(BASE_URL + "/delete/all")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
        String content = result.getResponse().getContentAsString();
		assert Boolean.valueOf(content);
    }
}
