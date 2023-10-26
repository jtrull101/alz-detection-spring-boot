import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImagePredictComponent } from './image-predict.component';

describe('ImagePredictComponent', () => {
  let component: ImagePredictComponent;
  let fixture: ComponentFixture<ImagePredictComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ImagePredictComponent]
    });
    fixture = TestBed.createComponent(ImagePredictComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
