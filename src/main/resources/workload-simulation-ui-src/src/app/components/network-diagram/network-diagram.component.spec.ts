import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDiagramComponent } from './network-diagram.component';

describe('NetworkDiagramComponent', () => {
  let component: NetworkDiagramComponent;
  let fixture: ComponentFixture<NetworkDiagramComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ NetworkDiagramComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(NetworkDiagramComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
