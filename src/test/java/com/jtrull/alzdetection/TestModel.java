package com.jtrull.alzdetection;

import org.javatuples.Pair;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.runner.JUnitCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.core.JsonProcessingException;
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
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestModel {
	public static final Logger LOGGER = LoggerFactory.getLogger(TestModel.class);
    @Autowired private MockMvc mvc;
	@Autowired private ModelRepository modelRepository;
	@Autowired private ModelService modelService;

    private static final String BASE_URL = "/api/v1/model";
	private static final String LOAD_URL = BASE_URL + "/load";
	private static final String DELETE_URL = BASE_URL +"/delete";
	private static final String DELETE_ALL_URL = DELETE_URL +"/all";

	private static final int TEST_INVOCATIONS = AlzDetectionApplicationTests.TEST_INVOCATIONS;
	private static final ObjectMapper MAPPER = AlzDetectionApplicationTests.MAPPPER;

    /**
     * Load model from file passed into REST request. We will assert GET/DELETE later.
	 * 
     * @throws Exception
     */
    @Test
	@Order(1)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadModel() throws Exception {
		Pair<String, Model> pair = runLoadModelRequest(modelService, getClass(), mvc);
		String modelName = pair.getValue0();
		Model model = pair.getValue1();
		Assert.assertNotNull("Unable to find Model object after load model request", model);
		Assert.assertTrue("Model after create request returned with different name - this is not intended", model.getName().equals(modelName));
	}

	/**
	 * Load a model into the API using the /model/load endpoint.
	 * @param <T>
	 * 
	 * @return
	 * @throws Exception
	 */
	public static <T> Pair<String, Model> runLoadModelRequest(ModelService modelService, Class<T> clazz, MockMvc mvc) throws Exception {
		String path = getModelPath(modelService);
        String filename = path.substring(path.lastIndexOf("/")+1);
		String modelName = new Random().nextInt(1000) + "-" + filename;
		FileInputStream fis = new FileInputStream(path);

		try (InputStream is = clazz.getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("file", modelName, "application/zip", ByteStreams.toByteArray(fis));

			MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
						.file(mockMultipartFile)
						.contentType("application/zip"))
					.andExpect(status().isOk())
					.andReturn();

			String content = _return.getResponse().getContentAsString();
			Assert.assertNotNull("Unable to find content after loading model through /load endpoint", content);
			Model m = null;
			try {
				m = MAPPER.readValue(content, Model.class);
			} catch (JsonProcessingException e) { Assert.fail("Return from model /load unable to parse to Model object!"); }
			return new Pair<String,Model>(modelName, m);
		} 
	}

	/**
	 * Load an empty file and validate the expected exception
	 * 
	 * @throws Exception
	 */
	@Test
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadInvalidModel() throws Exception {
		String path = findSavedModel(modelService).getAbsolutePath();
        String filename = path.substring(path.lastIndexOf("/")+1);
		String modelName = new Random().nextInt(1000) + "-" + filename;

		try (InputStream is = getClass().getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("file", modelName, "application/zip", 
					ByteStreams.toByteArray(InputStream.nullInputStream()));

			try {
				mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
				fail("succeeded sending invalid model when expected to fail");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                	httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
				Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
					httpException.getMessage().contains("Failed to store empty file"));
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
	@RepeatedTest(TEST_INVOCATIONS)
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

			try {
				mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
				fail("succeeded sending non zip file to load");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
				Assert.assertTrue("status code did not match 415 as expected, found: " + httpException.getStatusCode(), 
                	httpException.getStatusCode().equals(HttpStatus.valueOf(415)));
				Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
					httpException.getMessage().contains("Unable to load model from file that is not a .zip"));
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
	@RepeatedTest(TEST_INVOCATIONS)
	public void testGetModel() throws Exception {
		// Grab the first model and assert we can get it
		if (modelRepository.findAll().size() < 0) {
			throw new Exception("unable to run get if no model exists yet");
		}
		Model model = modelRepository.findAll().get(0);
		Model readModel = runGetModelRequest(model);
		model.setFilepath(null); // filepath nullified during serialize
		Assert.assertEquals("Model does not equal the expected object on return!", model, readModel);
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
		return MAPPER.readValue(content, Model.class);
	}

	/**
	 * Send invalid modelIds to the model endpoint and assert the correct exception is thrown
	 * 
	 * @throws Exception
	 */
	@Test 
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testGetModelInvalidId() throws Exception {
		long invalidId = 2345234523452345L;
		try {
			mvc.perform(get(BASE_URL + "/" + invalidId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded sending invalid modelId");

		} catch (ServletException e) {
			RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
			Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
				httpException.getStatusCode().equals(HttpStatus.valueOf(404)));
			Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
				httpException.getMessage().contains("Unable to find model with Id: " + invalidId));
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
	@RepeatedTest(TEST_INVOCATIONS)
    public void testGetAllModels() throws Exception {
		// Get all models and verify an array of Models is returned
		MvcResult _return = mvc.perform(get(BASE_URL + "/all")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = _return.getResponse().getContentAsString();
		Model[] models = MAPPER.readValue(content, Model[].class);

		// Iterate through each model and get it individually to ensure individual 
		//		getting for each model returns the same as getAll()
		for (Model m : models) {
			MvcResult response = mvc.perform(get(BASE_URL + "/" + m.getId())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
			String innerContent = response.getResponse().getContentAsString();
			Model readModel = MAPPER.readValue(innerContent, Model.class);
			Assert.assertEquals("Model does not equal the expected object on return!", m, readModel);
		}
	}


    /**
     * Attempt to delete a model with an Id that does not exist
	 * 
     * @throws Exception
     */
	@Test
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testInvalidModelDelete() throws Exception {
		long invalidId = 4892374923L;
		try {
			mvc.perform(delete(DELETE_URL + "/" + invalidId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded sending invalid model delete when expected to fail");

		} catch (ServletException e) {
			RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
			Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
				httpException.getStatusCode().equals(HttpStatus.valueOf(404)));
			Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
				httpException.getMessage().contains("Unable to find model with Id: " + invalidId));
		}
	}

	/** 
	 * Attempt to delete the default model and assert the failure matches expectations
	 */
	@Test
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testDeleteDefaultModel() throws Exception {
		try {
			mvc.perform(delete(DELETE_URL + "/1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded deleting default model when expected to fail");

		} catch (ServletException e) {
			RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
			Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
				httpException.getStatusCode().equals(HttpStatus.valueOf(403)));
			Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
				httpException.getMessage().contains("Unable to delete default model"));
		}
	}

	/**
	 * Test deleting a model by grabbing a non-default model
	 * 
	 * @throws Exception
	 */
	@Test
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	@Execution(SAME_THREAD) 
	public void testDeleteModel() throws Exception {
		// Grab the first model and delete it
		Optional<Model> model = modelRepository.findAll().stream().filter(m->m.getId() != 1).findFirst();
		if (model.isEmpty()) {
			throw new Exception("unable to run delete if no non-default model exists yet");
		}

		MvcResult result = mvc.perform(delete(DELETE_URL + "/" + model.get().getId())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = result.getResponse().getContentAsString();
		Assert.assertTrue("Content of delete model body not 'true' boolean as expected, found: " + content, Boolean.valueOf(content));
	}

    /** 
     * Delete all models present for the API
	 * 
	 * @throws Exception
     */
    @Test
    @Order(4)
	@RepeatedTest(TEST_INVOCATIONS)
	@Execution(SAME_THREAD) 
    public void testDeleteAllModels() throws Exception {
        MvcResult result = mvc.perform(delete(DELETE_ALL_URL)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
        String content = result.getResponse().getContentAsString();
		Assert.assertTrue("Content of delete all models body not 'true' boolean as expected, found: " + content, Boolean.valueOf(content));
    }

	/**
	 * Run all tests in class concurrently
	 */
	@Test
	@Order(5)
	@RepeatedTest(TEST_INVOCATIONS)
    public void runAllTests() {
		int numConcurrent = 25_000;
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
	private static File findSavedModel(ModelService modelService) throws Exception {
		Optional<File> savedModel = modelService.getSavedModelInResourcesDir(ModelService.DEFAULT_MODEL_NAME);
		if (savedModel.isEmpty()) {
			Assert.fail("Unable to find saved model");
		}
		return savedModel.get();
	}

	public static String getModelPath(ModelService modelService) throws Exception {
		return findSavedModel(modelService).getAbsolutePath();
	}
}
