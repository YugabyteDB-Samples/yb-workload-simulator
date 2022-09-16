import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FixedStepWorkloadComponent } from './fixed-step-workload.component';

describe('FixedStepWorkloadComponent', () => {
  let component: FixedStepWorkloadComponent;
  let fixture: ComponentFixture<FixedStepWorkloadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FixedStepWorkloadComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FixedStepWorkloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
