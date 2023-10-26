import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImagePredictRandomComponent } from './image-predict-random.component';

describe('ImagePredictRandomComponent', () => {
  let component: ImagePredictRandomComponent;
  let fixture: ComponentFixture<ImagePredictRandomComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ImagePredictRandomComponent]
    });
    fixture = TestBed.createComponent(ImagePredictRandomComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
