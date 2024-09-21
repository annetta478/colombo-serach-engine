package colombo.searchengine.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;


public class ForkIndexingException {

    private AtomicBoolean hasError;
    private Exception exception;

    public ForkIndexingException() {
        hasError = new AtomicBoolean(false);
    }

    public AtomicBoolean getHasError() {
        return hasError;
    }
    public void setHasError(AtomicBoolean hasError) {
        this.hasError = hasError;
    }
    public Exception getException() {
        return exception;
    }
    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getStackTrace() {
        if(exception == null){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTraceElements = exception.getStackTrace();
        for (StackTraceElement stackTraceElement : stackTraceElements) {
            sb.append(stackTraceElement).append("/n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ForkIndexingException{");
        sb.append("hasError=").append(hasError);
        sb.append(", exception=").append(exception);
        sb.append('}');
        return sb.toString();
    }
}
