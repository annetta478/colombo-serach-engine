package colombo.searchengine.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern pattern = Pattern.compile(Constants.URL_PATTERN);


    public static String getSubDirectory(String url, String rootDomain) {
        if(url == null || rootDomain == null) {
            return null;
        }
        String subDirectory = removeLastSlash(url.substring(url.indexOf(rootDomain) + rootDomain.length()));
        return (subDirectory.isBlank()) ? "/" : subDirectory;
    }

    public static String removeLastSlash(String input){
        if(input == null) {
            return null;
        }
        return input.endsWith("/") ? input.substring(0, input.length()-1) : input;
    }

    public static String checkUrlAndRetriveRootDomain(String url) {
        if(url == null || url.isBlank()) {
            return null;
        }
        Matcher matcher = pattern.matcher(url);
        return matcher.matches() ? matcher.group(2) : null;
    }

    public static boolean isPunctuationChar(char c) {
        return String.valueOf(c).matches(Constants.PUNCTUATION_REGEX);
    }

}
