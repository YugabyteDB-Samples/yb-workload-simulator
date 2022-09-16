import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StepsDiagramComponent } from './steps-diagram.component';

describe('StepsDiagramComponent', () => {
  let component: StepsDiagramComponent;
  let fixture: ComponentFixture<StepsDiagramComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ StepsDiagramComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(StepsDiagramComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
