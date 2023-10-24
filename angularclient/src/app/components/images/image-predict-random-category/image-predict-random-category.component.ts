import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-image-predict-random-category',
  templateUrl: './image-predict-random-category.component.html',
  styleUrls: ['./image-predict-random-category.component.css']
})
export class ImagePredictRandomCategoryComponent {
  title = 'Prediction on Random MRI of Specific Cateogry';
  public prediction: ImagePrediction|undefined;
  no:number|undefined;
  veryMild:number|undefined;
  mild:number|undefined;
  moderate:number|undefined;
  img:any;
  modelId: number | undefined;
  impairment:any;

  constructor(private service: ImagePredictionService, private modelService:ModelService, private route:ActivatedRoute) {
    this.modelId = modelService.getModelId();
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.impairment = s['impairment'];
    });
  }

  public predictRandom(): void {
    if (this.modelId && this.impairment) {
      this.service.predictFromRandomCategory(this.modelId, this.impairment).subscribe(
        (response: ImagePrediction) => {
          this.prediction = response;
          this.no = this.prediction.conf_NoImpairment;
          this.veryMild = this.prediction.conf_VeryMildImpairment;
          this.mild = this.prediction.conf_MildImpairment;
          this.moderate = this.prediction.conf_ModerateImpairment;

          const paths = this.prediction.filepath.split("/");
          const category = paths[paths.length-2];
          const filename = paths[paths.length-1];
          this.img = "assets/test/" + category + "/" + filename;
        },
        (error: HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
