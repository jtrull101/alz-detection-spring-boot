import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-image-predict',
  templateUrl: './image-predict.component.html',
  styleUrls: ['./image-predict.component.css'],
})
export class ImagePredictComponent {
  title = 'Prediction on MRI';
  public prediction: ImagePrediction| undefined;
  no:number|undefined;
  veryMild:number|undefined;
  mild:number|undefined;
  moderate:number|undefined;
  modelId: number | undefined;
  file: any;
  img:any;

  constructor(
    private service: ImagePredictionService,
    private modelService: ModelService,
    private route: ActivatedRoute
  ) {
    this.modelId = this.modelService.getModelId();
  }

  onFilechange(event: any) {
    this.file = event.target.files[0];
  }

  public runPredicitonOnFile(): void {
    if (this.modelId && this.file) {
      const formData = new FormData();
      formData.append('image', this.file);
      this.service.predictFromFile(formData, this.modelId).subscribe(
        (response: ImagePrediction) => {
          this.prediction = response;
          this.no = this.prediction.conf_NoImpairment;
          this.veryMild = this.prediction.conf_VeryMildImpairment;
          this.mild = this.prediction.conf_MildImpairment;
          this.moderate = this.prediction.conf_ModerateImpairment;

          let reader = new FileReader();
          reader.readAsDataURL(this.file);
          reader.onload = () => {
            this.img = reader.result;
          };
        },
        (error: HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
