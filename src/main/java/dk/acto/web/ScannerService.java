package dk.acto.web;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import javaslang.Tuple2;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
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

    public ScannerService() {
        todo = PublishSubject.create();
        result = PublishSubject.create();
        todo.subscribe(this::scanPage);
    }

    public void queue(String site, PageNode parent)  {
        try {
            todo.onNext(new Tuple2<>(new URI(site), parent));
        } catch (URISyntaxException e) {
            e.printStackTrace();
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
            Connection.Response temp = Jsoup.connect(site._1().toString()).followRedirects(false).ignoreContentType(true).ignoreHttpErrors(true).execute();
            PageNode pn = new PageNode(site._1, temp.contentType(), "");
            PageEdge pe = new PageEdge(site._2(), pn, temp.statusCode());
            result.onNext(pe);
            if (!pe.hasSameHost()) {
                return;
            }
            String redirect = temp.header("location");
            if (StringUtils.isNotEmpty(redirect))
            {
                queue(redirect, pn);
                return;
            }

            if (temp.contentType().contains("text/html")) {
                temp.parse()
                        .select("a[href]")
                        .stream()
                        .map(x -> x.absUrl("href"))
                        .map(schema::matcher)
                        .filter(Matcher::find)
                        .forEach(x -> queue(x.group(), pn));
            }
        } catch (IOException e) {
            PageNode pn = new PageNode(site._1, "", "Error: " + e.getMessage());
            PageEdge pe = new PageEdge(site._2(), pn, 0);
            result.onNext(pe);
        }
    }
}
