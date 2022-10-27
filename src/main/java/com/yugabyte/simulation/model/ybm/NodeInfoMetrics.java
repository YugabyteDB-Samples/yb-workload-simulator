package com.yugabyte.simulation.model.ybm;

public class NodeInfoMetrics {
    public long memory_used_bytes;
    public long total_sst_file_size_bytes;
    public long uncompressed_sst_file_size_bytes;
    public double read_ops_per_sec;
    public double write_ops_per_sec;
}
