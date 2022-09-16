import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-fixed-target-workload',
  templateUrl: './fixed-target-workload.component.html',
  styleUrls: ['./fixed-target-workload.component.css']
})
export class FixedTargetWorkloadComponent implements OnInit {

  @Input()
  data : any = {};

  @Input()
  duration : number = 3 * 60 * 1000;

  @Input()
  workloadId : string = 'id';

  constructor() { }

  getLatestData() : any {
    if (this.data && this.data.results && this.data.results.length) {
      return this.data.results[this.data.results.length-1];
    }
    else{
      return {};
    }
  }
  ngOnInit(): void {
  }

}
