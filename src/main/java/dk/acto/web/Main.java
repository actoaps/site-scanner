package dk.acto.web;

import io.reactivex.Observable;

public class Main
{
    public static void main( String[] args ) {

        ScannerService ss = new ScannerService();
        ss.getObservable().subscribe(System.out::println);
        Observable.fromArray(args).subscribe(x -> ss.queue(x, null));
    }
}
