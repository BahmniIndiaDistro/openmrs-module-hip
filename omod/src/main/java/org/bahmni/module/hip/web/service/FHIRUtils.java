package org.bahmni.module.hip.web.service;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.bahmni.module.hip.web.service.Constants.GIF;
import static org.bahmni.module.hip.web.service.Constants.IMAGE;
import static org.bahmni.module.hip.web.service.Constants.JPEG;
import static org.bahmni.module.hip.web.service.Constants.JPG;
import static org.bahmni.module.hip.web.service.Constants.MIMETYPE_IMAGE_JPEG;
import static org.bahmni.module.hip.web.service.Constants.MIMETYPE_PDF;
import static org.bahmni.module.hip.web.service.Constants.MIMETYPE_VIDEO_MP4;
import static org.bahmni.module.hip.web.service.Constants.MIMETYPE_VIDEO_MPEG;
import static org.bahmni.module.hip.web.service.Constants.MP4;
import static org.bahmni.module.hip.web.service.Constants.MPEG;
import static org.bahmni.module.hip.web.service.Constants.PDF;
import static org.bahmni.module.hip.web.service.Constants.PNG;

public class FHIRUtils {
    public static final String VALUESET_URL_ORG_TYPE_HL7 = "http://hl7.org/fhir/ValueSet/organization-type";
    public static final String CODE_SYSTEM_URL_ORG_TYPE = "http://terminology.hl7.org/CodeSystem/organization-type";
    private static Map<String, Enumerations.AdministrativeGender> genderMap = new HashMap<String, Enumerations.AdministrativeGender>() {{
        put("M", Enumerations.AdministrativeGender.MALE);
        put("F", Enumerations.AdministrativeGender.FEMALE);
        put("O", Enumerations.AdministrativeGender.OTHER);
        put("U", Enumerations.AdministrativeGender.UNKNOWN);
    }};

    public static Bundle createBundle(Date forDate, String bundleId, String webURL) {
        Bundle bundle = new Bundle();
        bundle.setId(bundleId);
        bundle.setTimestamp(forDate);

        Identifier identifier = new Identifier();
        identifier.setSystem(Utils.ensureTrailingSlash(webURL.trim()) + "/bundle");
        identifier.setValue(bundleId);
        bundle.setIdentifier(identifier);

        Meta bundleMeta = getMeta(forDate);
        bundle.setMeta(bundleMeta);
        bundle.setType(Bundle.BundleType.DOCUMENT);
        return bundle;
    }

    public static Meta getMeta(Date forDate) {
        Meta meta = new Meta();
        meta.setLastUpdated(forDate);
        meta.setVersionId("1.0"); //TODO
        CanonicalType profileCanonical = new CanonicalType("https://nrces.in/ndhm/fhir/r4/StructureDefinition/DocumentBundle");
        List<CanonicalType> profileList = Collections.singletonList(profileCanonical);
        meta.setProfile(profileList);
        return meta;
    }

    public static Identifier getIdentifier(String id, String domain, String resType) {
        Identifier identifier = new Identifier();
        identifier.setSystem(Utils.ensureTrailingSlash(domain) + resType);
        identifier.setValue(id);
        return identifier;
    }

    public static CodeableConcept getPrescriptionType() {
        CodeableConcept type = new CodeableConcept();
        type.setText("Prescription Record");
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("440545006");
        coding.setDisplay("Prescription record");
        return type;
    }

    public static CodeableConcept getDiagnosticReportType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("721981007");
        coding.setDisplay("Diagnostic Report");
        return type;
    }

    public static CodeableConcept getImmunizationRecordType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("41000179103");
        coding.setDisplay("Immunization record");
        return type;
    }

    public static CodeableConcept getPatientDocumentType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("371530004");
        coding.setDisplay("Clinical consultation report");
        return type;
    }
    public static CodeableConcept getMedicalRecordDocumentType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_LOINC_SYSTEM);
        coding.setCode("11503-0");
        coding.setDisplay("Medical records");
        return type;
    }

    public static CodeableConcept getRecordArtifactType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("419891008");
        coding.setDisplay("Record artifact");
        return type;
    }

    public static CodeableConcept getPatientRecordType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("184216000");
        coding.setDisplay("Record artifact");
        return type;
    }

    public static CodeableConcept getOPConsultType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("371530004");
        coding.setDisplay("Clinical consultation report");
        return type;
    }

    public static CodeableConcept getChiefComplaintType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("422843007");
        coding.setDisplay("Chief complaint section");
        return type;
    }

    public static CodeableConcept getMedicalHistoryType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("422843008"); // don't know
        coding.setDisplay("Medical history");
        return type;
    }

    public static CodeableConcept getPhysicalExaminationType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("425044008"); // don't know
        coding.setDisplay("Physical examination");
        return type;
    }

    public static CodeableConcept getCarePlanType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("736368003"); // don't know
        coding.setDisplay("Care Plan");
        return type;
    }

    public static CodeableConcept getProcedureType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("36969009");
        coding.setDisplay("Procedure");
        return type;
    }

    public static CodeableConcept getOrdersType(){
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("721963009");
        coding.setDisplay("Order");
        return type;
    }

    public static void addToBundleEntry(Bundle bundle, Resource resource, boolean useIdPart) {
        String resourceType = resource.getResourceType().toString();
        String id = useIdPart ? resource.getIdElement().getIdPart() : resource.getId();
        bundle.addEntry()
                .setFullUrl(resourceType + "/" + id)
                .setResource(resource);
    }

    public static void addToBundleEntry(Bundle bundle, List<? extends Resource> resources, boolean useIdPart) {
        resources.forEach(resource -> FHIRUtils.addToBundleEntry(bundle, resource, useIdPart));
    }

    public static Organization createOrgInstance(String hfrId, String hfrName, String hfrSystem) {
        Organization organization = new Organization();
        organization.setId(hfrId);
        organization.setName(hfrName);
        Identifier identifier = organization.addIdentifier();
        identifier.setSystem(hfrSystem);
        identifier.setValue(hfrId);
        identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        return organization;
    }

    public static Reference getReferenceToResource(Resource res) {
        Reference ref = new Reference();
        ref.setResource(res);
        return ref;
    }

    public static Reference getReferenceToResource(Resource res, String type) {
        Reference ref = new Reference();
        ref.setResource(res);
        ref.setType(type);
        return ref;
    }

    public static String getDisplay(Practitioner author) {
        String prefixAsSingleString = author.getNameFirstRep().getPrefixAsSingleString();
        if ("".equals(prefixAsSingleString)) {
            return author.getNameFirstRep().getText();
        } else {
            return prefixAsSingleString.concat(" ").concat(author.getNameFirstRep().getText());
        }
    }

    public static String getTypeOfTheObsDocument(String valueText) {
        if (valueText == null) return "";
        String extension = valueText.substring(valueText.indexOf('.') + 1);
        if (extension.compareTo(JPEG) == 0 || extension.compareTo(JPG) == 0) {
            return MIMETYPE_IMAGE_JPEG;
        } else if (extension.compareTo(PNG) == 0 || extension.compareTo(GIF) == 0) {
            return IMAGE + extension;
        } else if (extension.compareTo(PDF) == 0) {
            return MIMETYPE_PDF;
        } else if (extension.compareTo(MP4) == 0) {
            return MIMETYPE_VIDEO_MP4;
        } else if (extension.compareTo(MPEG) == 0) {
            return MIMETYPE_VIDEO_MPEG;
        } else {
            return "";
        }
    }

    public static CodeableConcept getCodeableConcept(String code, String codeSystem, String display, String text) {
        CodeableConcept concept = new CodeableConcept();
        Coding coding = concept.addCoding();
        coding.setSystem(codeSystem);
        coding.setCode(code);
        coding.setDisplay(display);
        if (!Utils.isBlank(text)) {
            concept.setText(text);
        }
        return concept;
    }

    public static CodeableConcept getWellnessRecordType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("1156871006");
        coding.setDisplay("Wellness record");
        return type;
    }
    public static CodeableConcept getVitalDocumentType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("1184593002");
        coding.setDisplay("Vital sign");
        return type;
    }

    public static CodeableConcept getBodyMeasurementType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("248326004");
        coding.setDisplay("Body Measurement");
        return type;
    }

    public static CodeableConcept getPhysicalActivityType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("68130003");
        coding.setDisplay("Physical Activity");
        return type;
    }

    public static CodeableConcept getGeneralAssessmentType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("310813001");
        coding.setDisplay("General Assessment");
        return type;
    }

    public static CodeableConcept getWomenHealthType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setDisplay("Women Health");
        return type;
    }

    public static CodeableConcept getLifestyleType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("134436002");
        coding.setDisplay("Lifestyle");
        return type;
    }

	public static CodeableConcept getOtherObservationType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setDisplay("Other Observations");
        return type;
	}

    public static CodeableConcept getDocumentReferenceType() {
        CodeableConcept type = new CodeableConcept();
        Coding coding = type.addCoding();
        coding.setSystem(Constants.FHIR_SCT_SYSTEM);
        coding.setCode("308910008");
        coding.setDisplay("Document Reference");
        return type;
    }
}
