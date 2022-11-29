import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-configuration',
  templateUrl: './configuration.component.html',
  styleUrls: ['./configuration.component.css']
})
export class ConfigurationComponent implements OnInit {

  @Input()
  ybOptions : string[] = [];

  @Input()
  ybOption : string = '';

  desiredValue : number = 0;
  currentValue : number = 0;

  constructor() { }

  ngOnInit(): void {
  }

  formatTime(value : number) : string {
    let hours = Math.floor(value / 3600);
    let mins = Math.floor((value - hours * 3600) / 60);
    let secs = value % 60;
    return hours + "h" + mins + "m";
  }
}
