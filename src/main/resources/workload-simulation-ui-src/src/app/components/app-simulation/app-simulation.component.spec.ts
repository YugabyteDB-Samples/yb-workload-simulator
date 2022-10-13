import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppSimulationComponent } from './app-simulation.component';

describe('AppSimulationComponent', () => {
  let component: AppSimulationComponent;
  let fixture: ComponentFixture<AppSimulationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AppSimulationComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AppSimulationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
