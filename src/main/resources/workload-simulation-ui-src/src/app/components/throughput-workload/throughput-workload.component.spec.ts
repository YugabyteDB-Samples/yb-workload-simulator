import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThroughputWorkloadComponent } from './throughput-workload.component';

describe('ThroughputWorkloadComponent', () => {
  let component: ThroughputWorkloadComponent;
  let fixture: ComponentFixture<ThroughputWorkloadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ThroughputWorkloadComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ThroughputWorkloadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
