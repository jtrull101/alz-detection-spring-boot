package com.jtrull.alzdetection;

import org.javatuples.Pair;
import org.json.JSONObject;
import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.jtrull.alzdetection.exceptions.model.InvalidModelFileException;
import com.jtrull.alzdetection.exceptions.model.ModelNotFoundException;
import com.jtrull.alzdetection.model.Model;
import com.jtrull.alzdetection.model.ModelRepository;
import com.jtrull.alzdetection.model.ModelService;
import com.jtrull.alzdetection.general.Utils;

import jakarta.servlet.ServletException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@Order(3)
public class TestModel {
	public static final Logger LOGGER = LoggerFactory.getLogger(TestModel.class);
    @Autowired private MockMvc mvc;
	@Autowired private ModelRepository modelRepository;
	@Autowired private ModelService modelService;

	private static final String ID_KEY = "?id=";
    private static final String BASE_URL = "/api/v1/model";
	private static final String LOAD_URL = BASE_URL + "/load";
	private static final String LOAD_DETAILS_URL = BASE_URL + "/load/details" + ID_KEY;
	private static final String DELETE_URL = BASE_URL +"/delete";
	private static final String DELETE_ALL_URL = DELETE_URL +"/all";

	private static final int TEST_INVOCATIONS = AlzDetectionApplicationTests.TEST_INVOCATIONS;
	private static final ObjectMapper MAPPER = AlzDetectionApplicationTests.MAPPER;

    /**
     * Load model from file passed into REST request. We will assert GET/DELETE later.
	 * 
     * @throws Exception
     */
	@Order(1)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadModel() throws Exception {
		Model m = runLoadModelRequest(modelService, getClass(), mvc, false);
		assertNotNull("Unable to find Model object after load model request", m);
	}

	@Order(1)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadModelPropertiesAndOrPlot() throws Exception {
		Model model = runLoadModelRequest(modelService, getClass(), mvc, true);
		assertNotNull("Unable to find Model object after load details request", model);
	}

	@Order(1)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testGetDefaultModelPropertiesAndPlot() throws Exception {
		Model m = runGetModelRequest(Optional.empty());
		Model modelFromService = modelService.getModelById(m.getId());
		assertNotNull("Unable to find seaborn plot path for default model", modelFromService.getSeabornPlotPath());
		assertNotNull("Unable to find loss for default model", modelFromService.getLoss());
		assertNotNull("Unable to find accuracy for default model", modelFromService.getAccuracy());
	}

	/**
	 * Load a model into the API using the /model/load endpoint.
	 * 
	 * @param <T>
	 * @param modelService
	 * @param clazz
	 * @param mvc
	 * @return
	 * @throws Exception
	 */
	public static <T> Model runLoadModelRequest(ModelService modelService, Class<T> clazz, MockMvc mvc, boolean withDetails) throws Exception {
		String path = getModelPath(modelService);
        String filename = path.substring(path.lastIndexOf("/")+1);
		String modelName = new Random().nextInt(1000) + "-" + filename;
		FileInputStream fis = new FileInputStream(path);
		Model m = null;

		try (InputStream is = clazz.getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("model", modelName, "application/zip", ByteStreams.toByteArray(fis));

			MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
						.file(mockMultipartFile)
						.contentType("application/zip"))
					.andExpect(status().isOk())
					.andReturn();

			String content = _return.getResponse().getContentAsString();
			assertNotNull("Unable to find content after loading model through /load endpoint", content);
			
			try {
				m = MAPPER.readValue(content, Model.class);
			} catch (JsonProcessingException e) { fail("Return from model /load unable to parse to Model object!"); }
			// Include modelName in request so we can assert the model was created with the correct ZIP file
			
			assertTrue("Model after create request returned with different name - this is not intended", m.getName().equals(modelName));
			
		} 
		if (!withDetails) {
			return m;
		}

		Model modelFromService = modelService.getModelById(m.getId());

		// use model's filepath 
		String modelPath = modelFromService.getFilepath();	
		File modelFile = new File(modelPath);
		String parent = modelFile.getParent();
		double rand = Math.random();

		// 33% of the time just send plot
		if (rand <= 0.33) {
			Pair<File, MockMultipartFile> plotPair = getPlotFile(parent, modelName);
			try (InputStream is = clazz.getResourceAsStream(plotPair.getValue0().getAbsolutePath())) {
				MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(LOAD_DETAILS_URL + modelFromService.getId())
							.file(plotPair.getValue1()))
						.andExpect(status().isOk())
						.andReturn();

				String content = _return.getResponse().getContentAsString();
				assertNotNull("Unable to find content after loading model plot through /details endpoint", content);
				try {
					modelFromService = MAPPER.readValue(content, Model.class);
					assertNotNull("Unable to find seaborn plot path after pushing plot to /details endpoint", modelFromService.getSeabornPlotPath());
					return modelFromService;
				} catch (JsonProcessingException e) { fail("Return from model /load/details unable to parse to Model object!"); }	
			} 

		// 33% of the time just send properties
		} else if (rand <= 0.66) {
			Pair<File, MockMultipartFile> propertiesPair = getPropertiesFile(parent, modelName);
			try (InputStream is = clazz.getResourceAsStream(propertiesPair.getValue0().getAbsolutePath())) {
				MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(LOAD_DETAILS_URL + modelFromService.getId())
							.file(propertiesPair.getValue1()))
						.andExpect(status().isOk())
						.andReturn();

				String content = _return.getResponse().getContentAsString();
				assertNotNull("Unable to find content after loading model properties through /details endpoint", content);
				try {
					modelFromService = MAPPER.readValue(content, Model.class);
					assertNotNull("Unable to find loss after pushing plot to /details endpoint", modelFromService.getLoss());
					assertNotNull("Unable to find accuracy after pushing plot to /details endpoint", modelFromService.getAccuracy());
					return modelFromService;
				} catch (JsonProcessingException e) { fail("Return from model /load/details unable to parse to Model object!"); }	
			} 
		} else {
			Pair<File, MockMultipartFile> plotPair = getPlotFile(parent, modelName);
			Pair<File, MockMultipartFile> propertiesPair = getPropertiesFile(parent, modelName);
			try (InputStream is = clazz.getResourceAsStream(plotPair.getValue0().getAbsolutePath())) {
				try (InputStream _is = clazz.getResourceAsStream(propertiesPair.getValue0().getAbsolutePath())) {

					MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(LOAD_DETAILS_URL + modelFromService.getId())
							.file(propertiesPair.getValue1())
							.file(plotPair.getValue1()))
						.andExpect(status().isOk())
						.andReturn();

					String content = _return.getResponse().getContentAsString();
					assertNotNull("Unable to find content after loading model properties through /details endpoint", content);
					try {
						modelFromService = MAPPER.readValue(content, Model.class);
						assertNotNull("Unable to find seaborn plot path after pushing plot to /details endpoint", modelFromService.getSeabornPlotPath());
						assertNotNull("Unable to find loss after pushing plot to /details endpoint", modelFromService.getLoss());
						assertNotNull("Unable to find accuracy after pushing plot to /details endpoint", modelFromService.getAccuracy());
						return modelFromService;
					} catch (JsonProcessingException e) { fail("Return from model /load/details unable to parse to Model object!"); }
				}
			}
		}
		throw new AssertionError("Unable to load model details for specified model!");


	}

	/**
	 * 
	 * @param path
	 * @param modelName
	 * @return
	 * @throws IOException
	 */
	private static Pair<File, MockMultipartFile> getPropertiesFile(String path, String modelName) throws IOException {
		File propertiesFile = Files.walk(Paths.get(path))
			.filter(Files::isRegularFile)
			.filter(r -> r.getFileName().toString().contains(Utils.PROPERTIES_FILE_TYPE))
			.map(x -> x.toFile())
			.findFirst()
			.orElseThrow(() -> 
				new AssertionError("Unable to find properties file for model name '" + modelName + "' in path '" + path + "'"));

		return new Pair<File,MockMultipartFile> (
			propertiesFile,
			new MockMultipartFile("properties", propertiesFile.getName(), MediaType.TEXT_PLAIN_VALUE, ByteStreams.toByteArray(new FileInputStream(propertiesFile))));
	}

	/**
	 * 
	 * @param path
	 * @param modelName
	 * @return
	 * @throws IOException
	 */
	private static Pair<File, MockMultipartFile> getPlotFile(String path, String modelName) throws IOException {
		File plotFile = Files.walk(Paths.get(path))
			.filter(Files::isRegularFile)
			.filter(r -> r.getFileName().toString().contains(Utils.SEABORN_PLOT_TYPE))
			.map(x -> x.toFile())
			.findFirst()
			.orElseThrow(() -> 
				new AssertionError("Unable to find seaborn plot for model name '" + modelName + "' in path '" + path + "'"));

		return new Pair<File,MockMultipartFile> (
			plotFile,
			new MockMultipartFile("plot", plotFile.getName(), MediaType.IMAGE_PNG_VALUE, ByteStreams.toByteArray(new FileInputStream(plotFile))));
		
	}

	/**
	 * Load an empty file and validate the expected exception
	 * 
	 * @throws Exception
	 */
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadInvalidModel() throws Exception {
		String path = findSavedModel(modelService).getAbsolutePath();
        String filename = path.substring(path.lastIndexOf("/")+1);
		String modelName = new Random().nextInt(1000) + "-" + filename;

		try (InputStream is = getClass().getResourceAsStream(path)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("model", modelName, "application/zip", 
					ByteStreams.toByteArray(InputStream.nullInputStream()));

			mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
					.file(mockMultipartFile)
					.contentType(MediaType.APPLICATION_JSON.toString()))
					.andExpect(status().is4xxClientError())
					.andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidModelFileException))
					.andExpect(result -> assertTrue("Unexpected message:" + result.getResolvedException().getMessage(), 
						result.getResolvedException().getMessage().contains("Unable to create model for input")))
					.andExpect(result -> assertTrue("Unexpected details:" + ((InvalidModelFileException) result.getResolvedException()).getDetails(),  
						((InvalidModelFileException) result.getResolvedException()).getDetails().contains("Found empty file")));
		} 
	}

	/**
	 * Attempt to load a non-zip file as a model and verify the exception
	 * 
	 * @throws Exception
	 */
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testLoadNonZipAsModel() throws Exception {
		String path = Utils.returnModelPath();
		String filepath = path + "/test.json";

		JSONObject json = new JSONObject();
		json.put("i am a json file", "not a model zip");
		FileWriter file = new FileWriter(filepath);
		file.write(json.toString());
		file.close();
		FileInputStream fis = new FileInputStream(filepath);

		try (InputStream is = getClass().getResourceAsStream(filepath)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("model", "test.json", MediaType.APPLICATION_JSON.toString(), ByteStreams.toByteArray(fis));
			
			mvc.perform(MockMvcRequestBuilders.multipart(LOAD_URL)
					.file(mockMultipartFile)
					.contentType(MediaType.APPLICATION_JSON.toString()))
					.andExpect(status().is4xxClientError())
					.andExpect(result -> assertTrue(result.getResolvedException() instanceof InvalidModelFileException))
					.andExpect(result -> assertTrue("Unexpected message:" + result.getResolvedException().getMessage(), 
						result.getResolvedException().getMessage().contains("Unable to create model for input")));
		} 
	}

	/**
     * Get model by specified modelId and assert the model matches the model we sent in
	 * 
     * @throws Exception
     */ 
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testGetModel() throws Exception {
		// Grab the first model and assert we can get it
		if (modelRepository.findAll().size() < 0) {
			throw new Exception("unable to run get if no model exists yet");
		}
		Model model = modelRepository.findAll().get(0);
		Model readModel = runGetModelRequest(Optional.of(model));
		model.setFilepath(null); // filepath nullified during serialize
		assertEquals("Model does not equal the expected object on return!", model, readModel);
	}

	/**
	 * Run GET request for a model, either with or without an input model specified. If no input specified, find the first model in the database.
	 * @param inputModel
	 * @return
	 * @throws Exception
	 */
	public Model runGetModelRequest(Optional<Model> inputModel) throws Exception {
		// Grab the first model and assert we can get it
		if (modelRepository.findAll().size() < 0) {
			throw new Exception("unable to run get if no model exists yet");
		}
		Long modelId = modelRepository.findAll().get(0).getId();
		if (inputModel.isPresent()) {
			modelId = inputModel.get().getId();
		}

		MvcResult response = mvc.perform(get(BASE_URL + ID_KEY + modelId)
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
	@Order(2)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testGetModelInvalidId() throws Exception {
		long invalidId = 2345234523452345L;
	
		mvc.perform(get(BASE_URL + ID_KEY + invalidId)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(result -> assertTrue(result.getResolvedException() instanceof ModelNotFoundException))
			.andExpect(result -> assertTrue(result.getResolvedException().getMessage().contains("search by ID for '" + invalidId + "'")));
			
	}

    /**
     * Get all models regardless of ID. Run a single get on each model returned from the getAll() and assert the 
	 * 	resource representations are identical
	 * 
     * @throws Exception
     */
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
			MvcResult response = mvc.perform(get(BASE_URL + ID_KEY + m.getId())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
			String innerContent = response.getResponse().getContentAsString();
			Model readModel = MAPPER.readValue(innerContent, Model.class);
			assertEquals("Model does not equal the expected object on return!", m, readModel);
		}
	}


    /**
     * Attempt to delete a model with an Id that does not exist
	 * 
     * @throws Exception
     */
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testInvalidModelDelete() throws Exception {
		long invalidId = 4892374923L;
		mvc.perform(delete(DELETE_URL + ID_KEY + invalidId)
			.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().is4xxClientError())
			.andExpect(result -> assertTrue(result.getResolvedException() instanceof ModelNotFoundException))
			.andExpect(result -> assertTrue(result.getResolvedException().getMessage().contains("search by ID for '" + invalidId + "'")));
	}

	/** 
	 * Attempt to delete the default model and assert the failure matches expectations
	 */
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	public void testDeleteDefaultModel() throws Exception {
		try {
			mvc.perform(delete(DELETE_URL + ID_KEY + "1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError());
			fail("succeeded deleting default model when expected to fail");

		} catch (ServletException e) {
			assertTrue("Unexpected exception type returned!", e.getRootCause() instanceof UnsupportedOperationException);
		}
	}

	/**
	 * Test deleting a model by grabbing a non-default model
	 * 
	 * @throws Exception
	 */
	@Order(3)
	@RepeatedTest(TEST_INVOCATIONS)
	@Execution(SAME_THREAD) 
	public void testDeleteModel() throws Exception {
		// Grab the first model and delete it
		Optional<Model> model = modelRepository.findAll().stream().filter(m->m.getId() != 1).findFirst();
		if (model.isEmpty()) {
			throw new Exception("unable to run delete if no non-default model exists yet");
		}

		MvcResult result = mvc.perform(delete(DELETE_URL + ID_KEY + model.get().getId())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
		String content = result.getResponse().getContentAsString();
		assertTrue("Content of delete model body not 'true' boolean as expected, found: " + content, Boolean.valueOf(content));
	}

    /** 
     * Delete all models present for the API
	 * 
	 * @throws Exception
     */
    @Order(4)
	@RepeatedTest(TEST_INVOCATIONS)
	// @Execution(SAME_THREAD) 
    public void testDeleteAllModels() throws Exception {
        MvcResult result = mvc.perform(delete(DELETE_ALL_URL)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();
        String content = result.getResponse().getContentAsString();
		assertTrue("Content of delete all models body not 'true' boolean as expected, found: " + content, Boolean.valueOf(content));
    }

	/**
	 * Run all tests in class concurrently
	 */
	@Order(5)
	// @RepeatedTest(TEST_INVOCATIONS)
    public void runAllTests() {
		int numConcurrent = 25_000;
        Class<?>[] classes  = new Class<?>[numConcurrent];
        Arrays.fill(classes, TestModel.class);
		Result result = JUnitCore.runClasses(new ParallelComputer(true, true), classes);
        assertTrue("Failed during execution of concurrent tests", result.wasSuccessful());
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
			fail("Unable to find saved model");
		}
		return savedModel.get();
	}

	public static String getModelPath(ModelService modelService) throws Exception {
		return findSavedModel(modelService).getAbsolutePath();
	}
}
