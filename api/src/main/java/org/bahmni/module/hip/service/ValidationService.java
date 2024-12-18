package org.bahmni.module.hip.service;

public interface ValidationService {
    boolean isValidVisit(String visitUuid);

    boolean isValidPatient(String pid);

    boolean isValidProgram(String programName);

    boolean isValidHealthId(String healthId);
}
