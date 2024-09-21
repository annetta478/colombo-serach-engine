package colombo.searchengine.dto.indexing;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class IndexingResponse {

    private boolean result = true;
    private String error;

    public IndexingResponse() {}
    public IndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

}
