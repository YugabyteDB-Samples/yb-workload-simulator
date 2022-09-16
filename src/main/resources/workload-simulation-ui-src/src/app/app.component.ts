import { AfterViewInit, Component, ElementRef, EventEmitter, HostListener, OnInit, ViewChild } from '@angular/core';
import { TimingData } from './model/timing-data.model';
import { TimingPoint } from './model/timing-point.model';
import { WorkloadDesc } from './model/workload-desc.model';
import { YugabyteDataSourceService } from './services/yugabyte-data-source.service';
import { ParamValue } from './model/param-value.model';
import { WorkloadService } from './services/workload-service.service';
import { WorkloadParamDesc } from './model/workload-param-desc.model';
import { InvocationResult } from './model/invocation-result.model';
import { WorkloadStatus } from './model/workload-status.model';
import { MenuItem } from 'primeng/api';
import { WorkloadResult } from './model/workload-result.model';
import { SystemPreferences } from './model/system-preferences.model';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})

export class AppComponent implements AfterViewInit, OnInit {
  @ViewChild("accordion", {static:false}) 
  accordion!: ElementRef;

  items!: MenuItem[];

  status = '';
  // updateThreads : number = 20;
  // updateRequests : number = 10000;
  // workload2Threads : number = 10;
  // workload2Requests : number = 40000;
  // workload1Threads: number = 5;
  // workload1Requests : number = 50000;

  AGGREGATION_WORKLOAD = 'Aggregation Counter';
  showDialog = false;
  title = 'workload-simulation-ui-src';
  currentData : any = {'Aggregation Counter' : { results:[] }};
  activeWorkloads : string[] = [];
  startTime = 0;
  MAX_READINGS = 3600;
  // WORKLOAD1 = "WORKLOAD1";
  // WORKLOAD2 = "WORKLOAD2";
  LATENCY = "LATENCY";
  THROUGHPUT = "THROUGHPUT";

  systemPreferences : SystemPreferences = {doLogging: false, loggingDir : '/tmp', workloadName: '', graphRefreshMs: 350, networkRefreshMs: 1000};
  editingSystemPreferences : SystemPreferences = {...this.systemPreferences};

  // workload1Latency = "Workload 1";
  // workload2Latency = "Workload 2";
  // workload1Throughput = "Workload 1";
  // workload2Throughput = "Workload 2";

  workloadValues : any = null;
  valuesComputed = false;
  selected : boolean[] = [];

  options =['first', 'second', 'third'];
  test : any;

  activeLoading : boolean = false;
  workloadResults : WorkloadResult[] = [];

  commsErrorDialog : boolean = false;
  commsErrorCount : number = 0;
  timer : any;

  private minDuration = 60*1000;
  private maxDuration = this.MAX_READINGS * 1000;
  duration = 3 * 60 * 1000;

  constructor(private dataSource : YugabyteDataSourceService,
            private workloadService : WorkloadService ) {
    this.timer = setInterval(() => {
      this.getResults();
    },350);

    workloadService.getWorkloadObservable().subscribe( data => this.computeWorkloadValues(data));
    this.getSystemPreferences();
  }

  ngOnInit() {
    this.items = [
      {
          label: 'Options',
          items: [{
              label: 'Stop Workload',
              icon: 'pi pi-refresh',
              command: (evt) => {
                  this.stopWorkload(evt);
              }
          },
          // {
          //     label: 'Export Statistics',
          //     icon: 'pi pi-times',
          //     command: () => {
          //         this.delete();
          //     }
          // }
      ]},
      // {
      //     label: 'Navigate',
      //     items: [{
      //         label: 'Angular',
      //         icon: 'pi pi-external-link',
      //         url: 'http://angular.io'
      //     },
      //     {
      //         label: 'Router',
      //         icon: 'pi pi-upload',
      //         routerLink: '/fileupload'
      //     }
      // ]}
    ];
  }

  ngAfterViewInit() {
  }

  private extractWorkloadIdFromEvent(evt : any)  : string {
    let control = evt.originalEvent.srcElement.closest('.workload-inst');
    let classes = control.classList;
    for (const thisClass of classes) {
      if (thisClass.match(/^[A-Z_]+_\d+$/)) {
        return thisClass;
      }
    }
    return '';
  }

  stopWorkload(evt :any) {
    let result = this.extractWorkloadIdFromEvent(evt);
    if (result) {
      this.terminateTask(result);
    }
  }
  delete() {
    console.log("delete");
  }
  
  private setSystemPreferences(preferences : SystemPreferences) {
    this.systemPreferences = preferences;
    clearInterval(this.timer);
    this.timer = setInterval(() => {
      this.getResults();
    }, preferences.graphRefreshMs);
  }

  getSystemPreferences() {
    this.dataSource.getSystemPreferences().subscribe(result => {
      this.setSystemPreferences(result);
    })
  }
  saveSystemSettings() {
    this.dataSource.saveSystemPreferences(this.editingSystemPreferences).subscribe(result => {
      this.status = "System Preferences Saved";
      this.setSystemPreferences(this.editingSystemPreferences);
    });
  }

  computeWorkloadValues(workloads : WorkloadDesc[]) {
    // let workloads = this.workloadService.getWorkloads();
    this.workloadValues = {};
    for (let i = 0; i < workloads.length; i++) {
      let thisWorkload = workloads[i];
      let currentValues : any = {};
      for (let j = 0; j < thisWorkload.params.length; j++) {
        let thisParam = thisWorkload.params[j];
        switch (thisParam.type) {
          case 'NUMBER':
            if (thisParam.defaultValue) {
              currentValues[thisParam.name] = thisParam.defaultValue.intValue || 0;
            }
            else {
              currentValues[thisParam.name] = 0;
            }
            break;

          case 'BOOLEAN':
            if (thisParam.defaultValue) {
              currentValues[thisParam.name] = thisParam.defaultValue.boolValue || false;
            }
            else {
              currentValues[thisParam.name] = false;
            }
            break;

          case 'STRING':
            if (thisParam.defaultValue) {
              currentValues[thisParam.name] = thisParam.defaultValue.stringValue || false;
            }
            else {
              currentValues[thisParam.name] = '';
            }
            break;
  
        }
      }
      this.workloadValues[thisWorkload.workloadId] = currentValues;
    }
    this.valuesComputed = true;
  }

  private refreshActiveTasks() {
    //this.activeLoading = true;
    this.dataSource.getActiveWorkloads().subscribe(workloads => {
      this.activeLoading = false;
      this.workloadResults = workloads;
    });
  }

  timerId : any = null;
  // Called when the tab changes.
  handleChange(e : any) {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
    if (e.index == 1) {
      this.refreshActiveTasks();
      this.timerId = setInterval(() => this.refreshActiveTasks(), 2000);
    }
  }

  getWorkloads() {
    return this.workloadService.getWorkloads();
  }

  terminateTask(workloadId : string) {
    this.dataSource.terminateWorkload(workloadId).subscribe( result => {
      this.refreshActiveTasks();
    });
  }


  private valueToParam(paramDesc: WorkloadParamDesc, paramValue : any) : ParamValue {
    let paramToSend : ParamValue = {type: paramDesc.type};
    switch (paramDesc.type) {
      case 'NUMBER': paramToSend.intValue = paramValue; return paramToSend;
      case 'BOOLEAN': paramToSend.boolValue = paramValue; return paramToSend;
      case 'STRING': paramToSend.stringValue = paramValue; return paramToSend;
    }
    console.log('Unknown parameter type for ' + paramDesc.name);
    return paramToSend;
  }

  launchWorkload(name : String) {
    console.log("launching " + name);
    let paramsToSend : ParamValue[] = [];
    let values = this.workloadValues[name as any];
    let workloads = this.workloadService.getWorkloads();
    for (let i = 0; i < workloads.length; i++) {
      if (workloads[i].workloadId === name) {
        let thisWorkload = workloads[i];

        // Set the names of the workloads
        // this.workload1Latency = thisWorkload.workloadNames.WORKLOAD1 || 'Workload 1';
        // this.workload2Latency = thisWorkload.workloadNames.WORKLOAD2 || 'Workload 2';
        // this.workload1Throughput = thisWorkload.workloadNames.WORKLOAD1 || 'Workload 1';
        // this.workload2Throughput = thisWorkload.workloadNames.WORKLOAD2 || 'Workload 2';

        for (let paramIndex = 0; paramIndex < thisWorkload.params.length; paramIndex++) {
          let thisParam = thisWorkload.params[paramIndex];
          let paramName = thisParam.name;
          let thisParamValue = this.valueToParam(thisParam, values[paramName]);
          paramsToSend.push(thisParamValue);
        }
      }
    }
    
    this.status = "Submitting workload " + name + "..."
    this.dataSource.invokeWorkload(name, paramsToSend).subscribe(success => {
      console.log(success);
      if (success.result ==0) {
        this.status = "Workload " + name + " successfully submitted."
      }
      else {
        this.status = "Workload " + name + " failed to submit. Reported error was " + success.data;
      }
    },
    (error => {
      console.log(error);
      this.status = "Workload " + name + " failed to submit";
    }));
  }

  getResults() {
    this.dataSource.getTimingResults(this.startTime).subscribe(data => {
      this.commsErrorCount = 0;
      this.commsErrorDialog = false;

      // Iterate through the workloads based on the information here. 
      if (!data[this.AGGREGATION_WORKLOAD]) {
        return;
      }
      this.activeWorkloads = [];

      let newData : any = {};

      for (const metricName in data) {
        if (data[metricName].canBeTerminated) {
          this.activeWorkloads.push(metricName);
        }
        // let metricName = this.AGGREGATION_WORKLOAD;

        newData[metricName] = data[metricName];

        if (this.startTime == 0 || !this.currentData[metricName]) {
          this.currentData[metricName] = newData[metricName];
        }
        else {
          let currentResults = this.currentData[metricName].results;
          this.currentData[metricName] = {...newData[metricName]};
          this.currentData[metricName].results = currentResults;
          // Append these results to the existing data and trim the front if needed
          this.currentData[metricName].results = this.currentData[metricName].results.concat(newData[metricName].results);
        }
        if (this.currentData[metricName].results.length > this.MAX_READINGS) {
          this.currentData[metricName].results.splice(0, this.currentData[metricName].results.length-this.MAX_READINGS);
        }
        this.currentData[metricName].results = [].concat(this.currentData[metricName].results);
      }
      let aggregateResults = this.currentData[this.AGGREGATION_WORKLOAD].results;
      if (aggregateResults.length > 0) {
        this.startTime = aggregateResults[aggregateResults.length-1].startTimeMs;
      }
      // let temp = this.currentData;
      // this.currentData = {};
      // this.currentData[metricName] = temp[metricName];
    },
    (error) => {
      if (!this.commsErrorDialog) {
        if (++this.commsErrorCount > 5) {
          this.commsErrorDialog = true;
        }
      }
    });
  }

  @HostListener('wheel', ['$event'])
  onMouseWheel(event : any) {
    // if ((event.srcElement.closest('p-dialog') == null)  && (event.srcElement.closest('figure') != null) || event.shiftKey) {
   if (event.shiftKey) {
      event.preventDefault();
      let amount = event.wheelDelta;
      let change = 1+(amount/1200);
      this.duration = Math.floor(Math.max(this.minDuration, Math.min(this.maxDuration, this.duration * change)));
    }
  } 

  displayDialog() {
    // setTimeout(() => {
    //   const accordionElement = this.accordion.nativeElement;
    //   if (accordionElement) {
    //     let workloadElementList = accordionElement.querySelectorAll('[role="region"]');
    //     for (let i = 0; i < workloadElementList.length; i++) {
    //       let workloadElement = workloadElementList[i];
    //       (workloadElement as any).setAttribute('style', 'height:0; overflow:hidden;');
    //     }
    //   }
    // },100);

    this.editingSystemPreferences = {...this.systemPreferences};
    this.status = "";
    let count = this.getWorkloads().length;
    for (let i =0; i < count; i++) {
      this.selected[i] = false;
    }
    this.showDialog = true;
    // this.computeWorkloadValues(this.workloadService.getWorkloads());
  }

  closeDialog() {
    this.showDialog = false;
  }
}
