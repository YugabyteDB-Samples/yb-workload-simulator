import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FixedTargetWorkloadComponent } from './fixed-target-workload.component';

describe('FixedTargetWorkloadComponent', () => {
  let component: FixedTargetWorkloadComponent;
  let fixture: ComponentFixture<FixedTargetWorkloadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ FixedTargetWorkloadComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FixedTargetWorkloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
