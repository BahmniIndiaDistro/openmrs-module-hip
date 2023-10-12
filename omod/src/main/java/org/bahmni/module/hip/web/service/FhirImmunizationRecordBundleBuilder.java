package org.bahmni.module.hip.web.service;

import org.bahmni.module.hip.web.model.CareContext;
import org.bahmni.module.hip.web.model.ImmunizationRecordBundle;
import org.bahmni.module.hip.web.model.OrganizationContext;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.Concept;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.Obs;
import org.openmrs.module.fhir2.api.translators.ConceptTranslator;
import org.openmrs.module.fhir2.api.translators.EncounterTranslator;
import org.openmrs.module.fhir2.api.translators.impl.FhirTranslatorUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

public class FhirImmunizationRecordBundleBuilder {
    private final FHIRResourceMapper fhirResourceMapper;
    private final EncounterTranslator<Encounter> encounterTranslator;
    private final OrganizationContext orgContext;
    private final Map<AbdmConfig.ImmunizationAttribute, Concept> immunizationAttributeConceptMap;
    private final ConceptTranslator conceptTranslator;

    public FhirImmunizationRecordBundleBuilder(FHIRResourceMapper fhirResourceMapper,
                                               ConceptTranslator conceptTranslator,
                                               EncounterTranslator<Encounter> encounterTranslator,
                                               OrganizationContext orgContext,
                                               Map<AbdmConfig.ImmunizationAttribute, Concept> immunizationAttributeConceptMap) {
        this.fhirResourceMapper = fhirResourceMapper;
        this.encounterTranslator = encounterTranslator;
        this.orgContext = orgContext;
        this.immunizationAttributeConceptMap = immunizationAttributeConceptMap;
        this.conceptTranslator = conceptTranslator;
    }

    public List<ImmunizationRecordBundle> build(Encounter encounter) {
        return encounter.getObsAtTopLevel(false)
                .stream()
                .filter(topLevelObs -> isApplicable(topLevelObs))
                .map(obs -> buildImmunizationBundle(obs, encounter))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isApplicable(Obs rootObs) {
        Concept rootObsConcept = rootObs.getConcept();
        Concept templateConcept = immunizationAttributeConceptMap.get(AbdmConfig.ImmunizationAttribute.TEMPLATE);
        boolean isImmunizationIncident = templateConcept != null && templateConcept.getUuid().equals(rootObsConcept.getUuid());
        if (!isImmunizationIncident) {
            return false;
        }
        Concept statusConcept = immunizationAttributeConceptMap.get(AbdmConfig.ImmunizationAttribute.STATUS);
        if (statusConcept != null) {
            Optional<Obs> statusObs = rootObs.getGroupMembers().stream().filter(obs -> obs.getConcept().getUuid().equals(statusConcept.getUuid())).findFirst();
            if (statusObs.isPresent()) {
                return Optional.ofNullable(statusObs.get().getValueCoded())
                        .map((obsValue) -> isYesTerm(obsValue))
                        .orElse(false);
            }
        }
        return true;
    }

    private boolean isYesTerm(Concept obsValue) {
        return obsValue.getConceptMappings().stream().filter(cm -> {
            ConceptReferenceTerm term = cm.getConceptReferenceTerm();
            return ("SNOMED-CT".equals(term.getConceptSource().getName()) && term.getCode().equals("373066001"))
                    || ("CIEL".equals(term.getConceptSource().getName()) && term.getCode().equals("1065"));
        }).count() > 0;
    }

    private ImmunizationRecordBundle buildImmunizationBundle(Obs obs, Encounter encounter) {
        Patient patient = fhirResourceMapper.mapToPatient(encounter.getPatient());
        org.hl7.fhir.r4.model.Encounter immunizationEncounter = encounterTranslator.toFhirResource(encounter);
        immunizationEncounter.getClass_().setDisplay("Immunization Record");
        List<Practitioner> practitioners = practitionersFrom(encounter.getEncounterProviders());
        Reference patientRef = FHIRUtils.getReferenceToResource(patient);

        Bundle bundle = FHIRUtils.createBundle(
                encounter.getEncounterDatetime(),
                String.format("IR-%d-%d", obs.getEncounter().getId(), obs.getId()),
                orgContext.getWebUrl());
        Composition document = compositionFrom(encounter.getEncounterDatetime(), UUID.randomUUID().toString());
        document
          .setSubject(patientRef)
          .setAuthor(Collections.singletonList(FHIRUtils.getReferenceToResource(orgContext.getOrganization(), "Organization")));
        Immunization incident = immunizationFrom(obs);
        incident
           .setPatient(patientRef)
           .setEncounter(FHIRUtils.getReferenceToResource(immunizationEncounter))
           .setPerformer(Collections.singletonList(
              new Immunization.ImmunizationPerformerComponent(FHIRUtils.getReferenceToResource(practitioners.get(0)))
           ));
        document
          .addSection()
          .setTitle("# Immunization Record")
          .setCode(FHIRUtils.getImmunizationRecordType())
          .addEntry(FHIRUtils.getReferenceToResource(incident, "Immunization"));

        FHIRUtils.addToBundleEntry(bundle, document, false);
        FHIRUtils.addToBundleEntry(bundle, incident, false);
        FHIRUtils.addToBundleEntry(bundle, patient, false); //add patient
        FHIRUtils.addToBundleEntry(bundle, orgContext.getOrganization(), false);
        FHIRUtils.addToBundleEntry(bundle, immunizationEncounter, false);
        FHIRUtils.addToBundleEntry(bundle, practitioners, false);
        if (incident.getManufacturer() != null && incident.getManufacturer().getResource() != null) {
            FHIRUtils.addToBundleEntry(bundle, (Resource) incident.getManufacturer().getResource(), false);
        }

        CareContext careContext = CareContext.builder().careContextReference(encounter.getVisit().getUuid()).careContextType("Visit").build();
        return new ImmunizationRecordBundle(careContext, bundle);
    }

    private Immunization immunizationFrom(Obs openmrsImmunization) {
        Immunization immunization = new Immunization();
        immunization.setId(openmrsImmunization.getUuid());
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);

        if (openmrsImmunization.isObsGrouping() && openmrsImmunization.hasGroupMembers()) {
            openmrsImmunization.getGroupMembers().forEach(member -> {
                Concept memberConcept = member.getConcept();

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.VACCINE_CODE)) {
                    immunization.setVaccineCode(conceptTranslator.toFhirResource(member.getValueCoded()));
                } else if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.VACCINE_NON_CODED)) {
                    String nonCodedVaccineName = member.getValueText();
                    immunization.setVaccineCode(new CodeableConcept().addCoding(
                            new Coding().setDisplay(nonCodedVaccineName)).setText(nonCodedVaccineName));
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.OCCURRENCE_DATE)) {
                    Date valueDatetime = convertToUTC(member.getValueDate());
                    System.out.println("valueDateTime" + valueDatetime);
                    if (valueDatetime != null) {
                        immunization.setOccurrence(new DateTimeType(valueDatetime));
                    }
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.DOSE_NUMBER)) {
                    Double valueNumeric = member.getValueNumeric();
                    if (valueNumeric != null) {
                        immunization.addProtocolApplied(new Immunization.ImmunizationProtocolAppliedComponent(new PositiveIntType(valueNumeric.intValue())));
                    }
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.MANUFACTURER)) {
                    String manufacturerName = member.getValueText();
                    if (manufacturerName != null) {
                        Organization manufacturerOrg = new Organization();
                        manufacturerOrg.setId(UUID.randomUUID().toString());
                        Identifier identifier = FHIRUtils.getIdentifier(manufacturerOrg.getId(), FHIRUtils.CODE_SYSTEM_URL_ORG_TYPE,"organization");
                        identifier.setType(FHIRUtils.getCodeableConcept("other", FHIRUtils.VALUESET_URL_ORG_TYPE_HL7, "Other", ""));
                        manufacturerOrg.addIdentifier(identifier);
                        manufacturerOrg.setName(manufacturerName);
                        Reference manufacturerRef = FHIRUtils.getReferenceToResource(manufacturerOrg).setDisplay(manufacturerName);
                        immunization.setManufacturer(manufacturerRef);
                    }
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.LOT_NUMBER)) {
                    String lotNumber = member.getValueAsString(Locale.ENGLISH);
                    if (lotNumber != null) {
                        immunization.setLotNumber(lotNumber);
                    }
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.EXPIRATION_DATE)) {
                    Date valueDatetime = member.getValueDatetime();
                    if (valueDatetime != null) {
                        immunization.setExpirationDate(valueDatetime);
                    }
                }

                if (conceptMatchesForAttribute(memberConcept, AbdmConfig.ImmunizationAttribute.BRAND_NAME)) {
                    String brandName = member.getValueText();
                    if (!StringUtils.isEmpty(brandName)) {
                        Extension ext = new Extension();
                        ext.setUrl("https://nrces.in/ndhm/fhir/r4/StructureDefinition/BrandName");
                        ext.setValue(new StringType(brandName));
                        immunization.addExtension(ext);
                    }
                }
            });
        }

        immunization.getMeta().setLastUpdated(FhirTranslatorUtils.getLastUpdated(openmrsImmunization));

        return immunization;
    }

    private Date convertToUTC(Date date) {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");

        // Set the time zone of the input date to UTC
        date.setTime(date.getTime() - utcTimeZone.getOffset(date.getTime()));

        return date;
    }

    private boolean conceptMatchesForAttribute(Concept memberConcept, AbdmConfig.ImmunizationAttribute immunizationAttribute) {
        Concept mappedConcept = immunizationAttributeConceptMap.get(immunizationAttribute);
        return mappedConcept != null && memberConcept.getUuid().equals(mappedConcept.getUuid());
    }


    private Composition compositionFrom(Date compositionDate, String documentId) {
        Composition document = new Composition();
        document.setId(documentId);
        document.setDate(compositionDate); //or should it be now?
        document.setIdentifier(FHIRUtils.getIdentifier(document.getId(), orgContext.getWebUrl(), "document"));
        document.setStatus(Composition.CompositionStatus.FINAL);
        document.setType(FHIRUtils.getImmunizationRecordType());
        document.setTitle("Immunization Incident Record");
        Meta meta = new Meta();
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/ImmunizationRecord");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        document.setMeta(meta);
        return document;
    }

    private List<Practitioner> practitionersFrom(Set<EncounterProvider> encounterProviders) {
        return encounterProviders
                .stream()
                .map(fhirResourceMapper::mapToPractitioner)
                .collect(Collectors.toList());
    }
}
