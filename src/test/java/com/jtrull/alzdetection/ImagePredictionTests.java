package com.jtrull.alzdetection;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jtrull.alzdetection.Image.ImagePrediction;
import com.jtrull.alzdetection.Image.ImageRepository;
import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;
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
    private ModelRepository modelRepository;

    @Autowired
    private ModelService modelService;

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
	@Order(2)
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
        
        String url = createBaseUrl(getModel().getId()) + "/";
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
    @Order(3)
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

    /**
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
        try {
            model = modelService.loadDefaultModel();
        } catch (Exception e) {
            assertFalse(true, e.getMessage());
        }
        return model;
        
    }
}
