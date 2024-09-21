package colombo.searchengine.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class SearchSiteData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
    @JsonIgnore
    private Integer absRelevance;

}
