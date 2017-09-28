package com.cus.metime.review.web.rest;

import com.cus.metime.review.ReviewApp;

import com.cus.metime.review.config.SecurityBeanOverrideConfiguration;

import com.cus.metime.review.domain.HelpFull;
import com.cus.metime.review.repository.HelpFullRepository;
import com.cus.metime.review.service.HelpFullService;
import com.cus.metime.review.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cus.metime.review.domain.enumeration.HelpFullCategory;
import com.cus.metime.review.service.ReviewService;
/**
 * Test class for the HelpFullResource REST controller.
 *
 * @see HelpFullResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ReviewApp.class, SecurityBeanOverrideConfiguration.class})
public class HelpFullResourceIntTest {

    private static final HelpFullCategory DEFAULT_HELPFULL_CATEGORY = HelpFullCategory.HELPFULL;
    private static final HelpFullCategory UPDATED_HELPFULL_CATEGORY = HelpFullCategory.UNHELPFULL;

    @Autowired
    private HelpFullRepository helpFullRepository;

    @Autowired
    private HelpFullService helpFullService;
    
    @Autowired
    private ReviewService reviewService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restHelpFullMockMvc;

    private HelpFull helpFull;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final HelpFullResource helpFullResource = new HelpFullResource(helpFullService, reviewService);
        this.restHelpFullMockMvc = MockMvcBuilders.standaloneSetup(helpFullResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static HelpFull createEntity(EntityManager em) {
        HelpFull helpFull = new HelpFull()
            .helpfullCategory(DEFAULT_HELPFULL_CATEGORY);
        return helpFull;
    }

    @Before
    public void initTest() {
        helpFull = createEntity(em);
    }

    @Test
    @Transactional
    public void createHelpFull() throws Exception {
        int databaseSizeBeforeCreate = helpFullRepository.findAll().size();

        // Create the HelpFull
        restHelpFullMockMvc.perform(post("/api/help-fulls")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(helpFull)))
            .andExpect(status().isCreated());

        // Validate the HelpFull in the database
        List<HelpFull> helpFullList = helpFullRepository.findAll();
        assertThat(helpFullList).hasSize(databaseSizeBeforeCreate + 1);
        HelpFull testHelpFull = helpFullList.get(helpFullList.size() - 1);
        assertThat(testHelpFull.getHelpfullCategory()).isEqualTo(DEFAULT_HELPFULL_CATEGORY);
    }

    @Test
    @Transactional
    public void createHelpFullWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = helpFullRepository.findAll().size();

        // Create the HelpFull with an existing ID
        helpFull.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restHelpFullMockMvc.perform(post("/api/help-fulls")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(helpFull)))
            .andExpect(status().isBadRequest());

        // Validate the HelpFull in the database
        List<HelpFull> helpFullList = helpFullRepository.findAll();
        assertThat(helpFullList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void getAllHelpFulls() throws Exception {
        // Initialize the database
        helpFullRepository.saveAndFlush(helpFull);

        // Get all the helpFullList
        restHelpFullMockMvc.perform(get("/api/help-fulls?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(helpFull.getId().intValue())))
            .andExpect(jsonPath("$.[*].helpfullCategory").value(hasItem(DEFAULT_HELPFULL_CATEGORY.toString())));
    }

    @Test
    @Transactional
    public void getHelpFull() throws Exception {
        // Initialize the database
        helpFullRepository.saveAndFlush(helpFull);

        // Get the helpFull
        restHelpFullMockMvc.perform(get("/api/help-fulls/{id}", helpFull.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(helpFull.getId().intValue()))
            .andExpect(jsonPath("$.helpfullCategory").value(DEFAULT_HELPFULL_CATEGORY.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingHelpFull() throws Exception {
        // Get the helpFull
        restHelpFullMockMvc.perform(get("/api/help-fulls/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateHelpFull() throws Exception {
        // Initialize the database
//        helpFullService.save(helpFull);

        int databaseSizeBeforeUpdate = helpFullRepository.findAll().size();

        // Update the helpFull
        HelpFull updatedHelpFull = helpFullRepository.findOne(helpFull.getId());
        updatedHelpFull
            .helpfullCategory(UPDATED_HELPFULL_CATEGORY);

        restHelpFullMockMvc.perform(put("/api/help-fulls")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedHelpFull)))
            .andExpect(status().isOk());

        // Validate the HelpFull in the database
        List<HelpFull> helpFullList = helpFullRepository.findAll();
        assertThat(helpFullList).hasSize(databaseSizeBeforeUpdate);
        HelpFull testHelpFull = helpFullList.get(helpFullList.size() - 1);
        assertThat(testHelpFull.getHelpfullCategory()).isEqualTo(UPDATED_HELPFULL_CATEGORY);
    }

    @Test
    @Transactional
    public void updateNonExistingHelpFull() throws Exception {
        int databaseSizeBeforeUpdate = helpFullRepository.findAll().size();

        // Create the HelpFull

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restHelpFullMockMvc.perform(put("/api/help-fulls")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(helpFull)))
            .andExpect(status().isCreated());

        // Validate the HelpFull in the database
        List<HelpFull> helpFullList = helpFullRepository.findAll();
        assertThat(helpFullList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteHelpFull() throws Exception {
        // Initialize the database
//        helpFullService.save(helpFull);

        int databaseSizeBeforeDelete = helpFullRepository.findAll().size();

        // Get the helpFull
        restHelpFullMockMvc.perform(delete("/api/help-fulls/{id}", helpFull.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<HelpFull> helpFullList = helpFullRepository.findAll();
        assertThat(helpFullList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(HelpFull.class);
        HelpFull helpFull1 = new HelpFull();
        helpFull1.setId(1L);
        HelpFull helpFull2 = new HelpFull();
        helpFull2.setId(helpFull1.getId());
        assertThat(helpFull1).isEqualTo(helpFull2);
        helpFull2.setId(2L);
        assertThat(helpFull1).isNotEqualTo(helpFull2);
        helpFull1.setId(null);
        assertThat(helpFull1).isNotEqualTo(helpFull2);
    }
}
