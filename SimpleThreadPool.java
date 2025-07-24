import java.util.concurrent.*;
import java.util.*;

public class SimpleThreadPool {
    private final int numThreads;
    private final List<Worker> workers;
    private final BlockingQueue<Runnable> taskQueue;
    private volatile boolean isShutdown = false;

    public SimpleThreadPool(int numThreads) {
        this.numThreads = numThreads;
        this.taskQueue = new LinkedBlockingQueue<>();
        this.workers = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker();
            workers.add(worker);
            worker.thread.start();
        }
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (isShutdown) {
            throw new RejectedExecutionException("ThreadPool is shut down");
        }

        FutureTask<T> futureTask = new FutureTask<>(task);
        taskQueue.offer(futureTask);
        return futureTask;
    }

    public void shutdown() {
        isShutdown = true;
        for (Worker worker : workers) {
            worker.thread.interrupt();
        }
    }

    private class Worker implements Runnable {
        private final Thread thread;

        Worker() {
            this.thread = new Thread(this);
        }

        public void run() {
            while (!isShutdown || !taskQueue.isEmpty()) {
                try {
                    Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        System.out.println("Running task in thread: " + Thread.currentThread().getName());
                        task.run();
                    }
                } catch (InterruptedException e) {
                    // Allow graceful shutdown
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // --------------------------
    // Example Usage
    // --------------------------
    public static void main(String[] args) throws Exception {
        SimpleThreadPool pool = new SimpleThreadPool(8);

        // Submit a task with return value
        Future<Integer> result = pool.submit(() -> {
            TimeUnit.SECONDS.sleep(2);
            System.out.println("Computing square...");
            return 4 * 4;
        });

        System.out.println("Result is: " + result.get());

        // Submit many void tasks
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 98; i++) {
            futures.add(pool.submit(() -> {
                TimeUnit.SECONDS.sleep(2);
                System.out.println("Running time-consuming task");
                return null;
            }));
        }

        // Wait for all to finish
        for (Future<?> f : futures) {
            f.get();
        }

        pool.shutdown();
        System.out.println("All tasks done.");
    }
}
