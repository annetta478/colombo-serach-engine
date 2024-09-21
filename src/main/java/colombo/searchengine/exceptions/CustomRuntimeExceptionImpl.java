package colombo.searchengine.exceptions;

import org.springframework.http.HttpStatus;

public interface CustomRuntimeExceptionImpl {

    String getMessage();
    HttpStatus getHttpStatus();

}
