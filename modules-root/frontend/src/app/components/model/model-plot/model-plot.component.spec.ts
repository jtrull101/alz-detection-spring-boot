import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelPlotComponent } from './model-plot.component';

describe('ModelPlotComponent', () => {
  let component: ModelPlotComponent;
  let fixture: ComponentFixture<ModelPlotComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ModelPlotComponent]
    });
    fixture = TestBed.createComponent(ModelPlotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
