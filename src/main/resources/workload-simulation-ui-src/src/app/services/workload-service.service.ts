import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { WorkloadDesc } from "../model/workload-desc.model";
import { YugabyteDataSourceService } from "./yugabyte-data-source.service";

@Injectable()
export class WorkloadService {
    private workloads : WorkloadDesc[] = [];
    private workloadObserver;
    constructor(private dataSource : YugabyteDataSourceService) {
        this.workloadObserver = this.dataSource.getWorkloads();

        this.workloadObserver.subscribe(workloads => {
            this.workloads = workloads;
        });
    }

    getWorkloads() : WorkloadDesc[] {
        return this.workloads;
    }

    getWorkloadObservable() : Observable<WorkloadDesc[]> {
        return this.workloadObserver;
    }
}