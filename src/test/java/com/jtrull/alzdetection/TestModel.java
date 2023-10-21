package com.jtrull.alzdetection;

import org.javatuples.Pair;
import org.json.JSONObject;
import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.runner.JUnitCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestModel {
    @Autowired
	private MockMvc mvc;

	@Autowired
	private ModelRepository modelRepository;

	@Autowired
	private ModelService modelService;

    private static final String BASE_URL = "/api/v1/model";
	private static final ObjectMapper MAPPPER = new ObjectMapper();

	private int modelNum = 0;

    /**
     * Load model from file passed into REST request. We will assert GET/DELETE later.
	 * 
     * @throws Exception
     */
    @Test
	@Order(1)
	@RepeatedTest(10)
	public void testLoadModel() throws Exception {
		Pair<String, Model> pair = runLoadModelRequest();
		String modelName = pair.getValue0();
		Model model = pair.getValue1();
		assert model != null;
		assert model.getName().equals(modelName);
		
	}

	/**
	 * Load a model into the API using the /model/load endpoint.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Pair<String, Model> runLoadModelRequest() throws Exception {
		String path = findSavedModel().getAbsolutePath();
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
			return new Pair<String,Model>(modelName, MAPPPER.readValue(content, Model.class));
		} 
	}

	/**
	 * Load an empty file and validate the expected exception
	 * 
	 * @throws Exception
	 */
	@Test
	@Order(2)
	@RepeatedTest(10)
	public void testLoadInvalidModel() throws Exception {
		String path = findSavedModel().getAbsolutePath();
        String filename = path.substring(path.lastIndexOf("/")+1);

		try (InputStream is = getClass().getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("file", modelNum + "-" + filename, "application/zip", 
					ByteStreams.toByteArray(InputStream.nullInputStream()));

			String url = BASE_URL + "/load";
			try {
				mvc.perform(MockMvcRequestBuilders.multipart(url)
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
				fail("succeeded sending invalid model when expected to fail");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				assert httpException.getStatusCode().equals(HttpStatus.valueOf(400));
				assert httpException.getMessage().contains("Failed to store empty file");
			}
		} 
	}

	/**
	 * Attempt to load a non-zip file as a model and verify the exception
	 * 
	 * @throws Exception
	 */
	@Test
	@Order(2)
	@RepeatedTest(10)
	public void testLoadNonZipAsModel() throws Exception {
		String path = modelService.returnModelPath();
		String filepath = path + "/test.json";

		JSONObject json = new JSONObject();
		json.put("i am a json file", "not a model zip");
		FileWriter file = new FileWriter(filepath);
		file.write(json.toString());
		file.close();
		FileInputStream fis = new FileInputStream(filepath);

		try (InputStream is = getClass().getResourceAsStream(filepath)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "test.json", MediaType.APPLICATION_JSON.toString(), ByteStreams.toByteArray(fis));

			String url = BASE_URL + "/load";
			try {
				mvc.perform(MockMvcRequestBuilders.multipart(url)
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
				fail("succeeded sending non zip file to load");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				assert httpException.getStatusCode().equals(HttpStatus.valueOf(415));
				assert httpException.getMessage().contains("Unable to load model from file that is not a .zip");
			}
		} 
	}

	/**
     * Get model by specified modelId and assert the model matches the model we sent in
	 * 
     * @throws Exception
     */
	@Test 
	@Order(2)
	@RepeatedTest(10)
	public void testGetModel() throws Exception {
		// Grab the first model and assert we can get it
		if (modelRepository.findAll().size() < 0) {
			throw new Exception("unable to run get if no model exists yet");
		}
		Model model = modelRepository.findAll().get(0);
		Model readModel = runGetModelRequest(model);
		model.setFilepath(null); // filepath nullified during serialize
		assert model.equals(readModel);
	}

	/**
	 * Run GET request for a model, either with or without an input model specified. If no input specified, find the first model in the database.
	 * @param inputModel
	 * @return
	 * @throws Exception
	 */
	public Model runGetModelRequest(Model inputModel) throws Exception {
		// Grab the first model and assert we can get it
		if (modelRepository.findAll().size() < 0) {
			throw new Exception("unable to run get if no model exists yet");
		}
		Long modelId = modelRepository.findAll().get(0).getId();
		if (inputModel != null) {
			modelId = inputModel.getId();
		}

		MvcResult response = mvc.perform(get(BASE_URL + "/" + modelId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String content = response.getResponse().getContentAsString();
		return MAPPPER.readValue(content, Model.class);
	}

	/**
	 * Send invalid modelIds to the model endpoint and assert the correct exception is thrown
	 * 
	 * @throws Exception
	 */
	@Test 
	@Order(2)
	@RepeatedTest(10)
	public void testGetModelInvalidId() throws Exception {
		long invalidId = 2345234523452345L;
		try {
			mvc.perform(get(BASE_URL + "/" + invalidId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded sending invalid modelId");

		} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				assert httpException.getStatusCode().equals(HttpStatus.valueOf(404));
				assert httpException.getMessage().contains("Unable to find model with Id: " + invalidId);
		}
	}

    /**
     * Get all models regardles of ID. Run a single get on each model returned from the getAll() and assert the 
	 * 	resource representations are identical
	 * 
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
		Model[] models = MAPPPER.readValue(content, Model[].class);

		// Iterate through each model and get it individually to ensure individual 
		//		getting for each model returns the same as getAll()
		for (Model m : models) {
			MvcResult response = mvc.perform(get(BASE_URL + "/" + m.getId().toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
			String innerContent = response.getResponse().getContentAsString();
			Model readModel = MAPPPER.readValue(innerContent, Model.class);
			assert m.equals(readModel);
		}
	}


    /**
     * Attempt to delete a model with an Id that does not exist
	 * 
     * @throws Exception
     */
	@Test
	@Order(3)
	@RepeatedTest(10)
	public void testInvalidModelDelete() throws Exception {
		long invalidId = 4892374923L;
		try {
			mvc.perform(delete(BASE_URL + "/delete/" + invalidId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded sending invalid model delete when expected to fail");

		} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				assert httpException.getStatusCode().equals(HttpStatus.valueOf(404));
				assert httpException.getMessage().contains("Unable to find model with specified Id: " + invalidId);
		}
	}

	/** 
	 * Attempt to delete the default model and assert the failure matches expectations
	 */
	@Test
	@Order(3)
	@RepeatedTest(10)
	public void testDeleteDefaultModel() throws Exception {
		try {
			mvc.perform(delete(BASE_URL + "/delete/1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded deleting default model when expected to fail");

		} catch (ServletException e) {
			RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
			assert httpException.getStatusCode().equals(HttpStatus.valueOf(403));
			assert httpException.getMessage().contains("Unable to delete default model");
		}
	}

	/**
	 * Test deleting a model by grabbing a non-default model
	 * 
	 * @throws Exception
	 */
	@Test
	@Order(3)
	@RepeatedTest(5)
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
	 * 
	 * @throws Exception
     */
    @Test
    @Order(4)
	@RepeatedTest(10)
    public void testDeleteAllModels() throws Exception {
        MvcResult result = mvc.perform(delete(BASE_URL + "/delete/all")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
        String content = result.getResponse().getContentAsString();
		assert Boolean.valueOf(content);
    }

	/**
	 * Run all tests in class concurrently
	 */
	@Test
	@Order(5)
	@RepeatedTest(10)
    public void runAllTests() {
		int numConcurrent = 1_000_000;
        Class<?>[] classes  = new Class<?>[numConcurrent];
        Arrays.fill(classes, TestModel.class);
        JUnitCore.runClasses(new ParallelComputer(true, true), classes);
    }

	/**
	 * Find a saved model in the resources/model/ directory with the default model name. 
	 * 	If this cannot be found we cannot perform the load model tests
	 * 
	 * @return
	 * @throws Exception
	 */
	private File findSavedModel() throws Exception {
		Optional<File> savedModel = modelService.getSavedModelInResourcesDir(ModelService.DEFAULT_MODEL_NAME);
		if (savedModel.isEmpty()) {
			throw new Exception("Unable to find saved model");
		}
		return savedModel.get();
	}
}
