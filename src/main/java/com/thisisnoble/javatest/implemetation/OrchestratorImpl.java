package com.thisisnoble.javatest.implemetation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.thisisnoble.javatest.Event;
import com.thisisnoble.javatest.Orchestrator;
import com.thisisnoble.javatest.Processor;
import com.thisisnoble.javatest.Publisher;
import com.thisisnoble.javatest.impl.CompositeEvent;

public class OrchestratorImpl implements Orchestrator {
	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Set<Processor> processors = new HashSet<>();
	private Publisher publisher;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	@Override
	public void register(final Processor processor) {
		lock.writeLock().lock();
		processors.add(processor);
		lock.writeLock().unlock();
	}

	@Override
	public void receive(final Event event) {
		// Start of a chain of events. Create a composite event to publish.
		final CompositeEvent composite = new CompositeEvent(event);
		final ChainedItem chainedItem = new ChainedItem(composite);
		chainedReceive(chainedItem, event);
	}

	public void chainedReceive(final ChainedItem chainedItem, final Event event) {
		lock.readLock().lock();
		try {
			for (Processor processor : processors) {
				if (processor.interestedIn(event)) {
					chainedItem.incrementProcessingCount();
					final EventTask task = new EventTask(chainedItem, event, this, processor);
					threadPool.submit(task);
				}
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void setup(final Publisher publisher) {
		// Expecting setup to be externally thread controlled, so no lock here.
		this.publisher = publisher;
	}

	private class EventTask implements Runnable {
		private final ChainedItem chainedItem;
		private final OrchestratorImpl orchestrator;
		private final Processor processor;
		private final Event eventToProcess;
		
		private EventTask(final ChainedItem chainedItem,
				final Event input, 
				final OrchestratorImpl orchestrator, 
				final Processor processor) {
			this.eventToProcess = input;
			this.orchestrator = orchestrator;
			this.processor = processor;
			this.chainedItem = chainedItem;
		}

		@Override
		public void run() {
			final Event output = processor.process(eventToProcess);
			chainedItem.getTargetEvent().addChild(output);
			orchestrator.chainedReceive(chainedItem, output);
			chainedItem.decrementProcessingCount();
		}
	}

	private final class ChainedItem
	{
		private final CompositeEvent targetEvent;
		private final AtomicInteger processingCount = new AtomicInteger();

		public ChainedItem(final CompositeEvent targetEvent) {
			this.targetEvent = targetEvent;
		}

		public void incrementProcessingCount() {
			int count = processingCount.incrementAndGet();
			System.out.println("Processing count increment. Now at " + count + " for target event " + targetEvent.getId() );
		}
		
		public void decrementProcessingCount() {
			//When the count reaches 0, then chain is complete, so publish.
			int count = processingCount.decrementAndGet();
			System.out.println("Processing count decrement. Now at " + count + " for target event " + targetEvent.getId() );
			if(count == 0) {
				publisher.publish(targetEvent);
			}
		}

		public CompositeEvent getTargetEvent() {
			return targetEvent;
		}

	}

}
