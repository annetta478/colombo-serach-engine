package colombo.searchengine.exceptions;

import org.springframework.http.HttpStatus;


public class CustomException extends RuntimeException implements CustomRuntimeExceptionImpl {

    private String message;
    private HttpStatus httpStatus;

    public CustomException(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
