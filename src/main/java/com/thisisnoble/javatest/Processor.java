package com.thisisnoble.javatest;

public interface Processor {

    boolean interestedIn(Event event);

	Event process(Event event);
}
