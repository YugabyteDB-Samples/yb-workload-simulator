import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';
import { AppSimulationComponent } from './components/app-simulation/app-simulation.component';
import { ThroughputComponent } from './components/throughput/throughput.component';
import { StatisticsGraphComponent } from './components/statistics-graph/statistics-graph.component';
import { HttpClientModule } from '@angular/common/http';
import { NetworkDiagramComponent } from './components/network-diagram/network-diagram.component';

import { AccordionModule } from 'primeng/accordion';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputSwitchModule } from 'primeng/inputswitch';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { DropdownModule } from 'primeng/dropdown';
import { MenuModule } from 'primeng/menu'
import { PanelModule } from 'primeng/panel';
import { PasswordModule } from 'primeng/password';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SliderModule} from 'primeng/slider';
import { StepsModule } from 'primeng/steps'
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { ToastModule } from 'primeng/toast';
import {  KnobModule } from 'primeng/knob';

import { WorkloadService } from './services/workload-service.service';
import { StepsDiagramComponent } from './components/steps-diagram/steps-diagram.component';
import { Routes, RouterModule } from '@angular/router';
import { FixedStepWorkloadComponent } from './components/fixed-step-workload/fixed-step-workload.component';
import { ThroughputWorkloadComponent } from './components/throughput-workload/throughput-workload.component';
import { FixedTargetWorkloadComponent } from './components/fixed-target-workload/fixed-target-workload.component';
import { ConfigurationComponent } from './components/configuration/configuration.component';


@NgModule({
  declarations: [
    AppComponent,
    AppSimulationComponent,
    ThroughputComponent,
    StatisticsGraphComponent,
    NetworkDiagramComponent,
    StepsDiagramComponent,
    FixedStepWorkloadComponent,
    ThroughputWorkloadComponent,
    FixedTargetWorkloadComponent,
    ConfigurationComponent,
  ],
  imports: [
    AccordionModule,
    BrowserModule,
    BrowserAnimationsModule,
    ButtonModule,
    ConfirmDialogModule,
    DialogModule,
    DropdownModule,
    HttpClientModule,
    InputNumberModule,
    InputSwitchModule,
    InputTextareaModule,
    InputTextModule,
    KnobModule,
    MenuModule,
    PanelModule,
    PasswordModule,
    SliderModule,
    StepsModule,
    FormsModule,
    SelectButtonModule,
    TabViewModule,
    TableModule,
    ToastModule,
    RouterModule.forRoot([
      {path: 'simulation', component: AppSimulationComponent},
      {path: 'configuration', component: ConfigurationComponent},
      {path:'', component: AppSimulationComponent}
    ])
  ],
  providers: [WorkloadService],
  bootstrap: [AppComponent],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class AppModule { }
