package colombo.searchengine.dto.search;

import colombo.searchengine.utils.Constants;
import lombok.Data;

import java.util.*;

@Data
public class SearchResponse {

    boolean result = true;
    int count;
    List<SearchSiteData> data = new ArrayList<>();

    public void addSiteData(SearchSiteData siteData) {
        if(siteData != null) {
            data.add(siteData);
        }
    }

    public void orderDataByHighRelevance() {
        Collections.sort(data, new Comparator<SearchSiteData>() {
            @Override
            public int compare(SearchSiteData o1, SearchSiteData o2) {
                return Double.compare(o2.getRelevance(), o1.getRelevance());
            }
        });
    }

    public void setOffsetAndLimit(int offset, int limit) {
        if(offset < 0) {
            offset = Constants.DEFAULT_SEARCH_RESULT_OFFSET;
        }
        if(limit < 1) {
          limit = Constants.DEFAULT_SEARCH_RESULT_LIMIT;
        }
        setData(data.stream().skip(offset).limit(limit).toList());
    }

}
