export interface NodeInfoMetrics {
    memory_used_bytes : number;
    total_sst_file_size_bytes : number;
    uncompressed_sst_file_size_bytes : number;
    read_ops_per_sec : number;
    write_ops_per_sec : number;
}