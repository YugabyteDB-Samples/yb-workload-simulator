import { Component, OnInit, Input } from '@angular/core';
import { MenuItem } from 'primeng/api';

@Component({
  selector: 'app-steps-diagram',
  templateUrl: './steps-diagram.component.html',
  styleUrls: ['./steps-diagram.component.css']
})
export class StepsDiagramComponent implements OnInit {

  @Input()
  steps : MenuItem[] = [];

  @Input()
  index : number = 0; 
  constructor() { }

  ngOnInit(): void {
  }

}
