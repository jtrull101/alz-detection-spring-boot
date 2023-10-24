import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModelIdPromptComponent } from './model-id-prompt.component';

describe('ModelIdPromptComponent', () => {
  let component: ModelIdPromptComponent;
  let fixture: ComponentFixture<ModelIdPromptComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ModelIdPromptComponent]
    });
    fixture = TestBed.createComponent(ModelIdPromptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
