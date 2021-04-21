package org.bahmni.module.hip.web.controller;

import junit.framework.TestCase;
import org.bahmni.module.hip.web.TestConfiguration;
import org.bahmni.module.hip.web.service.ExistingPatientService;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Patient;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PatientController.class, TestConfiguration.class})
@WebAppConfiguration
public class PatientControllerTest extends TestCase {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ExistingPatientService existingPatientService;

    @Before
    public void setup() {
        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @Test
    public void shouldReturn200OKWhenMatchingPatientFound() throws Exception {
        Patient patient = mock(Patient.class);
        List<Patient> patients = new ArrayList<>();
        patients.add(patient);
        JSONObject existingPatientsListObject = new JSONObject();
        existingPatientsListObject.put("PatientName:", "sam tom");
        existingPatientsListObject.put("PatientAge:", "35");
        existingPatientsListObject.put("PatientGender:", "M");
        existingPatientsListObject.put("PatientAddress:", "null, null");

        when(existingPatientService.getMatchingPatients(anyString(), anyInt(), anyString()))
                .thenReturn(patients);
        when(existingPatientService.getMatchingPatientDetails(patients))
                .thenReturn(existingPatientsListObject);

        mockMvc.perform(get(String.format("/rest/%s/hip/existingPatients", RestConstants.VERSION_1))
                .param("patientName", "sam tom")
                .param("patientYearOfBirth", "1985")
                .param("patientGender", "M")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturn400BadRequestWhenNoMatchingPatientFound() throws Exception {
        List<Patient> patients = new ArrayList<>();
        JSONObject existingPatientsListObject = new JSONObject();
        existingPatientsListObject.put("PatientName:", "sam tom");
        existingPatientsListObject.put("PatientAge:", "35");
        existingPatientsListObject.put("PatientGender:", "M");
        existingPatientsListObject.put("PatientAddress:", "null, null");

        when(existingPatientService.getMatchingPatients(anyString(), anyInt(), anyString()))
                .thenReturn(patients);
        when(existingPatientService.getMatchingPatientDetails(patients))
                .thenReturn(existingPatientsListObject);

        mockMvc.perform(get(String.format("/rest/%s/hip/existingPatients", RestConstants.VERSION_1))
                .param("patientName", "sam tom")
                .param("patientYearOfBirth", "1985")
                .param("patientGender", "M")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}