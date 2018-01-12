package dk.acto.web;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import io.reactivex.Observable;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main( String[] args ) {
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
