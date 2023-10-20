package com.jtrull.alzdetection;

import org.junit.experimental.ParallelComputer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import com.jtrull.alzdetection.Image.ImagePrediction;
import com.jtrull.alzdetection.Image.ImageRepository;
import com.jtrull.alzdetection.Image.ImageService;
import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class ImagePredictionTests {
    @Autowired
	private MockMvc mvc;

	@Autowired
	private ImageRepository imageRepository;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ModelRepository modelRepository;

    Model model = null;

    private static final String BASE_URL = "/api/v1/model";

    private String createBaseUrl(Long modelId) {
        return BASE_URL + "/" + String.valueOf(modelId) + "/predict";
    }

    @Test
	@Order(1)
    @RepeatedTest(10)
	public void testRandomPrediction() throws Exception {
        MvcResult _return = mvc.perform(get(createBaseUrl(getModel().getId()) + "/random")
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
    @RepeatedTest(10)
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
    @RepeatedTest(10)
	public void testRandomPredictionFromInvalidCategory() throws Exception {
        String impairment = RandomStringUtils.random(5, true, true);

        try {
            mvc.perform(get(createBaseUrl(getModel().getId()) + "/" + impairment)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded sending invalid impairment category when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            assert httpException.getStatusCode().equals(HttpStatus.valueOf(400));
            assert httpException.getMessage().contains( "Unable to parse category: " + impairment + ". Expected values=[" + Arrays.asList(ImpairmentEnum.asStrings().toArray()) + "]");
        }
	}

    @Test
	@Order(2)
    @RepeatedTest(10)
	public void testPredictionFromFile() throws Exception {
        Optional<ImagePrediction> imageOpt = imageRepository.findAll().stream().findAny();
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
    @RepeatedTest(10)
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

			String url = createBaseUrl(getModel().getId());
			try {
				mvc.perform(MockMvcRequestBuilders.multipart(url)
						.file(mockMultipartFile)
						.contentType(MediaType.APPLICATION_JSON.toString()))
						.andExpect(status().is4xxClientError());
                fail("succeeded sending invalid image when expected to fail");

			} catch (ServletException e) {
				RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
                assert httpException.getStatusCode().equals(HttpStatus.valueOf(400));
				assert httpException.getMessage().contains("is it a valid image?");
			}
		}
    }

    @Test
    @Order(3)
    @RepeatedTest(10)
    public void testDeletePrediction() throws Exception {
        Optional<ImagePrediction> imageOpt = imageRepository.findAll().stream().findFirst();
        if (imageOpt.isEmpty()) {
            throw new Exception("Unable to find an image in image repository");
        }
        ImagePrediction initialImagePrediction = imageOpt.get();

        MvcResult _return = mvc.perform(delete(createBaseUrl(getModel().getId()) + "/" + initialImagePrediction.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String content = _return.getResponse().getContentAsString();

        // Assert this string is valid prediction class
        assert content != null;
        assert Boolean.valueOf(content);
    }

    @Test
    @Order(3)
    @RepeatedTest(10)
    public void testInvalidDeletePrediction() throws Exception {
        long invalidId = 2345234523452345L;
        try {
            mvc.perform(delete(createBaseUrl(getModel().getId()) + "/" + invalidId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
            fail("succeeded deleting invalid image when expected to fail");

        } catch (ServletException e) {
            RestClientResponseException httpException = (RestClientResponseException) e.getRootCause();
            assert httpException.getStatusCode().equals(HttpStatus.valueOf(404));
            assert httpException.getMessage().contains("Unable to find prediction with specified Id: " + invalidId);
        }
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
        Arrays.fill(classes, ImagePredictionTests.class);
        JUnitCore.runClasses(new ParallelComputer(true, true), classes);
    }

    /**
     * Get a model from the model repository. If unable to find a model in the repository, reload the default model.
     * 
     * @return
     */
    public Model getModel() {
        if (model != null) return model;
        List<Model> allModels = modelRepository.findAll();
        if (allModels.size() > 0) {
            model = allModels.get(0);
            return model;
        }
        throw new RuntimeException("Unable to find default model, is default model now deletable?");
    }
}
