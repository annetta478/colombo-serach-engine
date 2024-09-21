package colombo.searchengine.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import colombo.searchengine.dto.indexing.IndexingResponse;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<IndexingResponse> handleException(RuntimeException e) {
        LOGGER.error("RuntimeException: ", e);
        if(e instanceof CustomRuntimeExceptionImpl){
            CustomRuntimeExceptionImpl exception = (CustomRuntimeExceptionImpl)e;
            IndexingResponse response = new IndexingResponse(false, exception.getMessage());
            return new ResponseEntity<>(response, exception.getHttpStatus());
        }
        return new ResponseEntity<>(new IndexingResponse(false, Messages.GENERIC_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);

    }

}
