package com.yugabyte.simulation.model.ybm;

public class NodeInfo {
    public String name;
    public boolean is_node_up;
    public boolean is_master;
    public boolean is_tserver;
    public boolean is_read_replica;
    public NodeInfoMetrics metrics;
    public NodeInfoCloudInfo cloud_info;
}
