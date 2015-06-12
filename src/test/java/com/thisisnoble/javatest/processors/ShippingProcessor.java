package com.thisisnoble.javatest.processors;

import static com.thisisnoble.javatest.util.TestIdGenerator.shipEventId;

import com.thisisnoble.javatest.Event;
import com.thisisnoble.javatest.Processor;
import com.thisisnoble.javatest.events.ShippingEvent;
import com.thisisnoble.javatest.events.TradeEvent;

public class ShippingProcessor implements Processor {


    @Override
    public boolean interestedIn(Event event) {
        return event instanceof TradeEvent;
    }

    @Override
    public Event process(final Event event) {
        String parId = event.getId();
        if (event instanceof TradeEvent)
            return new ShippingEvent(shipEventId(parId), parId, calculateTradeShipping(event));
        throw new IllegalArgumentException("unknown event for shipping " + event);
    }

    private double calculateTradeShipping(Event te) {
        return ((TradeEvent) te).getNotional() * 0.2;
    }
}
