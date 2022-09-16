import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-fixed-step-workload',
  templateUrl: './fixed-step-workload.component.html',
  styleUrls: ['./fixed-step-workload.component.css']
})
export class FixedStepWorkloadComponent implements OnInit, OnChanges {

  @Input()
  data : any = {};

  steps : MenuItem[] = [];
  stepIndex : number = 0;

  constructor() { }

  ngOnInit(): void {
    for (const thisStep of this.data.steps) {
      this.steps.push({label: thisStep.name});
    }
  }

  ngOnChanges(changes : SimpleChanges) {
    this.stepIndex = this.data.currentStepNumber;
  }

}
