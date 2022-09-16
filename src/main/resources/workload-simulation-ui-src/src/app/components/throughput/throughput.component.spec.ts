import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ThroughputComponent } from './throughput.component';

describe('ThroughputComponent', () => {
  let component: ThroughputComponent;
  let fixture: ComponentFixture<ThroughputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ThroughputComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ThroughputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
