import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelDeleteAllComponent } from './model-delete-all.component';

describe('ModelDeleteAllComponent', () => {
  let component: ModelDeleteAllComponent;
  let fixture: ComponentFixture<ModelDeleteAllComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ModelDeleteAllComponent]
    });
    fixture = TestBed.createComponent(ModelDeleteAllComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
