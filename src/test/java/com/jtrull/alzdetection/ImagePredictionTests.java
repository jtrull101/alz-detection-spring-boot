package com.jtrull.alzdetection;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jtrull.alzdetection.Model.Model;
import com.jtrull.alzdetection.Model.ModelRepository;
import com.jtrull.alzdetection.Model.ModelService;
import com.jtrull.alzdetection.Prediction.ImpairmentEnum;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
public class ImagePredictionTests {


    @Autowired
	private MockMvc mvc;

	// @Autowired
	// private ImagePredictionRepositiory imageRepository;

	// @Autowired
	// private ImagePredictionService imageService;  

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
	public void testPredictionFromFile() throws Exception {
        this.model = getModel();
		MvcResult _return = mvc.perform(post(createBaseUrl(model.getId()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = _return.getResponse().getContentAsString();
	}

    @Test
	@Order(1)
	public void testRandomPrediction() throws Exception {
        this.model = getModel();
        MvcResult _return = mvc.perform(post(createBaseUrl(model.getId()) + "/random")
                .param("modelId", String.valueOf(model.getId()))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is2xxSuccessful())
            .andReturn();
        String content = _return.getResponse().getContentAsString();

        // Assert this string is valid prediction class
        assert content != null;
	}

    @Test
	@Order(1)
	public void testRandomPredictionFromCategory() throws Exception {
        this.model = getModel();
        for (ImpairmentEnum val : ImpairmentEnum.values()) {
            MvcResult _return = mvc.perform(post(createBaseUrl(model.getId()) + "/" + val.toString())
                    .param("modelId", String.valueOf(model.getId()))
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().is2xxSuccessful())
                .andReturn();
            String content = _return.getResponse().getContentAsString();

            // Assert this string is valid prediction class
            assert content != null;
        }
	}


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
