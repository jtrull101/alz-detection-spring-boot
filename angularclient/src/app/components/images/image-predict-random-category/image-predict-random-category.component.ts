import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';

@Component({
  selector: 'app-image-predict-random-category',
  templateUrl: './image-predict-random-category.component.html',
  styleUrls: ['./image-predict-random-category.component.css']
})
export class ImagePredictRandomCategoryComponent {
  title = 'Prediction on Random MRI of Specific Cateogry';
  public prediction: ImagePrediction;
  modelId:number;
  impairment:any;

  constructor(private service: ImagePredictionService, private route:ActivatedRoute) {
    this.modelId = -1;
    this.impairment = null;
    this.prediction = new ImagePrediction();
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
      this.impairment = s['impairment'];
    });
    this.predictRandom();
  }

  public predictRandom(): void {
    if (this.modelId && this.impairment) {
      this.service.predictFromRandomCategory(this.modelId, this.impairment).subscribe(
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
