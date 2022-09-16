import { TimingPoint } from "./timing-point.model";

export interface WorkloadResult {
    workloadId : string;
    workloadTypeName : string;
    canBeTerminated : boolean;
    isTerminated : boolean;
    startTime : number;
    endTime : number;
    status : string;
    results : TimingPoint[];
    description : string;
}