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

  constructor() { }

  ngOnInit(): void {
  }

}
