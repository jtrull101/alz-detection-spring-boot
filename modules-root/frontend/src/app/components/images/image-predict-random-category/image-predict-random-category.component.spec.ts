import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImagePredictRandomCategoryComponent } from './image-predict-random-category.component';

describe('ImagePredictRandomCategoryComponent', () => {
  let component: ImagePredictRandomCategoryComponent;
  let fixture: ComponentFixture<ImagePredictRandomCategoryComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ImagePredictRandomCategoryComponent]
    });
    fixture = TestBed.createComponent(ImagePredictRandomCategoryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
