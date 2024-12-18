package org.bahmni.module.hip.service;

import org.bahmni.module.hip.Config;
import org.bahmni.module.hip.model.OrganizationContext;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class OrganizationContextService {

    static final String VISIT_LOCATION_TAG = "Visit Location";
    static final String ORGANIZATION_LOCATION_TAG = "Organization";

    static final String LOCATION_ATTRIBUTE_EMAIL_ID = "Email";
    static final String LOCATION_ATTRIBUTE_PHONE_NUMBER = "Phone Number";
    static final String LOCATION_ATTRIBUTE_WEBSITE_URL = "Website";
    static final String LOCATION_ATTRIBUTE_ABDM_HFR_ID = "ABDM HFR ID";
    public static final String ABDM_HFR_SYSTEM = "https://facility.abdm.gov.in/";

    /**
     * Please use buildContext with location
     * @return
     */
    @Deprecated
    OrganizationContext buildContext() {
        Organization organization = createOrganizationInstance();
        return OrganizationContext.builder()
                .organization(organization)
                .webUrl(getOrganizationUrl(organization))
                .build();
    }

    OrganizationContext buildContext(Optional<Location> org) {
        Organization organization = org.map(this::buildOrg).orElseGet(this::createOrganizationInstance);
        return OrganizationContext.builder()
                .organization(organization)
                .webUrl(getOrganizationUrl(organization))
                .build();
    }

    private String getOrganizationUrl(Organization organization) {
        List<Identifier> identifiers = organization.getIdentifier().stream().filter(identifier -> identifier.getUse().equals(Identifier.IdentifierUse.USUAL)).collect(Collectors.toList());
        return !identifiers.isEmpty() ? identifiers.get(0).getSystem() : webURL();
    }

    private Organization buildOrg(Location location) {
        Organization organization = new Organization();
        organization.setId(location.getUuid());

        List<LocationAttribute> abdmHFRIds = getAttributesByTag(location, LOCATION_ATTRIBUTE_ABDM_HFR_ID);
        if (!abdmHFRIds.isEmpty()) {
            organization.addIdentifier(createOrgIdentifier(ABDM_HFR_SYSTEM, abdmHFRIds.get(0).getValue().toString(), Identifier.IdentifierUse.OFFICIAL));
        }

        List<LocationAttribute> websiteUrls = getAttributesByTag(location, LOCATION_ATTRIBUTE_WEBSITE_URL);
        if (!websiteUrls.isEmpty()) {
            organization.addIdentifier(createOrgIdentifier(websiteUrls.get(0).getValue().toString(), location.getUuid(), Identifier.IdentifierUse.USUAL));
        }

        organization.setName(location.getName());

        List<LocationAttribute> phoneNumbers = getAttributesByTag(location, LOCATION_ATTRIBUTE_PHONE_NUMBER);
        if (!phoneNumbers.isEmpty()) {
            ContactPoint phoneContact = getContactPoint(ContactPoint.ContactPointSystem.PHONE, phoneNumbers.get(0));
            organization.addTelecom(phoneContact);
        }

        List<LocationAttribute> emailIds = getAttributesByTag(location, LOCATION_ATTRIBUTE_EMAIL_ID);
        if (!emailIds.isEmpty()) {
            ContactPoint emailContact = getContactPoint(ContactPoint.ContactPointSystem.EMAIL, emailIds.get(0));
            organization.addTelecom(emailContact);
        }
        return organization;
    }

    private Identifier createOrgIdentifier(String systemUrl, String value, Identifier.IdentifierUse identifierUse) {
        Identifier identifier = new Identifier();
        identifier.setSystem(systemUrl);
        identifier.setType(FHIRUtils.getCodeableConcept("PRN", "http://terminology.hl7.org/CodeSystem/v2-0203", "Provider number", ""));
        if (identifierUse != Identifier.IdentifierUse.NULL) {
            identifier.setUse(identifierUse);
        }
        identifier.setValue(value);
        return identifier;
    }

    private ContactPoint getContactPoint(ContactPoint.ContactPointSystem system, LocationAttribute attribute) {
        ContactPoint phoneContact = new ContactPoint();
        phoneContact.setSystem(system);
        phoneContact.setValue(attribute.getValue().toString());
        phoneContact.setUse(ContactPoint.ContactPointUse.WORK);
        return phoneContact;
    }

    private List<LocationAttribute> getAttributesByTag(Location location, String tagName) {
        return location.getAttributes().stream().filter(attr -> attr.getAttributeType().getName().equalsIgnoreCase(tagName)).collect(Collectors.toList());
    }

    private String webURL() {
        AdministrationService administrationService = Context.getAdministrationService();
        return administrationService.getGlobalProperty(Config.PROP_HFR_URL.getValue());
    }

    private Organization createOrganizationInstance() {
        AdministrationService administrationService = Context.getAdministrationService();
        String hfrId = administrationService.getGlobalProperty(Config.PROP_HFR_ID.getValue());
        String hfrName = administrationService.getGlobalProperty(Config.PROP_HFR_NAME.getValue());
        String hfrSystem = administrationService.getGlobalProperty(Config.PROP_HFR_SYSTEM.getValue());
        return FHIRUtils.createOrgInstance(hfrId, hfrName, hfrSystem);
    }

    public static Optional<Location> findOrganization(Location location) {
        Optional<Location> orgLocation = identifyLocationByTag(location, ORGANIZATION_LOCATION_TAG);
        if (!orgLocation.isPresent()) {
            return identifyLocationByTag(location, VISIT_LOCATION_TAG);
        } else {
            return orgLocation;
        }
    }

    private static Optional<Location> identifyLocationByTag(Location location, String tagName) {
        if (location == null) {
            return Optional.empty();
        }
        boolean isMatched = location.getTags().size() > 0 && location.getTags().stream().anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));
        return isMatched ? Optional.of(location) : identifyLocationByTag(location.getParentLocation(), tagName);
    }
}
