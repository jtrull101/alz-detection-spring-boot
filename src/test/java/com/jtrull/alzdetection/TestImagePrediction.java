package com.jtrull.alzdetection;

import org.junit.Assert;
import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
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
    Logger logger = LoggerFactory.getLogger(TestImagePrediction.class);
    @Autowired
	private MockMvc mvc;

	@Autowired
	private ImageRepository imageRepository;
    @Autowired
    private ImageService imageService;
    @Autowired
    private ModelRepository modelRepository;
    @Autowired
    private ModelService modelService;

    private static final String BASE_URL = "/api/v1/model";

    private int num = 0;
    private static final int TEST_INVOCATIONS = 1;

    private String createBaseUrl(Long modelId) {
        return BASE_URL + "/" + String.valueOf(modelId) + "/predict";
    }

    @Test
	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPrediction() throws Exception {
        String url = createBaseUrl(getModel().getId());
        logger.info("Running random prediction on url: " + url);
        MvcResult _return = mvc.perform(get(url + "/random")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String content = _return.getResponse().getContentAsString();

        // Assert this string is valid prediction class
        assert content != null;
        ObjectMapper mapper = new ObjectMapper();
        ImagePrediction prediction = mapper.readValue(content, ImagePrediction.class);
        assert prediction != null;
	}

    @Test
	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPredictionFromCategory() throws Exception {
        for (ImpairmentEnum val : ImpairmentEnum.values()) {
            MvcResult _return = mvc.perform(get(createBaseUrl(getModel().getId()) + "/" + val.toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is2xxSuccessful())
                .andReturn();
            String content = _return.getResponse().getContentAsString();

            // Assert this string is valid prediction class
            assert content != null;
            ObjectMapper mapper = new ObjectMapper();
            ImagePrediction prediction = mapper.readValue(content, ImagePrediction.class);
            assert prediction != null;
        }
	}

    @Test
	@Order(1)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testRandomPredictionFromInvalidCategory() throws Exception {
        String impairment = RandomStringUtils.random(5, true, true);

        try {
            mvc.perform(get(createBaseUrl(getModel(1L).getId()) + "/" + impairment)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded sending invalid impairment category when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains( "Unable to parse category: " + impairment + ". Expected values=[" + Arrays.asList(ImpairmentEnum.asStrings().toArray()) + "]"));
        }
	}

    @Test
	@Order(2)
    @RepeatedTest(TEST_INVOCATIONS)
	public void testPredictionFromFile() throws Exception {
        List<ImagePrediction> allImages = imageRepository.findAll();
        if (allImages.size() == 0) {
            // populate some images into the repository using the random prediction test
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
 
        String path = initialImagePrediction.getFilepath();
        String filename = path.substring(path.lastIndexOf("/")+1);
        MediaType mediaType = new MediaType("multipart", "form-data", Collections.singletonMap("boundary", "265001916915724"));
      
        byte[] inputArray = Files.readAllBytes(Paths.get(path));
        MockMultipartFile mockMultipartFile = new MockMultipartFile("image", filename, mediaType.toString(), inputArray);
        
        String url = createBaseUrl(getModel().getId());
        MvcResult _return = mvc.perform(MockMvcRequestBuilders.multipart(url)
                    .file(mockMultipartFile)
                    .contentType(mediaType))
                .andExpect(status().isOk())
                .andReturn();

        String content = _return.getResponse().getContentAsString();

        assert content != null;
        ObjectMapper mapper = new ObjectMapper();
        ImagePrediction prediction = mapper.readValue(content, ImagePrediction.class);
        assert prediction != null;

        // Assert if a picture is passed in that we have the known predictions for, the predictions will match
        assert prediction.getConf_NoImpairment().equals(initialImagePrediction.getConf_NoImpairment());
        assert prediction.getConf_VeryMildImpairment().equals(initialImagePrediction.getConf_VeryMildImpairment());
        assert prediction.getConf_MildImpairment().equals(initialImagePrediction.getConf_MildImpairment());
        assert prediction.getConf_ModerateImpairment().equals(initialImagePrediction.getConf_ModerateImpairment());
	}

    
    @Test
	@Order(2)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testPredictionFromInvalidFile() throws Exception {
        String path = imageService.returnImagePath();
		String filepath = path + "/test-" + num++ + ".json";
        File f = new File(filepath);
        if (!f.exists()) {
            JSONObject json = new JSONObject();
            json.put("i am a json file", "not a jpg");
            FileWriter file = new FileWriter(filepath);
            file.write(json.toString());
            file.close();
        }
		
		try (InputStream is = getClass().getResourceAsStream(filepath)) {
			MockMultipartFile mockMultipartFile = new MockMultipartFile("image", "test.json", 
            MediaType.APPLICATION_JSON.toString(), ByteStreams.toByteArray(new FileInputStream(filepath)));

			try {
				mvc.perform(MockMvcRequestBuilders.multipart(createBaseUrl(getModel().getId()))
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
                fail("succeeded sending invalid image when expected to fail");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
                Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                    httpException.getMessage().contains("is it a valid image?"));
                Assert.assertTrue("status code did not match 400 as expected, found: " + httpException.getStatusCode(), 
                    httpException.getStatusCode().equals(HttpStatus.valueOf(400)));
			}
		}
    }

    

    /**
	 * Run all tests in class concurrently
	 */
	@Test
	@Order(4)
	@RepeatedTest(TEST_INVOCATIONS)
    public void runAllTests() {
        int numConcurrent = 25_000;
        Class<?>[] classes  = new Class<?>[numConcurrent];
        Arrays.fill(classes, TestImagePrediction.class);
        JUnitCore.runClasses(new ParallelComputer(true, true), classes);
    }

  

    @Test
    @Order(5)
    @RepeatedTest(TEST_INVOCATIONS)
    public void testDeletePrediction() throws Exception {
        Optional<ImagePrediction> imageOpt = imageRepository.findAll().stream().findFirst();
        if (imageOpt.isEmpty()) {
            throw new Exception("Unable to find an image in image repository");
        }
        ImagePrediction initialImagePrediction = imageOpt.get();

        MvcResult _return = mvc.perform(delete(createBaseUrl(getModel(initialImagePrediction.getAssociatedModel()).getId()) + "/" + initialImagePrediction.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String content = _return.getResponse().getContentAsString();

        // Assert this string is valid prediction class
        assert content != null;
        assert Boolean.valueOf(content);
    }

    @Test
    @Order(5)
    @RepeatedTest(TEST_INVOCATIONS)  
    public void testInvalidDeletePrediction() throws Exception {
        long invalidId = 2345234523452345L;
        try {
            mvc.perform(delete(createBaseUrl(getModel().getId()) + "/" + invalidId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded deleting invalid image when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            Assert.assertTrue("status code did not match 404 as expected, found: " + httpException.getStatusCode(), 
                httpException.getStatusCode().equals(HttpStatus.valueOf(404)));
            Assert.assertTrue("status message was not as expected, found: " + httpException.getStatusCode(), 
                httpException.getMessage().contains("Unable to find prediction with specified Id: " + invalidId));
        }
    }

    public Model getModel() throws Exception {
        return getModel(null);
    }
    /**
     * Get a model from the model repository. If unable to find a model in the repository, reload the default model.
     * 
     * @return
     * @throws Exception
     */
    public Model getModel(Long id) throws Exception {
        List<Model> allModels = modelRepository.findAll();

        // load 10 models into the model repository
        long modelNum = 0L;
        for (int i=allModels.size(); i<=10; i++) {
            TestModel.runLoadModelRequest(modelService, modelNum++, getClass(), mvc);
        }
        allModels = modelRepository.findAll();

        // pick a random model
        Optional<Model> randomModelOpt = allModels.stream()
            .skip(new Random().nextInt(allModels.size()))
            .findAny();

        if (randomModelOpt.isPresent()) return randomModelOpt.get();

        throw new RuntimeException("Unable to find model");
    }
}
