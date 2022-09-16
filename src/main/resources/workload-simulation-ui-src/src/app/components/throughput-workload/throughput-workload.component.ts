import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';

@Component({
  selector: 'app-throughput-workload',
  templateUrl: './throughput-workload.component.html',
  styleUrls: ['./throughput-workload.component.css']
})
export class ThroughputWorkloadComponent implements OnInit, OnChanges {

  @Input()
  data : any = {};

  @Input()
  duration : number = 3 * 60 * 1000;

  @Input()
  workloadId : string = 'id';

  constructor() { }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (changes.duration) {
        this.duration = changes.duration.currentValue;
      }
  }

}
