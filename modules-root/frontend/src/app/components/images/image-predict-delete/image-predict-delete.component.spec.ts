import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImagePredictDeleteComponent } from './image-predict-delete.component';

describe('ImagePredictDeleteComponent', () => {
  let component: ImagePredictDeleteComponent;
  let fixture: ComponentFixture<ImagePredictDeleteComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ImagePredictDeleteComponent]
    });
    fixture = TestBed.createComponent(ImagePredictDeleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
