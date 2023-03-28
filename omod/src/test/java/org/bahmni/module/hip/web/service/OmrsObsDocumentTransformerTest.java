package org.bahmni.module.hip.web.service;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;

import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OmrsObsDocumentTransformerTest {


    public static final String PRESCRIPTION_DOC_TYPE_CONCEPT = "e53c2ed9-6c23-4683-ad9e-12c2a5d780d8";

    @Test
    @Ignore
    public void isDocumentRef() {

    }

    @Test
    @Ignore
    public void transForm() {
    }

    @Test
    @Ignore
    public void isSupportedDocumentType() {

    }

    @Test
    public void isSupportedDocument() {
        ConceptService conceptService = mock(ConceptService.class);
        AdministrationService administrationService = mock(AdministrationService.class);
        Properties abdmProperties = new Properties();
        abdmProperties.put(AbdmConfig.DocumentKind.PRESCIPTION.getMapping(), PRESCRIPTION_DOC_TYPE_CONCEPT);
        abdmProperties.put(AbdmConfig.DocumentKind.TEMPLATE.getMapping(), "4daeef16-696b-4689-98a4-8b3f3401db80");
        AbdmConfig abdmConfig = AbdmConfig.instanceFrom(abdmProperties, administrationService, conceptService);
        OmrsObsDocumentTransformer transformer = new OmrsObsDocumentTransformer(abdmConfig);
        Obs obs = new Obs();
        Concept concept = new Concept();
        concept.setUuid(PRESCRIPTION_DOC_TYPE_CONCEPT);
        obs.setConcept(concept);
        when(conceptService.getConceptByUuid(PRESCRIPTION_DOC_TYPE_CONCEPT)).thenReturn(concept);
        Assert.assertTrue(transformer.isSupportedDocument(obs, AbdmConfig.DocumentKind.PRESCIPTION));
    }
}