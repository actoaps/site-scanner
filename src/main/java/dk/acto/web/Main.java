package dk.acto.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.google.common.collect.ImmutableMap;
import io.reactivex.Observable;
import io.vavr.control.Try;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main
{
    private static Map<String, String> urlMap = ImmutableMap.of(
            "windows", "http://chromedriver.storage.googleapis.com/2.35/chromedriver_win32.zip",
            "linux", "http://chromedriver.storage.googleapis.com/2.35/chromedriver_linux64.zip"
    );

    public static void main( String[] args ) throws IOException {
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
            String name = entry.getName();
            Files.copy(zis, Paths.get(name));
        }

        final Mustache mustache = new DefaultMustacheFactory().compile("ResponseSummaryTemplate.mustache");

        ScannerService ss = new ScannerService();
        List<PageEdge> blah = new ArrayList<>();
        ss.getObservable().subscribe(blah::add);
        Observable.fromArray(args).subscribe(x -> ss.queue(x, null));

        blah.stream().map(PageEdge::getStatusCode).distinct().sorted().map(x -> buildResponseSummarty(x, blah)).forEach(x -> {
            try {
                mustache.execute(new PrintWriter(System.out), x).flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private static ResponseSummary buildResponseSummarty(Integer statusCode, List<PageEdge> edges) {
        ResponseSummary.ResponseSummaryBuilder builder = ResponseSummary.builder()
                .statusCode(statusCode);
        edges.stream().filter(x -> x.getStatusCode() == statusCode)
                .map(PageEdge::getChild)
                .distinct()
                .forEach(builder::node);
        return builder.build();
    }
}
