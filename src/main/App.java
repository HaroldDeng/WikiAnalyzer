package main;

public class App {
    static final String[] include = new String[] { "#.+?", "/wiki/.+?" };
    static final String[] exclude = new String[] { "/wiki/.+?:.+?", "#cite note.+?", };

    public static void main(String[] args) {
        // String[] arr = new String[] { "en", "ja", "zh", "de", "ko", "fr", "ar", "vi"
        // };
        String[] arr = new String[] { "de" };

        for (String lang : arr) {
            MultiProcsWikiQuerier querier = new MultiProcsWikiQuerier();
            querier.setProperty(lang, 0, include, exclude);
            querier.setProcs(1, 5000);

            // number of article chain
            querier.setLimit(1);

            querier.query("data", "res_" + lang);
        }

        System.out.println("Done");
    }
}