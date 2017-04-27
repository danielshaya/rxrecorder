package org.rxrecorder.examples.trading;

import net.openhft.chronicle.wire.Marshallable;

/**
 * Created by daniel on 06/12/16.
 */
public class Order implements Marshallable {
    private double price;
    private int volume;
    private String id;

    public Order(){

    }

    public Order(String id, double price, int volume) {
        this.price = price;
        this.volume = volume;
        this.id = id;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Order{" +
                "price=" + price +
                ", volume=" + volume +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Order trade = (Order) o;

        if (Double.compare(trade.price, price) != 0) return false;
        if (volume != trade.volume) return false;
        return id != null ? id.equals(trade.id) : trade.id == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(price);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + volume;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
