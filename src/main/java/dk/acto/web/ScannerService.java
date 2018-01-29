package dk.acto.web;


import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import io.vavr.control.Try;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.tika.Tika;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScannerService {
    private final PublishSubject<Tuple2<URI, PageNode>> todo;
    private final PublishSubject<PageEdge> result;
    private final Map<URI, PageNode> visited = new ConcurrentHashMap<>();
    private final Pattern schema = Pattern.compile("(http[s]?:\\/\\/[^#]+)");
    private final Pattern chars = Pattern.compile("charset=(.+)$");
    private final Pattern content = Pattern.compile("([^;]+)");
    private final WebDriver driver;
    private final Tika tika = new Tika();
    private final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build();


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
            PageEdge pe = PageEdge.builder()
                    .parent(parent)
                    .child(PageNode.builder()
                            .uri(null)
                            .contentType(null)
                            .message(site + " is an invalid url. ")
                            .build())
                    .build();
            result.onNext(pe);
        }
    }

    public Observable<PageEdge> getObservable ()
    {
        return result;
    }

    private void scanPage(Tuple2<URI, PageNode> site) {
        if (visited.keySet().contains(site._1())) {
            PageEdge pe = PageEdge.builder()
                    .parent(site._2())
                    .child(visited.get(site._1()))
                    .build();
            result.onNext(pe);
            return;
        }
        try {
            Request request = new Request.Builder()
                    .url(site._1.toURL())
                    .build();
            Response temp = client.newCall(request).execute();
            String redirect = temp.header("location");
            PageNode pn = buildPageNode(temp, site._1());

            temp.close();
            visited.put(site._1(), pn);

            PageEdge pe = PageEdge.builder()
                    .parent(site._2())
                    .child(pn)
                    .build();

            if (pe.getParent() != null) {
                result.onNext(pe);
            }

            if (!pe.hasSameHost()) {
                return;
            }


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

            if (pn.getContentType().equals("text/html")) {
                long start = System.currentTimeMillis();
                driver.get(site._1.toString());
                pn.setLoadTimeInMillis(System.currentTimeMillis() - start);
                Jsoup.parse(driver.getPageSource(), site._1.toString())
                        .select("a[href]")
                        .stream()
                        .map(x -> x.absUrl("href"))
                        .map(schema::matcher)
                        .filter(Matcher::find)
                        .forEach(x -> queue(x.group(), pn));
            }

        System.out.println("Done: " + this.visited.size());
        } catch (Throwable t) {

            PageNode pn = PageNode.builder()
                    .uri(site._1())
                    .contentType("")
                    .statusCode(-1)
                    .message("Error: " + t.getMessage())
                    .build();
            PageEdge pe = PageEdge.builder()
                    .parent(site._2())
                    .child(pn)
                    .build();
            result.onNext(pe);
        }
    }

    private PageNode buildPageNode(Response temp, URI uri) {
        String result = Try.of(() -> tika.detect(temp.body().bytes(), uri.getPath())).getOrElse("");
        String ct = temp.header("Content-Type");

        return PageNode.builder()
                .uri(uri)
                .actualType(result)
                .contentType(decodeContentType(ct))
                .charset(decodeCharset(ct))
                .statusCode(temp.code())
                .build();
    }

    private String decodeContentType(String ct) {
        Matcher m = content.matcher(ct);
        if (!m.find()) {
            return "";
        }
        return m.group(0);
    }

    private String decodeCharset(String ct) {
        Matcher m = chars.matcher(ct);
        if (!m.find()) {
            return "";
        }
        return m.group(1);
    }
}
