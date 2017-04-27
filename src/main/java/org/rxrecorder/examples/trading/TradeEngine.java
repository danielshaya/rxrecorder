package org.rxrecorder.examples.trading;


import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * Created by daniel on 08/12/16.
 */
public class TradeEngine {
    private String id;

    public TradeEngine(String id){
        this.id = id;
    }

    public Observable<Order> init(Observable<Trade> tradeObservable) {
        Subject<Order, Order> orderPublisher = PublishSubject.create();
        tradeObservable.subscribeOn(Schedulers.io()).subscribe(
                trade -> {
                    System.out.println(Thread.currentThread().getName() + " " + id + " engine received " + trade);
                    orderPublisher.onNext(new Order("ORDER-" + trade.getId(), trade.getPrice(), trade.getVolume()));
                },
                e -> {
                },
                () -> {
                    System.out.println(Thread.currentThread().getName() + " " + id + " engine complete");
                    orderPublisher.onCompleted();
                });
        return orderPublisher;
    }
}
