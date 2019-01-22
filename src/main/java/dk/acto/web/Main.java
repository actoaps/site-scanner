package dk.acto.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class Main {
    private static Map<String, String> urlMap = ImmutableMap.of(
            "windows", "http://chromedriver.storage.googleapis.com/2.35/chromedriver_win32.zip",
            "linux", "http://chromedriver.storage.googleapis.com/2.35/chromedriver_linux64.zip"
    );

    public static void main(String[] args) {
        try {
            fixChromeDriver();
        } catch (IOException e) {
            log.error("Couldn't fix ChromeDriver", e);
        }

        final Mustache mdTemplate = new DefaultMustacheFactory().compile("ResponseSummaryTemplate.mustache");
        final Mustache siteMapTemplate = new DefaultMustacheFactory().compile("sitemap.xml.mustache");

        ScannerService ss = new ScannerService();
        List<PageEdge> blah = new ArrayList<>();
        ss.getObservable().subscribe(blah::add);
        Observable.fromArray(args).subscribe(x -> ss.queue(x, null));

        Try.of(() -> new PrintWriter(new File("sitemap.xml")))
                .andThen(x -> siteMapTemplate.execute(x, Stream.of(buildResponseSummary(200,
                        blah.stream()
                                .filter(PageEdge::hasSameHost)
                                .collect(Collectors.toList())))
                        .flatMap(ResponseSummary::getPages)
                        .flatMap(PageChildren::getChildren)
                        .distinct()
                        .map(PageNode::getUri)
                        .toJavaList()))
                .andThen(PrintWriter::flush)
                .andThen(PrintWriter::close);

        Try.of(() -> new PrintWriter(new File("report.md")))
                .andThen(x -> blah.stream()
                        .map(y -> y.getParent().getStatusCode())
                        .distinct()
                        .sorted()
                        .map(y -> buildResponseSummary(y, blah))
                        .forEach(y -> mdTemplate.execute(x, y)))
                .andThen(PrintWriter::flush)
                .andThen(PrintWriter::close);
    }

    private static ResponseSummary buildResponseSummary(Integer statusCode, List<PageEdge> edges) {
        ResponseSummary.ResponseSummaryBuilder builder = ResponseSummary.builder()
                .statusCode(statusCode);
        edges.stream().filter(x -> x.getParent().getStatusCode() == statusCode)
                .map(PageEdge::getParent)
                .distinct()
                .forEach(x -> builder.page(PageChildren.builder()
                        .parent(x)
                        .children(
                                edges.stream()
                                        .filter(y -> y.getParent().equals(x))
                                        .map(PageEdge::getChild)
                                        .distinct()
                                        .collect(Collectors.toList())
                        ).build()));
        return builder.build();
    }

    private static void fixChromeDriver() throws IOException {
        Optional<Path> res = Try.of(() -> Files.walk(Paths.get("."), 1))
                .get()
                .filter(x -> String.valueOf(x.getFileName()).contains("chromedriver"))
                .findFirst();

        if (!res.isPresent()) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(
                            SystemUtils.IS_OS_WINDOWS ? urlMap.get("windows") : urlMap.get("linux")
                    )
                    .build();
            Response zipResponse = client.newCall(request).execute();
            ZipInputStream zis = new ZipInputStream(zipResponse.body().byteStream());
            ZipEntry entry = zis.getNextEntry();
            var path = Paths.get(entry.getName());
            Files.copy(zis, path);
            var perms = Files.getPosixFilePermissions(path);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(path, perms);
        }

    }
}
