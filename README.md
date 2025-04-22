⚙️ Technical Overview of the Ski Data Processing System

This system is designed to handle high-throughput, low-latency processing of skier lift ride data using a modular and scalable architecture. It supports both large-scale data ingestion (POST) and efficient query retrieval (GET), optimized for concurrent workloads.

⸻

🧱 Architecture Components
	•	Client1 / Client2: Generate and send up to 200,000 lift ride events using multithreaded API clients. Client2 additionally logs latency, status, and throughput metrics.
	•	Server (Servlet-based): Handles RESTful POST/GET requests. Writes go through RabbitMQ; reads are served via Redis cache.
	•	RabbitMQ (Message Queue): Asynchronous decoupling between ingestion and processing layers.
	•	Consumer: Pulls messages from RabbitMQ, parses data, and stores results in Redis using a thread pool and asynchronous processing.

⸻

🛠️ Core Technologies & Techniques
	•	Concurrency:
	•	ExecutorService and CompletableFuture ensure scalable multithreading.
	•	ConcurrentHashMap for safe, shared-memory operations.

	•	Redis:
	•	Used as an in-memory cache to reduce latency and offload read-heavy traffic.
	•	Structures: Hashes (per-skier stats), Sets (unique skiers), optimized with precomputed fields and TTLs.

	•	RabbitMQ:
	•	Enables high-throughput message queuing between API and consumer layers.
	•	Custom channel pooling to reduce connection overhead.

	•	Performance Testing:
	•	JMeter used for simulating 128 threads × 500 iterations (64,000+ GET requests).
	•	System supports ~2,965 requests/sec with <30ms average response time.

⸻

📈 Optimization Strategies
	•	Redis Read Replicas: Offload heavy read traffic.
	•	Redis Cluster: Enables automatic key sharding across multiple nodes.
	•	Hot Key Sharding: Hash-based key splitting for load balancing.
	•	Hot/Cold Data Separation: Redis used for active (“hot”) data; MySQL planned for persistent storage of inactive (“cold”) data.
