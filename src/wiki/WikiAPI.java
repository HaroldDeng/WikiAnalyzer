package wiki;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class WikiAPI {

    // public String page;
    // public Prop prop;

    // public boolean mobileformat;
    // public boolean noimages;

    private final static String TEXT_URL = "https://%s.wikipedia.org/w/api.php?action=parse" + "&format=json"
            + "&redirects=1" + "&page=%s" + "&prop=text" + "&mobileformat=1" + "&noimages=1" + "&utf8=1";

    private final static String RAMDOM_URL = "https://%s.wikipedia.org/w/api.php?action=query" + "&format=json"
            + "&generator=random" + "&grnnamespace=0" + "&grnlimit=%d" + "&utf8=1";


    // language type
    private String lang;


    public WikiAPI(String lang) {
        this.lang = lang;
    }

    public String queryArticle(String title) {
        title = title.replace(" ", "_");
        return urlGet(String.format(TEXT_URL, lang, title), "utf-8");
    }

    /**
     * From Wikipedia, get random articles' title
     * 
     * @param count number of title to be query
     * @return {@code null} if query unsuccessful, an utf-8 formatted
     *         {@code BufferedReader[]} otherwise
     * @exception NegativeArraySizeException parameter is negative value
     */
    public String queryRandomTitle(int count) {
        return urlGet(String.format(RAMDOM_URL, lang, count), "utf-8");
    }

    private String urlGet(String url, String format) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(url).openStream(), format))) {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
