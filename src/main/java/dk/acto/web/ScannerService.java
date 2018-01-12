package dk.acto.web;


import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.vavr.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.tika.Tika;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScannerService {
    private final PublishSubject<Tuple2<URI, PageNode>> todo;
    private final PublishSubject<PageEdge> result;
    private final Set<URI> visited = ConcurrentHashMap.newKeySet();
    private final Pattern schema = Pattern.compile("(http[s]?:\\/\\/[^#]+)");
    private final WebDriver driver;
    private final Tika tika = new Tika();

    public ScannerService() {
        todo = PublishSubject.create();
        result = PublishSubject.create();
        todo.subscribe(this::scanPage);
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--headless");

        driver = new ChromeDriver(chromeOptions);
    }

    public void queue(String site, PageNode parent)  {
        try {
            URIBuilder builder = new URIBuilder(site).clearParameters();
            todo.onNext(new Tuple2<>(builder.build(), parent));
        } catch (URISyntaxException e) {
            PageEdge pe = new PageEdge(parent, PageNode.builder().uri(null).contentType(null).message(site + " is an invalid url. ").build(), 0);
            result.onNext(pe);
        }
    }

    public Observable<PageEdge> getObservable ()
    {
        return result;
    }

    public void scanPage(Tuple2<URI, PageNode> site) {
        if (visited.contains(site._1()))
            return;
        visited.add(site._1());
        try {
            Connection.Response temp = Jsoup.connect(site._1().toString())
                    .followRedirects(false)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();

            PageNode pn = PageNode.builder()
                    .uri(site._1())
                    .contentType(temp.contentType())
                    .build();
            PageEdge pe = PageEdge.builder()
                    .parent(site._2())
                    .child(pn)
                    .statusCode(temp.statusCode())
                    .build();

            result.onNext(pe);

            if (!pe.hasSameHost()) {
                return;
            }

            String redirect = temp.header("location");

            if (StringUtils.isNotEmpty(redirect))
            {
                if (redirect.startsWith("/")) {
                    redirect = site._1.getScheme()
                            .concat("://")
                            .concat(site._1.getHost())
                            .concat(redirect);
                }
                queue(redirect, pn);
                return;
            }

            String result = tika.detect(temp.bodyAsBytes(), site._1.getPath());
            System.out.println(String.format("Tika says: %s is %s, server reported %s.", site._1.getPath(), result, temp.contentType()));

            if (temp.contentType().contains("text/html")) {
                if (!result.equals("text/html")) {
                    pn.getMessages().add("Server claims Content-type is %s but was detected as %s");
                }
                long start = System.currentTimeMillis();
                driver.get(site._1.toString());
                System.out.println(String.format("Page load took %sms", System.currentTimeMillis() - start));
                Jsoup.parse(driver.getPageSource(), site._1.toString())
                        .select("a[href]")
                        .stream()
                        .map(x -> x.absUrl("href"))
                        .map(schema::matcher)
                        .filter(Matcher::find)
                        .forEach(x -> queue(x.group(), pn));
            }
        } catch (Throwable t) {

            PageNode pn = PageNode.builder()
                    .uri(site._1())
                    .contentType("")
                    .message("Error: " + t.getMessage())
                    .build();
            PageEdge pe = PageEdge.builder()
                    .parent(site._2())
                    .child(pn)
                    .statusCode(-1)
                    .build();
            result.onNext(pe);
        }
    }
}
