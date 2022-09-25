import { NodeInfoCloudInfo } from "./node-info-cloud-info.model";
import { NodeInfoMetrics } from "./node-info-metrics.model";

export interface NodeInfo {
    name : string;
    is_node_up : boolean;
    is_master : boolean;
    is_tserver : boolean;
    is_read_replica : boolean;
    metrics : NodeInfoMetrics;
    cloud_info : NodeInfoCloudInfo;
}