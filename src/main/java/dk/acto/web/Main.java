package dk.acto.web;

import io.reactivex.Observable;

import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main( String[] args ) {

        ScannerService ss = new ScannerService();
        List<PageEdge> blah = new ArrayList<>();
        ss.getObservable().subscribe(blah::add);
        Observable.fromArray(args).subscribe(x -> ss.queue(x, null));
        for (PageEdge pageEdge : blah) {
            System.out.println(pageEdge);
        }
    }
}
