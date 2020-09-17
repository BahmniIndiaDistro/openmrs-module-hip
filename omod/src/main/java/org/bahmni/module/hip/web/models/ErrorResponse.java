package org.bahmni.module.hip.web.models;

public class ErrorResponse {
    private String errMessage;

    public ErrorResponse() {
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "errMessage='" + errMessage + '\'' +
                '}';
    }

    public ErrorResponse(String errMessage){
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }
}
