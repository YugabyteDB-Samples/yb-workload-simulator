package com.yugabyte.simulation.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.yugabyte.simulation.workload.AggregationWorkloadType;

@Service
public class LoggingFileManager {

	private String loggingPath = null;
	private Map<String, BufferedWriter> openFiles = new HashMap<String, BufferedWriter>();
	private final Thread loggingThread;
	private final BlockingQueue<LoggingAction> queue;
	private int counter = 0;
	private AtomicBoolean shutdownComplete = new AtomicBoolean(false);
	private AtomicBoolean doShutdown = new AtomicBoolean(false);
	

	private interface LoggingAction {
		void execute();
	}
	private class CreateFileClass implements LoggingAction {
		private String id;
		private String heading;
		
		public CreateFileClass(String id, String heading) {
			super();
			this.id = id;
			this.heading = heading;
		}

		@Override
		public void execute() {
			File file = new File(loggingPath);
			file.mkdirs();
			String filePath = loggingPath + id + ".csv";
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath), true), 1024);
				writer.write(heading);
				openFiles.put(id,  writer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class CloseFileClass implements LoggingAction {
		private String id;
		
		public CloseFileClass(String id) {
			super();
			this.id = id;
		}

		@Override
		public void execute() {
			BufferedWriter writer = openFiles.get(id);
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private class WriteLineClass implements LoggingAction {
		private String id;
		private String line;
		
		public WriteLineClass(String id, String line) {
			super();
			this.id = id;
			this.line = line;
		}

		@Override
		public void execute() {
			BufferedWriter writer = openFiles.get(id);
			if (writer != null) {
				try {
					writer.write(line);
					// Flush the aggregate counter every 10s, the others will auto flush when done.
					if (AggregationWorkloadType.AGGREGATION_WORKLOAD_NAME.equals(id) && (++counter) % 10 == 0) {
						writer.flush();
					}
					
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	private class ShutdownClass implements LoggingAction {
		@Override
		public void execute() {
			doShutdown.set(true);
		}
	}

	public LoggingFileManager() {
		queue = new ArrayBlockingQueue<LoggingAction>(10000, false);
		
		loggingThread = new Thread(() -> {
			while (!doShutdown.get()) {
				try {
					queue.take().execute();
				}
				catch (InterruptedException ie) {}
			}
			shutdownComplete.set(true);
		});
		loggingThread.setDaemon(true);
		loggingThread.start();
	}
	
	public synchronized void createFile(String id, String heading) {
		if (loggingPath != null) {
			try {
				queue.put(new CreateFileClass(id, heading));
			}
			catch (InterruptedException e) {}
		}
	}
	
	public synchronized void closeFile(String id) {
		try {
			queue.put(new CloseFileClass(id));
		}
		catch (InterruptedException e) {}
	}
	
	public synchronized void writeLine(String id, String line) {
		try {
			queue.put(new WriteLineClass(id, line));
		}
		catch (InterruptedException e) {}
	}
	
	private void closeAllLogs() {
		for (String id : openFiles.keySet()) {
			try {
				closeFile(id);
			}
			catch (Exception e) {};
		}
	}
	public synchronized void updateLoggingPreferences(boolean doLogging, String loggingPath) {
		if (doLogging && loggingPath != null && loggingPath.length() > 0) {
			if (!loggingPath.endsWith("/")) {
				this.loggingPath = loggingPath + "/";
			}
			else {
				this.loggingPath = loggingPath;
			}
			// We need to move the aggregation log to the new directory, all other
			// logs will continue where they were originally opened.
			closeFile(AggregationWorkloadType.AGGREGATION_WORKLOAD_NAME);
			createFile(AggregationWorkloadType.AGGREGATION_WORKLOAD_NAME, AggregationWorkloadType.csvHeader);
		}
		else {
			this.loggingPath = null;
			this.closeAllLogs();
		}
	}
	
	public String getLoggingPath() {
		return this.loggingPath;
	}
	
	public boolean isDoLogging() {
		return this.loggingPath != null;
	}
	
	@PreDestroy
	public void shutdown() {
		try {
			this.closeAllLogs();
			queue.put(new ShutdownClass());
			while (!shutdownComplete.get()) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
		}
	}
}
