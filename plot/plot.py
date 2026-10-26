import csv
import sys
from collections import Counter

import matplotlib.pyplot as plt

csv_path = sys.argv[1] if len(sys.argv) > 1 else "../client2/latencies.csv"

with open(csv_path) as f:
    starts = [int(row["start_ms"]) for row in csv.DictReader(f)]

first = min(starts)
per_second = Counter((s - first) // 1000 for s in starts)

seconds = range(max(per_second) + 1)
counts = [per_second[s] for s in seconds]

plt.figure(figsize=(10, 4))
plt.plot(seconds, counts)
plt.title("Client 2: throughput over time (32 threads)")
plt.xlabel("Time since start (s)")
plt.ylabel("Requests per second")
plt.grid(alpha=0.3)
plt.tight_layout()
plt.savefig("throughput.png", dpi=150)
print("saved throughput.png")
