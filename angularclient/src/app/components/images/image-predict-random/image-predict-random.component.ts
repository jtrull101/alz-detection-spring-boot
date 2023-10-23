import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';

@Component({
  selector: 'app-image-predict-random',
  templateUrl: './image-predict-random.component.html',
  styleUrls: ['./image-predict-random.component.css'],
})
export class ImagePredictRandomComponent {
  title = 'Prediction on Random MRI';
  public prediction: ImagePrediction;
  modelId: number;

  constructor(private service: ImagePredictionService, private route: ActivatedRoute) {
    this.modelId = -1;
    this.prediction = new ImagePrediction();
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
    });
    this.predictRandom();
  }

  public predictRandom(): void {
    if (this.modelId) {
      this.service.predictFromRandom(this.modelId).subscribe(
        (response: ImagePrediction) => {
          this.prediction = response;
        },
        (error: HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
