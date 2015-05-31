Priority Work-Stealing Scheduler
================================

A decentralized work-stealing scheduler that dynamically schedules fixed-priority tasks in a non-preemptive manner.

## Brief Description

We develop a decentralized work-stealing scheduler that dynamically schedules fixed-priority tasks in a non-preemptive manner.
We adapt a multi-level queue scheduling algorithm where the tasks can be classified into priority classes and assign a separate container for each priority class.
Our algorithm uses non-blocking operations and minimizes the number of compare-and-swap operations that each local worker thread performs.
Furthermore, our workload independent approach extracts performance even in the presence of fine-grained tasks.
Our approach relies on the unusual approach of performing steals even if the worker thread is not idle to adhere close to the priority order while scheduling.
This strategy ensures that worker threads, in our scheduler, are executing tasks from the highest priority class.
Thus, we minimize instances of priority inversion where low priority tasks are scheduled for execution even if higher priority tasks are available in the distributed work queue.
Our approach uses non-blocking operations, is workload independent, and we achieve performance even in the presence of fine-grained tasks.
Our experimental results show that the Java implementation of our scheduler performs favorably compared to other schedulers (priority and non-priority) available in the Java standard library.

Please refer to the following paper for further details: <br />
Load Balancing Prioritized Tasks via Work-Stealing.
<a href="mailto:shams@rice.edu">Shams Imam</a>,
<a href="mailto:vsarkar@rice.edu">Vivek Sarkar</a>.
21st International European Conference on Parallel and Distributed Computing (<a href="http://www.europar2015.org/">Euro-Par'15<a/>),
August 2015.