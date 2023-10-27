package com.jtrull.alzdetection;

import org.junit.Assert;
import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
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
import com.jtrull.alzdetection.Image.ImagePrediction;
import com.jtrull.alzdetection.Image.ImageRepository;
import com.jtrull.alzdetection.Image.ImageService;
import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class TestImagePrediction {
    public static final Logger LOGGER = LoggerFactory.getLogger(TestImagePrediction.class);
    @Autowired private MockMvc mvc;
	@Autowired private ImageRepository imageRepository;
    @Autowired private ImageService imageService;
    @Autowired private ModelRepository modelRepository;
    @Autowired private ModelService modelService;

    private static final String BASE_URL = "/api/v1/model";
    private static final String ID_KEY = "?id=";

    private static final int TEST_INVOCATIONS = AlzDetectionApplicationTests.TEST_INVOCATIONS;
    private static final ObjectMapper MAPPER = AlzDetectionApplicationTests.MAPPER;


	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPrediction() throws Exception {
        String url = createPredictUrl(getModel(1L).getId());
        MvcResult _return = mvc.perform(get(url + "/random")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        validatePrediction(_return);
	}

	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPredictionFromCategory() throws Exception {
        for (ImpairmentEnum val : ImpairmentEnum.values()) {
            MvcResult _return = mvc.perform(get(createPredictUrl(getModel(1L).getId()) + "/random/" + val.toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is2xxSuccessful())
                .andReturn();
            validatePrediction(_return);
        }
	}

	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPredictionFromInvalidCategory() throws Exception {
        String impairment = RandomStringUtils.random(5, true, true);

        try {
            mvc.perform(get(createPredictUrl(getModel(1L).getId()) + "/random/" + impairment)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded sending invalid impairment category when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();       
            Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains("Unable to parse category: " + impairment + ". Expected values=[" + Arrays.asList(ImpairmentEnum.asStrings().toArray()) + "]"));
        }
	}

	@Order(2)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testPredictionFromFile() throws Exception {
        ImagePrediction initialImagePrediction = getInitialImagePrediction();
 
        String path = initialImagePrediction.getFilepath();
        String filename = path.substring(path.lastIndexOf("/")+1);
        MediaType mediaType = new MediaType("multipart", "form-data", Collections.singletonMap("boundary", "265001916915724"));
      
        byte[] inputArray = Files.readAllBytes(Paths.get(path));
        MockMultipartFile mockMultipartFile = new MockMultipartFile("image", filename, mediaType.toString(), inputArray);
        
        String url = createPredictUrl(getModel(1L).getId());
        MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(url)
                    .file(mockMultipartFile)
                    .contentType(mediaType))
                .andExpect(status().isOk())
                .andReturn();
        validatePrediction(_return);

        String content = _return.getResponse().getContentAsString();
        ImagePrediction prediction = MAPPER.readValue(content, ImagePrediction.class);

        // Assert if a picture is passed in that we have the known predictions for, the predictions will match
        Assert.assertEquals("Found not matching impairment level",prediction.getConf_NoImpairment(),initialImagePrediction.getConf_NoImpairment());
        Assert.assertEquals("Found not matching impairment level",prediction.getConf_VeryMildImpairment(),initialImagePrediction.getConf_VeryMildImpairment());
        Assert.assertEquals("Found not matching impairment level",prediction.getConf_MildImpairment(),initialImagePrediction.getConf_MildImpairment());
        Assert.assertEquals("Found not matching impairment level",prediction.getConf_ModerateImpairment(),initialImagePrediction.getConf_ModerateImpairment());
	}

    /**
     * Get an image prediction from the repository. Populate the repository with some random predictions if none are present.
     * 
     * @return
     * @throws Exception
     */
    private ImagePrediction getInitialImagePrediction() throws Exception {
        List<ImagePrediction> allImages = imageRepository.findAll();
        if (allImages.size() == 0) {
            testRandomPrediction();
        }
        allImages = imageRepository.findAll();
        Optional<ImagePrediction> imageOpt = allImages.stream()
            .skip(new Random().nextInt(allImages.size()))
            .findAny();
        if (imageOpt.isEmpty()) {
            throw new Exception("Unable to find an image in image repository");
        }
        ImagePrediction initialImagePrediction = imageOpt.get();
        return initialImagePrediction;
    }


	@Order(2)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testPredictionFromInvalidFile() throws Exception {
        String path = imageService.returnImagePath();
		String filepath = path + "/test.json";

		JSONObject json = new JSONObject();
		json.put("i am a json file", "not a jpg");
		FileWriter file = new FileWriter(filepath);
		file.write(json.toString());
		file.close();
		FileInputStream fis = new FileInputStream(filepath);

		try (InputStream is = getClass().getResourceAsStream(filepath)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("image", "test.json", 
            MediaType.APPLICATION_JSON.toString(), ByteStreams.toByteArray(fis));

			try {
				mvc.perform(MockMvcRequestBuilders.multipart(createPredictUrl(getModel(1L).getId()))
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
                fail("succeeded sending invalid image when expected to fail");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
                Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                    httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
                Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                    httpException.getMessage().contains("is it a valid image?"));
			}
		}
    }

    @Order(3)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testGetPrediction() throws Exception {
        ImagePrediction initialPrediction = getInitialImagePrediction();
        String url = createGetPredictionUrl(initialPrediction.getAssociatedModel(), initialPrediction.getId());
        MvcResult _return = mvc.perform(get(url)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        String content = _return.getResponse().getContentAsString();
        Assert.assertNotNull("Unable to read content from result of prediction GET request to URL:" + url, content);
        ImagePrediction prediction = MAPPER.readValue(content, ImagePrediction.class);
        Assert.assertNotNull("Unable to marshall prediction object from content:" + content, prediction);
        // properties ignored by json 
        initialPrediction.setAssociatedModel(null);
        Assert.assertEquals("Prediction after GET does not match prediction from repository", initialPrediction, prediction);
    }

    @Order(3)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testGetInvalidPredictionId() throws Exception {
        ImagePrediction initialPrediction = getInitialImagePrediction();
        long invalidId = 2345234523452345L;
        String url = createGetPredictionUrl(initialPrediction.getAssociatedModel(), invalidId);
        try {
            mvc.perform(get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded getting prediction with invalid Id when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(404)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains("Unable to find prediction with Id: " + invalidId));
        }
    }

    @Order(3)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testGetInvalidModelIdForPrediction() throws Exception {
        ImagePrediction initialPrediction = getInitialImagePrediction();
        long invalidId = 2345234523452345L;
        String url = createGetPredictionUrl(invalidId, initialPrediction.getId());
        try {
            mvc.perform(get(url)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded getting prediction with invalid model Id expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains("Error with request, unable to find prediction for modelId " + invalidId));
        }
    }
    

    /**
	 * Run all tests in class concurrently
	 */
	@Order(4)
	// @RepeatedTest(TEST_INVOCATIONS)
    public void runAllTests() {
        int numConcurrent = 25_000;
        Class<?>[] classes  = new Class<?>[numConcurrent];
        Arrays.fill(classes, TestImagePrediction.class);
        Result result = JUnitCore.runClasses(new ParallelComputer(true, true), classes);
        Assert.assertTrue("Failed during execution of concurrent tests", result.wasSuccessful());
    }

    @Order(5)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testDeletePrediction() throws Exception {
        Optional<ImagePrediction> imageOpt = imageRepository.findAll().stream()
            .filter(i -> i.getAssociatedModel() == 1L).findFirst();
        if (imageOpt.isEmpty()) {
            throw new Exception("Unable to find an image in image repository");
        }
        ImagePrediction initialImagePrediction = imageOpt.get();

        MvcResult _return = mvc.perform(delete(createPredictUrl(
                getModel(initialImagePrediction.getAssociatedModel()).getId()) + "/delete" + ID_KEY + initialImagePrediction.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String content = _return.getResponse().getContentAsString();
        Assert.assertNotNull("Unable to validate return of delete request, expected boolean", content);
        Assert.assertTrue("Result of delete request did not match expected true", Boolean.valueOf(content));
    }

    @Order(5)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testInvalidDeletePrediction() throws Exception {
        long invalidId = 2345234523452345L;
        try {
            mvc.perform(delete(createPredictUrl(getModel().getId()) + "/delete" + ID_KEY + invalidId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded deleting invalid image when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(404)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains("Unable to find prediction with Id: " + invalidId));
        }
    }

    private String createPredictUrl(Long modelId) {
        return BASE_URL + "/" + String.valueOf(modelId) + "/predict";
    }

    private String createGetPredictionUrl(Long modelId, long predictionId) {
        return createPredictUrl(modelId) + "/get" + ID_KEY + predictionId;
    }

    public Model getModel() throws Exception {
        return getModel(null);
    }

    /**
     * Get a model from the model repository. If unable to find a model in the repository, reload the default model.
     * 
     * @return
     */
    public Model getModel(Long desiredId) throws Exception {
        synchronized (modelRepository) {
            List<Model> allModels = modelRepository.findAll();
            // load models into the model repository
            for (int i = allModels.size(); i<=10; i++) {
                TestModel.runLoadModelRequest(modelService, getClass(), mvc);
            }
            allModels = modelRepository.findAll();

            if (desiredId != null) {
                return allModels.stream().filter(m -> m.getId() == desiredId).findFirst().get();
            }

            // pick a random model
            return allModels.stream()
                .skip(new Random().nextInt(allModels.size()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Unable to pick random model"));
        }
    }

    public void validatePrediction(MvcResult _return) throws UnsupportedEncodingException {
        String content = _return.getResponse().getContentAsString();
        Assert.assertNotNull("Unable to find ImagePrediction object after random prediction request", content);
        ImagePrediction prediction = null;
        try {
            prediction = MAPPER.readValue(content, ImagePrediction.class);
        } catch (JsonProcessingException e) { Assert.fail("Return from image prediction unable to parse to ImagePrediction object!"); }
		Assert.assertNotNull("Failed during marshalling of ImagePrediction object after request", prediction);
    }
}

