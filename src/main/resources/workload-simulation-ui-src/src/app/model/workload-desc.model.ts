import { WorkloadParamDesc } from "./workload-param-desc.model";

export interface WorkloadDesc {
    workloadId : string;
    name : string;
    description : string;
    params : WorkloadParamDesc[];
    workloadNames : any;
}