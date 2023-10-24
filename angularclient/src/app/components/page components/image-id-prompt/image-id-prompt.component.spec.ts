import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImageIdPromptComponent } from './image-id-prompt.component';

describe('ImageIdPromptComponent', () => {
  let component: ImageIdPromptComponent;
  let fixture: ComponentFixture<ImageIdPromptComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ImageIdPromptComponent]
    });
    fixture = TestBed.createComponent(ImageIdPromptComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
