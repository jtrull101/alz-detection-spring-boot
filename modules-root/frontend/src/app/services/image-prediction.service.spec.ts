import { TestBed } from '@angular/core/testing';

import { ImagePredictionService } from './image-prediction.service';

describe('ImagePredictionService', () => {
  let service: ImagePredictionService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ImagePredictionService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
