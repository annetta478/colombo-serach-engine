package colombo.searchengine.utils;

public class Constants {

    public static final String URL_PATTERN = "^((?:https?://)?(?:www.)?)((?:[-\\w\\.]+)+\\.\\w+)(?:.*\\s*)*";
    public static final String PUNCTUATION_REGEX = "[\\p{Punct}\\s]+";
    public static final String TITLE_UNAVAILABLE = "Заголовок недоступен";

    public static final int DEFAULT_SEARCH_RESULT_LIMIT = 20;
    public static final int DEFAULT_SEARCH_RESULT_OFFSET = 0;
}
