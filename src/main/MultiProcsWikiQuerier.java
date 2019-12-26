package main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import graph.Graph;
import graph.HeuristicSearchGraph;
import wiki.WikiAPI;

public class MultiProcsWikiQuerier {
    private Thread tIds[]; // child threads ids
    private int limit;
    private int procs;

    private Bundle bundle;

    private LinkedBlockingDeque<String> upward, downward;

    /**
     * 
     * @param procs
     * @param nth      nth element being extract and follow
     * @param limit
     * @param lang     language
     * @param omission elements will be skip, case sensitive
     */
    public MultiProcsWikiQuerier() {
        upward = new LinkedBlockingDeque<String>();
        downward = new LinkedBlockingDeque<String>();
        bundle = new Bundle();
    }

    public void setProperty(String lang, int nth, String[] include, String[] exclude) {
        bundle.lang = lang;
        bundle.nth = nth;
        bundle.include = include;
        bundle.exclude = exclude;
    }

    public void setProcs(int numOfProcs, int delayMul) {
        this.procs = numOfProcs;

        tIds = new Thread[numOfProcs];
        bundle.delayMul = delayMul;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * Receive data from child processes, store it as utf-8 format in user specified
     * location
     * 
     * @param dest       destination that files will be store.
     * @param fName      file name
     * @param identifier very first identifier
     * 
     */
    public void query(String dest, String fName) {
        // Get random articles, then buffered into the downward
        // message queue.
        WikiAPI api = new WikiAPI(bundle.lang);
        for (String title : retriveTitles(api.queryRandomTitle(limit), limit)) {
            downward.add(title);
        }
        for (int i = 0; i < procs; ++i) {
            downward.add(bundle.killSign);
        }

        // create new file, then write current time as header
        File file = new File(String.format("%s/%s.txt", dest, fName));
        appendToFile(LocalDateTime.now().toString(), file);

        // create threads
        for (int i = 0; i < procs; i++) {
            tIds[i] = new Thread(new WikiQuerier(bundle, upward, downward));
            tIds[i].start();
        }

        try {
            int alive = procs;
            while (alive > 0) {
                while (upward.size() > 0) {
                    String res = upward.poll();
                    if (res.equals(bundle.killSign)) {
                        --alive;
                    } else {
                        appendToFile(res, file);
                    }
                }
                Thread.sleep(200);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Append utf-8 content to file
     * 
     * @param s    content to be write to file
     * @param file file object
     */
    private void appendToFile(String s, File file) {
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8);
            BufferedWriter w = new BufferedWriter(osw);
            w.write(s);
            w.newLine();
            w.flush();
            w.close();
        } catch (IOException e) {
            System.err.println("Error: Failed to output to file.");
        }
    }

    private String[] retriveTitles(String JSON, int size) {
        String[] ret = new String[size];
        JSONObject pages = new JSONObject(JSON).getJSONObject("query").getJSONObject("pages");
        Iterator<String> iter = pages.keys();
        int i = 0;
        while (i < size && iter.hasNext()) {
            ret[i] = pages.getJSONObject(iter.next()).getString("title");
            ++i;
        }

        return ret;
    }
}

class WikiQuerier extends Thread {
    private Bundle bundleInfo;
    private Graph hsg;
    private WikiAPI api;
    private LinkedBlockingDeque<String> upward, downward;
    private JSONObject json;

    /**
     * Constructor
     * 
     * @param ranPage,  url of the random page
     * @param n,        n of nth url extract from webpage
     * @param limit,    maximun number of pages chain will be queried
     * @param msgQueue, message passing queue, from current process to parent
     *                  process
     */
    public WikiQuerier(Bundle bundleInfo, LinkedBlockingDeque<String> upward, LinkedBlockingDeque<String> downward) {
        this.bundleInfo = bundleInfo;
        this.upward = upward;
        this.downward = downward;
        hsg = HeuristicSearchGraph.getInstance();
        api = new WikiAPI(bundleInfo.lang);
    }

    @Override
    public void run() {
        ArrayList<String> record = new ArrayList<String>();
        String title;

        while (downward.size() > 0) {
            title = downward.poll();
            if (title.equals(bundleInfo.killSign)) {
                // job is done, kill thread
                upward.add(bundleInfo.killSign);
                return;
            }
            // title = "統計學應用領域列表";
            title = "Mikroökonomie";

            while (!title.isEmpty()) {
                String rawIn = api.queryArticle(title);
                json = new JSONObject(rawIn);
                if (json.has("parse")) {
                    json = json.getJSONObject("parse");
                } else {
                    // page not exist
                    break;
                }
                title = json.getString("title");
                record.add(title);

                if (!hsg.contains(title.toUpperCase())) {
                    // for consistency, use upper case letters
                    hsg.insert(title.toUpperCase());
                } else {
                    break;
                }
                System.out.println(title);

                String next = getNth();
                if (!next.isEmpty() && !next.startsWith("#")) {
                    title = next.substring(6).split("#")[0];
                }

                // delay
                try {
                    Thread.sleep((int) (bundleInfo.delayMul * Math.random()));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            upward.add(String.join(" => ", record));
            record.clear(); // reset
        }
    }

    private String getNth() {
        String html = json.getJSONObject("text").getString("*");
        Element body = Jsoup.parseBodyFragment(html).body();
        System.out.println(body);
        for (Element a_tag : body.select("div.mw-parser-output>div>p>a,div.mw-parser-output>div>ul>li>a")) {
            try {
                // extract title
                final String T = URLDecoder.decode(a_tag.attr("href"), "utf-8").replace("_", " ");
                if (bundleInfo.include == null || Arrays.stream(bundleInfo.include).anyMatch(e -> T.matches(e))) {
                    if (bundleInfo.exclude == null || Arrays.stream(bundleInfo.exclude).noneMatch(e -> T.matches(e))) {
                        System.out.println(T + "   " + a_tag.cssSelector());
                        // return T;
                    }
                }
            } catch (Exception exc) {
                return "";
            }

        }

        System.exit(0);
        return "";
    }
}

class Bundle {
    int nth = 0;
    int delayMul = 1000;

    String lang = "en";
    String killSign = "###STOP###";

    String[] include;
    String[] exclude;
}