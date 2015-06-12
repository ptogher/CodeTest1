package com.thisisnoble.javatest.processors;

import static com.thisisnoble.javatest.util.TestIdGenerator.riskEventId;

import com.thisisnoble.javatest.Event;
import com.thisisnoble.javatest.Processor;
import com.thisisnoble.javatest.events.RiskEvent;
import com.thisisnoble.javatest.events.ShippingEvent;
import com.thisisnoble.javatest.events.TradeEvent;

public class RiskProcessor implements Processor {

    @Override
    public boolean interestedIn(Event event) {
        return event instanceof TradeEvent || event instanceof ShippingEvent;
    }

    @Override
    public Event process(final Event event) {
        String parId = event.getId();
        if (event instanceof TradeEvent)
            return new RiskEvent(riskEventId(parId), parId, calculateTradeRisk(event));
        else if (event instanceof ShippingEvent)
            return new RiskEvent(riskEventId(parId), parId, calculateShippingRisk(event));
        throw new IllegalArgumentException("unknown event for risk " + event);
    }

    private double calculateShippingRisk(Event se) {
        return ((ShippingEvent) se).getShippingCost() * 0.05;
    }

    private double calculateTradeRisk(Event te) {
        return ((TradeEvent) te).getNotional() * 0.05;
    }
}
