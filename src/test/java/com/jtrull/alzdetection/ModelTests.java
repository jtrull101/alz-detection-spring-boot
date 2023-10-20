package com.jtrull.alzdetection;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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

	private int modelNum = 0;

    /**
     * Load model from file passed into REST reqeust
     * @throws Exception
     */
    @Test
	@Order(1)
	@RepeatedTest(10)
	public void testLoadModel() throws Exception {
		Optional<File> savedModel = modelService.getSavedModelInResourcesDir(ModelService.DEFAULT_MODEL_NAME);
		if (savedModel.isEmpty()) {
			throw new Exception("Unable to find saved model");
		}

		String path = savedModel.get().getAbsolutePath();
        String filename = path.substring(path.lastIndexOf("/")+1);
		String modelName = modelNum + "-" + filename;
		FileInputStream fis = new FileInputStream(path);

		try (InputStream is = getClass().getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("file", modelNum + "-" + filename, "application/zip", ByteStreams.toByteArray(fis));

			String url = BASE_URL + "/load";
			MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(url)
						.file(mockMultipartFile)
						.contentType("application/zip"))
					.andExpect(status().isOk())
					.andReturn();

			String content = _return.getResponse().getContentAsString();

			assert content != null;
			ObjectMapper mapper = new ObjectMapper();
			Model model = mapper.readValue(content, Model.class);
			assert model != null;
			assert model.getName().equals(modelName);
		} 
	}

	/**
     * Get model by specified modelID
     * @throws Exception
     */
	@Test 
	@Order(2)
	@RepeatedTest(10)
	public void testGetModel() throws Exception {
		// Grab the first model and assert we can get it
		Model model = modelRepository.findAll().get(0);
		if (model == null) {
			throw new Exception("unable to run get if no model exists yet");
		}
		MvcResult response = mvc.perform(get(BASE_URL + "/" + model.getId().toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = response.getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		Model readModel = mapper.readValue(content, Model.class);
		// filepath nullified during serialize
		model.setFilepath(null);
		assert model.equals(readModel);
	}

    /**
     * Get all models regardles of ID
     * @throws Exception
     */
    @Test
    @Order(2)
	@RepeatedTest(10)
    public void testGetAllModels() throws Exception {
		// Get all models and verify an array of Models is returned
		MvcResult _return = mvc.perform(get(BASE_URL + "/all")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = _return.getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();

		Model[] models = mapper.readValue(content, Model[].class);

		// Iterate through each model and get it individually to ensure individual 
		//		getting for each model returns the same as getAll()
		for (Model m : models) {
			MvcResult response = mvc.perform(get(BASE_URL + "/" + m.getId().toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
			String innerContent = response.getResponse().getContentAsString();
			Model readModel = mapper.readValue(innerContent, Model.class);
			assert m.equals(readModel);
		}
	}


    /**
     * Delete model of specified ID
	 * 
     * @throws Exception
     */
	@Test
	@Order(3)
	public void testDeleteModel() throws Exception {
		// Grab the first model and delete it
		Optional<Model> model = modelRepository.findAll().stream().filter(m->m.getId() != 1).findFirst();
		if (model.isEmpty()) {
			throw new Exception("unable to run delete if no non-default model exists yet");
		}

		MvcResult result = mvc.perform(delete(BASE_URL + "/delete/" + model.get().getId().toString())
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
